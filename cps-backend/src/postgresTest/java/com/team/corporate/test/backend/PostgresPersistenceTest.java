package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.repositories.*;
import com.team.corporate.core.config.ApplicationConfiguration;
import com.team.corporate.core.entities.*;
import com.team.corporate.core.repositories.impls.HibernateUsersRepository;
import com.team.corporate.core.services.*;
import com.team.corporate.core.utils.HibernateFactory;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresPersistenceTest {
    private EmbeddedPostgres postgres;
    private ConfigurableApplicationContext application;
    private JdbcTemplate jdbc;

    @BeforeAll
    void startCleanPostgresAndTheRealApplication() throws Exception {
        postgres = EmbeddedPostgres.builder().setServerConfig("listen_addresses", "127.0.0.1").start();
        try {
            application = new SpringApplicationBuilder(BackendMain.class).run(
                    "--server.port=0",
                    "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                    "--spring.datasource.username=postgres", "--spring.datasource.password=postgres",
                    "--spring.flyway.enabled=true", "--spring.jpa.hibernate.ddl-auto=validate",
                    "--cps.security.jwt.secret=cG9zdGdyZXMtdGVzdC1vbmx5LXNlY3JldC0zMi1ieXRlcw==",
                    "--cps.security.bootstrap.username=", "--cps.security.bootstrap.password=");
            jdbc = application.getBean(JdbcTemplate.class);
        } catch (Exception | Error failure) {
            postgres.close();
            throw failure;
        }
    }

    @AfterAll
    void stopApplicationAndRemoveTestDatabase() throws Exception {
        try {
            if (application != null) application.close();
        } finally {
            if (postgres != null) postgres.close();
        }
    }

    @BeforeEach
    void clearTestData() {
        jdbc.execute("TRUNCATE audit_events, audit_logs, order_items, orders, inventory, products, "
                + "categories, warehouses, refresh_tokens, refresh_sessions, user_credentials, users CASCADE");
    }

    @Test
    void allMigrationsAndHibernateValidationWorkOnAnEmptyPostgres18Database() {
        assertTrue(jdbc.queryForObject("SHOW server_version", String.class).startsWith("18.6"));
        var flyway = application.getBean(Flyway.class);
        assertEquals("16", flyway.info().current().getVersion().getVersion());
        assertEquals(16, flyway.info().applied().length);
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals("validate", application.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("user_role", columnType("users", "user_role"));
        assertEquals("order_status", columnType("orders", "order_status"));
        assertEquals("order_status", columnType("audit_logs", "old_status"));
        assertEquals("order_status", columnType("audit_logs", "new_status"));
        assertEquals("varchar", columnType("audit_events", "event_type"));
        assertEquals("varchar", columnType("audit_events", "entity_type"));
        UUID id = jdbc.queryForObject("INSERT INTO categories (name) VALUES ('default UUID') RETURNING id", UUID.class);
        assertEquals(7, id.version());
    }

    @Test
    void nativeRolesAndOrderStatusesCanBeInsertedUpdatedReadAndQueried() {
        var users = application.getBean(SpringUsersRepository.class);
        var orders = application.getBean(SpringOrdersRepository.class);
        var history = application.getBean(SpringAuditLogsRepository.class);
        for (UserRole role : UserRole.values()) {
            User user = users.save(new User("role-" + role, role));
            assertEquals(role, users.findById(user.getId()).orElseThrow().getUserRole());
            user.setUserRole(UserRole.ADMIN);
            users.save(user);
            assertEquals("ADMIN", jdbc.queryForObject("SELECT user_role::text FROM users WHERE id = ?", String.class, user.getId()));
        }
        User author = users.findByUsername("role-ADMIN").orElseThrow();
        Order order = orders.save(new Order(author, OrderStatus.CREATED));
        for (OrderStatus status : OrderStatus.values()) {
            OrderStatus previous = order.getStatus();
            order.setStatus(status);
            order = orders.save(order);
            history.add(new AuditLog(order, author, previous, status));
            assertEquals(status, orders.getById(order.getId()).orElseThrow().getStatus());
            assertEquals(status.name(), jdbc.queryForObject("SELECT order_status::text FROM orders WHERE id = ?", String.class, order.getId()));
            try (var em = application.getBean(EntityManagerFactory.class).createEntityManager()) {
                assertEquals(1, em.createQuery("SELECT o FROM Order o WHERE o.status = :status", Order.class)
                        .setParameter("status", status).getResultList().size());
            }
        }
        assertEquals(OrderStatus.values().length, history.getByOrderId(order.getId()).size());
        var events = application.getBean(AuditEventsRepository.class).findAll();
        assertTrue(events.stream().anyMatch(e -> e.getEventType().name().equals("ORDER_STATUS_CHANGED")));
    }

    @Test
    void serviceOperationsAllowZeroPricesAndDeliveryCanConsumeAllStock() {
        var users = application.getBean(UsersService.class);
        var categories = application.getBean(CategoriesService.class);
        var products = application.getBean(ProductsService.class);
        var warehouses = application.getBean(WarehousesService.class);
        var inventories = application.getBean(InventoriesService.class);
        var orders = application.getBean(OrdersService.class);
        var customer = users.createUser("customer", UserRole.USER);
        var manager = users.createUser("manager", UserRole.MANAGER);
        var category = categories.createCategory("Category");
        var product = products.createProduct("Free product", "ZERO", category.getId(), BigDecimal.ZERO);
        var warehouse = warehouses.createWarehouse("Warehouse", null);
        var inventory = inventories.placeProductInWarehouse(product.getId(), warehouse.getId(), 0);
        inventories.updateInventory(inventory.getId(), 2);
        UUID order = orders.createOrder(customer.getId(), List.of(new OrdersService.Line(product.getId(), 2)), null);
        orders.changeStatus(manager.getId(), order, OrderStatus.APPROVED);
        orders.changeStatus(manager.getId(), order, OrderStatus.DELIVERED);
        assertEquals(0, inventories.getInventory(inventory.getId()).orElseThrow().getQuantity());
        assertEquals(new BigDecimal("0.00"), jdbc.queryForObject("SELECT purchase_price FROM order_items WHERE order_id = ?", BigDecimal.class, order));
        products.updateProduct(product.getId(), null, null, null, new BigDecimal("12.34"));
        products.updateProduct(product.getId(), null, null, null, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), products.getProduct(product.getId()).orElseThrow().getPrice());
        var disposable = products.createProduct("Disposable", "DELETE", category.getId(), BigDecimal.ZERO);
        products.deleteProduct(disposable.getId());
        assertTrue(products.getProduct(disposable.getId()).isEmpty());
    }

    @Test
    void databaseRejectsDuplicateStockAndNegativeValuesEvenWhenServicesAreBypassed() {
        UUID product = jdbc.queryForObject("INSERT INTO products (sku, name, price) VALUES ('RAW', 'Raw', 0) RETURNING id", UUID.class);
        UUID warehouse = jdbc.queryForObject("INSERT INTO warehouses (name) VALUES ('Warehouse') RETURNING id", UUID.class);
        jdbc.update("INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?, ?, 0)", product, warehouse);
        assertSqlState("23505", () -> jdbc.update("INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?, ?, 1)", product, warehouse));
        assertSqlState("23514", () -> jdbc.update("UPDATE inventory SET quantity = -1 WHERE product_id = ?", product));
        assertSqlState("23514", () -> jdbc.update("UPDATE products SET price = -0.01 WHERE id = ?", product));
        UUID user = jdbc.queryForObject("INSERT INTO users (username, full_name, user_role) VALUES ('raw', 'Raw user', 'USER') RETURNING id", UUID.class);
        UUID order = jdbc.queryForObject("INSERT INTO orders (user_id) VALUES (?) RETURNING id", UUID.class, user);
        assertSqlState("23514", () -> jdbc.update("INSERT INTO order_items (order_id, product_id, quantity, purchase_price) VALUES (?, ?, 1, -0.01)", order, product));
        assertSqlState("23514", () -> jdbc.update("INSERT INTO order_items (order_id, product_id, quantity, purchase_price) VALUES (?, ?, 0, 0)", order, product));
        UUID secondWarehouse = jdbc.queryForObject("INSERT INTO warehouses (name) VALUES ('Second warehouse') RETURNING id", UUID.class);
        jdbc.update("INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?, ?, 0)", product, secondWarehouse);
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM inventory", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT sum(quantity) FROM inventory", Integer.class));
    }

    @Test
    void auditWritesRollBackWithFlushedBusinessChangesOnPostgres() {
        var products = application.getBean(SpringProductsRepository.class);
        var factory = application.getBean(EntityManagerFactory.class);
        var transactions = new TransactionTemplate(application.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class, () -> transactions.execute(status -> {
            products.save(new Product("ROLLBACK", "Rollback", null, BigDecimal.ZERO));
            EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_events", Integer.class));
            throw new IllegalStateException("Rollback test");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM products", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM audit_events", Integer.class));
    }

    @Test
    void existingDatabaseUpgradesWithoutLosingDataOrLegacyHistory() {
        var legacy = legacyDatabase("upgrade_test", "14");
        var ids = seedLegacyStock(legacy);
        UUID actor = legacy.queryForObject("INSERT INTO users (username, full_name, user_role) VALUES ('legacy', 'Legacy', 'MANAGER') RETURNING id", UUID.class);
        UUID order = legacy.queryForObject("INSERT INTO orders (user_id, order_status) VALUES (?, 'APPROVED') RETURNING id", UUID.class, actor);
        UUID history = legacy.queryForObject("INSERT INTO audit_logs (order_id, changed_by, old_status, new_status) VALUES (?, ?, 'CREATED', 'APPROVED') RETURNING id", UUID.class, order, actor);
        var migration = flyway("upgrade_test");
        assertEquals(2, migration.migrate().migrationsExecuted);
        assertEquals(1, legacy.queryForObject("SELECT count(*) FROM audit_logs WHERE id = ?", Integer.class, history));
        assertEquals(1, legacy.queryForObject("SELECT count(*) FROM audit_events WHERE id = ? AND actor_id = ? AND event_type = 'ORDER_STATUS_CHANGED'", Integer.class, history, actor));
        legacy.update("UPDATE products SET price = 0 WHERE id = ?", ids.product());
        legacy.update("UPDATE inventory SET quantity = 0 WHERE id = ?", ids.inventory());
        assertEquals(0, migration.migrate().migrationsExecuted);
        assertTrue(migration.validateWithResult().validationSuccessful);
    }

    @Test
    void upgradeRefusesDuplicateStockWithoutDeletingOrChangingExistingRows() {
        var legacy = legacyDatabase("duplicates_test", "15");
        var ids = seedLegacyStock(legacy);
        UUID duplicate = legacy.queryForObject("INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?, ?, 4) RETURNING id", UUID.class, ids.product(), ids.warehouse());
        var migration = flyway("duplicates_test");
        assertThrows(FlywayException.class, migration::migrate);
        assertEquals(2, legacy.queryForObject("SELECT count(*) FROM inventory", Integer.class));
        assertEquals(7, legacy.queryForObject("SELECT sum(quantity) FROM inventory", Integer.class));
        // PostgreSQL rolls back all of V16, including the earlier CHECK changes.
        assertSqlState("23514", () -> legacy.update("UPDATE products SET price = 0 WHERE id = ?", ids.product()));
        assertEquals("15", migration.info().current().getVersion().getVersion());
        // Resolve only this test fixture, then verify the same migration can be retried.
        legacy.update("DELETE FROM inventory WHERE id = ?", duplicate);
        assertEquals(1, migration.migrate().migrationsExecuted);
        assertSqlState("23505", () -> legacy.update("INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?, ?, 1)", ids.product(), ids.warehouse()));
    }

    @Test
    void standaloneCoreHibernateRepositoriesRemainCompatibleWithTheMigratedSchema() {
        var properties = new Properties();
        properties.setProperty("db.url", postgres.getJdbcUrl("postgres", "postgres"));
        properties.setProperty("db.username", "postgres");
        properties.setProperty("db.password", "postgres");
        properties.setProperty("hibernate.ddl.auto", "validate");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");
        try (var factory = HibernateFactory.createSessionFactory(ApplicationConfiguration.fromProperties(properties))) {
            var users = new HibernateUsersRepository(factory);
            User user = users.add(new User("standalone", UserRole.ADMIN));
            user.setUserRole(UserRole.MANAGER);
            users.update(user);
            assertEquals(UserRole.MANAGER, users.getById(user.getId()).orElseThrow().getUserRole());
        }
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject("SELECT udt_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?", String.class, table, column);
    }

    private JdbcTemplate legacyDatabase(String name, String target) {
        // Names are fixed test literals, never caller input. This cluster belongs to this test only.
        jdbc.execute("CREATE DATABASE " + name);
        Flyway.configure().dataSource(postgres.getJdbcUrl("postgres", name), "postgres", "postgres")
                .locations("classpath:database/migrations").target(target).load().migrate();
        return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl("postgres", name), "postgres", "postgres"));
    }

    private Flyway flyway(String database) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl("postgres", database), "postgres", "postgres")
                .locations("classpath:database/migrations").load();
    }

    private static LegacyStock seedLegacyStock(JdbcTemplate legacy) {
        UUID product = legacy.queryForObject("INSERT INTO products (sku, name, price) VALUES ('OLD', 'Legacy product', 12.34) RETURNING id", UUID.class);
        UUID warehouse = legacy.queryForObject("INSERT INTO warehouses (name) VALUES ('Legacy warehouse') RETURNING id", UUID.class);
        UUID inventory = legacy.queryForObject("INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?, ?, 3) RETURNING id", UUID.class, product, warehouse);
        return new LegacyStock(product, warehouse, inventory);
    }

    private static void assertSqlState(String expected, Runnable action) {
        var failure = assertThrows(DataIntegrityViolationException.class, action::run);
        var sql = assertInstanceOf(SQLException.class, failure.getMostSpecificCause());
        assertEquals(expected, sql.getSQLState());
    }

    private record LegacyStock(UUID product, UUID warehouse, UUID inventory) { }
}
