package com.team.corporate.backend.audit;

import com.team.corporate.backend.entities.AuditEntityType;
import com.team.corporate.backend.entities.AuditEventType;
import com.team.corporate.core.entities.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditEntityListener {
    private final AuditEventWriter writer;

    public AuditEntityListener(AuditEventWriter writer) {
        this.writer = writer;
    }

    public void created(Object entity) {
        if (entity instanceof AuditLog log) {
            writer.record(AuditEventType.ORDER_STATUS_CHANGED, AuditEntityType.ORDER, log.getOrder().getId(),
                    "Статус заказа: " + log.getOldStatus() + " → " + log.getNewStatus() + ".");
        } else {
            record(AuditEventType.CREATE, entity, "Создана запись: ");
        }
    }

    public void updated(Object entity) { record(AuditEventType.UPDATE, entity, "Изменена запись: "); }

    public void deleted(Object entity) { record(AuditEventType.DELETE, entity, "Удалена запись: "); }

    private void record(AuditEventType type, Object entity, String description) {
        Target target = switch (entity) {
            case User value -> new Target(AuditEntityType.USER, value.getId());
            case Category value -> new Target(AuditEntityType.CATEGORY, value.getId());
            case Product value -> new Target(AuditEntityType.PRODUCT, value.getId());
            case Warehouse value -> new Target(AuditEntityType.WAREHOUSE, value.getId());
            case Inventory value -> new Target(AuditEntityType.INVENTORY, value.getId());
            case Order value -> new Target(AuditEntityType.ORDER, value.getId());
            case OrderItem value -> new Target(AuditEntityType.ORDER_ITEM, value.getId());
            case AuditLog value -> new Target(AuditEntityType.ORDER_STATUS_HISTORY, value.getId());
            default -> null;
        };
        if (target != null) writer.record(type, target.type(), target.id(), description + target.type() + ".");
    }

    private record Target(AuditEntityType type, UUID id) { }
}
