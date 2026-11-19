package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.DeletedRecordReference;
import com.team.corporate.backend.exceptions.DeletedRecordConflictException;
import com.team.corporate.backend.exceptions.DeletedRecordNotFoundException;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.services.impls.OrderStock;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.team.corporate.backend.services.SoftDeletedEntity.*;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class SoftDeleteAdministrationService {
    private static final List<Reference> REFERENCES = List.of(
            new Reference(PRODUCTS, "category_id", CATEGORIES),
            new Reference(INVENTORIES, "product_id", PRODUCTS),
            new Reference(INVENTORIES, "warehouse_id", WAREHOUSES),
            new Reference(ORDERS, "user_id", USERS),
            new Reference(ORDER_ITEMS, "order_id", ORDERS),
            new Reference(ORDER_ITEMS, "product_id", PRODUCTS),
            new Reference(AUDIT_LOGS, "order_id", ORDERS),
            new Reference(AUDIT_LOGS, "changed_by", USERS)
    );

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final OrderStock stock;
    private final Clock clock;

    public SoftDeleteAdministrationService(JdbcTemplate jdbc, EntityManager entityManager,
                                         OrderStock stock, Clock clock) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.stock = stock;
        this.clock = clock;
    }

    @Transactional
    public void restore(String entityPath, UUID id) {
        var entity = SoftDeletedEntity.fromPath(entityPath);
        entityManager.flush();
        var row = lockRecord(entity, id);
        if (row == null) {
            throw new DeletedRecordNotFoundException();
        }
        if (row.get("deleted_at") == null) {
            return;
        }

        requireActiveParents(entity, row);

        jdbc.update("UPDATE " + entity.table()
                + " SET deleted_at = NULL, version = version + 1 WHERE id = ?", id);
        if (entity == USERS) {
            jdbc.update("UPDATE refresh_sessions SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
                    Timestamp.from(clock.instant()), id);
        }
        entityManager.clear();
        validateReservations(entity, id, row);
    }

    @Transactional
    public void purge(String entityPath, UUID id) {
        var entity = SoftDeletedEntity.fromPath(entityPath);
        entityManager.flush();
        var deletionOrder = new ArrayList<RecordKey>();
        collectDeletedRecords(new RecordKey(entity, id), new HashSet<>(), deletionOrder);

        for (var record : deletionOrder) {
            if (record.entity() == USERS) {
                deleteUserCredentialsAndSessions(record.id());
            }
            if (record.entity() == PRODUCTS) {
                jdbc.update("DELETE FROM product_tags WHERE product_id = ?", record.id());
            }
            if (record.entity() == TAGS) {
                jdbc.update("DELETE FROM product_tags WHERE tag_id = ?", record.id());
            }
            jdbc.update("DELETE FROM " + record.entity().table() + " WHERE id = ? AND deleted_at IS NOT NULL",
                    record.id());
        }
        entityManager.clear();
    }

    private Map<String, Object> lockRecord(SoftDeletedEntity entity, UUID id) {
        var rows = jdbc.queryForList("SELECT * FROM " + entity.table() + " WHERE id = ? FOR UPDATE", id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private void requireActiveParents(SoftDeletedEntity entity, Map<String, Object> row) {
        for (var reference : REFERENCES) {
            if (reference.child() == entity) {
                var parentId = (UUID) row.get(reference.column());
                if (parentId != null) {
                    var parent = lockRecord(reference.parent(), parentId);
                    if (parent == null || parent.get("deleted_at") != null) {
                        throw new DeletedRecordConflictException("Сначала восстановите связанную запись.",
                                List.of(new DeletedRecordReference(reference.parent().path(), parentId)));
                    }
                    requireActiveParents(reference.parent(), parent);
                }
            }
        }
    }

    private void collectDeletedRecords(RecordKey record, Set<RecordKey> visited, List<RecordKey> deletionOrder) {
        if (!visited.add(record)) {
            return;
        }
        var row = lockRecord(record.entity(), record.id());
        if (row == null) {
            return;
        }
        if (row.get("deleted_at") == null) {
            throw new DeletedRecordConflictException("Окончательное удаление затрагивает активную запись.",
                    List.of(new DeletedRecordReference(record.entity().path(), record.id())));
        }

        for (var reference : REFERENCES) {
            if (reference.parent() == record.entity()) {
                var childIds = jdbc.queryForList("SELECT id FROM " + reference.child().table()
                        + " WHERE " + reference.column() + " = ? ORDER BY id FOR UPDATE", UUID.class, record.id());
                for (var childId : childIds) {
                    collectDeletedRecords(new RecordKey(reference.child(), childId), visited, deletionOrder);
                }
            }
        }
        deletionOrder.add(record);
    }

    private void deleteUserCredentialsAndSessions(UUID userId) {
        jdbc.update("DELETE FROM refresh_tokens WHERE session_id IN (SELECT id FROM refresh_sessions WHERE user_id = ?)", userId);
        jdbc.update("DELETE FROM refresh_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_credentials WHERE user_id = ?", userId);
    }

    private void validateReservations(SoftDeletedEntity entity, UUID id, Map<String, Object> row) {
        List<UUID> orderIds = switch (entity) {
            case USERS -> jdbc.queryForList("SELECT id FROM orders WHERE user_id = ? AND deleted_at IS NULL ORDER BY id",
                    UUID.class, id);
            case PRODUCTS -> jdbc.queryForList("""
                    SELECT DISTINCT o.id FROM orders o
                    JOIN users u ON u.id = o.user_id AND u.deleted_at IS NULL
                    JOIN order_items i ON i.order_id = o.id AND i.deleted_at IS NULL
                    WHERE i.product_id = ? AND o.deleted_at IS NULL ORDER BY o.id
                    """, UUID.class, id);
            case ORDERS -> List.of(id);
            case ORDER_ITEMS -> List.of((UUID) row.get("order_id"));
            default -> List.of();
        };

        for (var orderId : orderIds) {
            var orders = jdbc.queryForList("SELECT order_status FROM orders WHERE id = ? AND deleted_at IS NULL FOR UPDATE", orderId);
            if (!orders.isEmpty() && OrderStock.reservesStock(
                    OrderStatus.valueOf(orders.getFirst().get("order_status").toString()))) {
                try {
                    stock.requireAvailable(stock.quantities(orderId), orderId);
                } catch (IllegalArgumentException exception) {
                    throw new DeletedRecordConflictException(exception.getMessage(),
                            List.of(new DeletedRecordReference(ORDERS.path(), orderId)));
                }
            }
        }
    }

    private record Reference(SoftDeletedEntity child, String column, SoftDeletedEntity parent) {}

    private record RecordKey(SoftDeletedEntity entity, UUID id) {}
}
