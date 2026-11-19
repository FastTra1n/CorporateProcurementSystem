package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.audit.AuditEventWriter;
import com.team.corporate.backend.entities.*;
import com.team.corporate.backend.filter.AuditFilter;
import com.team.corporate.backend.filter.CorrelationIdFilter;
import com.team.corporate.backend.repositories.*;
import com.team.corporate.backend.security.SessionJwtAuthenticationConverter;
import com.team.corporate.backend.services.AuditEventsService;
import com.team.corporate.core.entities.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend-audit;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "cps.security.bootstrap.username=", "cps.security.bootstrap.password=",
        "cps.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "cps.security.rate-limit.account-attempts=100", "cps.security.rate-limit.ip-attempts=1000"
})
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.TestTime.class)
class AuditIntegrationTest {
    private static final String PASSWORD = "audit-password-123";
    private static final String NEW_PASSWORD = "changed-audit-password-456";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SpringUsersRepository users;
    @Autowired private UserCredentialsRepository credentials;
    @Autowired private RefreshSessionsRepository sessions;
    @Autowired private AuditEventsRepository events;
    @Autowired private AuditEventWriter writer;
    @Autowired private AuditEventsService auditService;
    @Autowired private SpringCategoriesRepository categories;
    @Autowired private SpringProductsRepository products;
    @Autowired private SpringWarehousesRepository warehouses;
    @Autowired private SpringInventoriesRepository inventories;
    @Autowired private SpringOrdersRepository orders;
    @Autowired private SpringOrderItemsRepository items;
    @Autowired private SpringAuditLogsRepository history;
    @Autowired private PasswordEncoder encoder;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private JwtDecoder decoder;
    @Autowired private SessionJwtAuthenticationConverter converter;
    @Autowired private SecurityIntegrationTest.MutableClock clock;
    @PersistenceContext private EntityManager entityManager;
    private User admin;
    private User worker;
    private User supervisor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        MDC.clear();
        clock.advance(Duration.ofDays(1));
        jdbc.batchUpdate("DELETE FROM refresh_tokens", "DELETE FROM refresh_sessions", "DELETE FROM user_credentials",
                "DELETE FROM audit_logs", "DELETE FROM order_items", "DELETE FROM orders", "DELETE FROM inventory",
                "DELETE FROM products", "DELETE FROM categories", "DELETE FROM warehouses", "DELETE FROM users",
                "DELETE FROM audit_events");
        admin = permanentUser("audit-admin", UserRole.ADMIN);
        worker = permanentUser("audit-worker", UserRole.USER);
        supervisor = permanentUser("audit-manager", UserRole.MANAGER);
        events.deleteAllInBatch();
    }

    @Test
    void accountChangesRecordTheAuthenticatedActorAndRequestCorrelation() throws Exception {
        Tokens administrator = login("audit-admin");
        MvcResult created = mvc.perform(post("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(administrator))
                .header(CorrelationIdFilter.HEADER, "create-user-test").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("username", "created-user", "fullName", "Created User",
                        "password", PASSWORD, "role", "USER")))).andReturn();
        assertEquals(201, created.getResponse().getStatus(), created.getResponse().getContentAsString());
        UUID userId = UUID.fromString(json(created).get("id").asString());
        AuditEvent event = single(AuditEventType.CREATE, userId);
        assertEquals(AuditEntityType.USER, event.getEntityType());
        assertEquals(admin.getId(), event.getActorId());
        assertEquals("audit-admin", event.getActorUsername());
        assertEquals("create-user-test", event.getCorrelationId());
        assertEquals("create-user-test", created.getResponse().getHeader(CorrelationIdFilter.HEADER));
        MvcResult result = audit(administrator, Map.of("entityId", userId.toString(), "correlationId", "create-user-test"));
        assertEquals(200, result.getResponse().getStatus());
        assertEquals(1, json(result).get("totalElements").asInt());
        assertEquals(event.getId().toString(), json(result).get("content").get(0).get("id").asString());
        assertNoSecrets(result.getResponse().getContentAsString());
    }

    @Test
    void failedLoginIsRecordedAfterRollbackAndSuccessfulLoginHasAVerifiedActor() throws Exception {
        MvcResult failed = loginAttempt("audit-worker", "wrong-password", "failed-login-test");
        assertEquals(401, failed.getResponse().getStatus());
        assertEquals("failed-login-test", json(failed).get("correlationId").asString());
        AuditEvent failure = single(AuditEventType.LOGIN_FAILURE, null);
        assertNull(failure.getActorId());
        assertNull(failure.getEntityId());
        assertEquals("audit-worker", failure.getActorUsername());
        assertEquals("failed-login-test", failure.getCorrelationId());
        assertEquals(0, sessions.count());

        MvcResult succeeded = loginAttempt("audit-worker", PASSWORD, "successful-login-test");
        assertEquals(200, succeeded.getResponse().getStatus());
        AuditEvent success = single(AuditEventType.LOGIN_SUCCESS, worker.getId());
        assertEquals(worker.getId(), success.getActorId());
        assertEquals("audit-worker", success.getActorUsername());
        assertEquals("successful-login-test", success.getCorrelationId());
        for (AuditEvent event : events.findAll()) assertNoSecrets(event.getDescription());
    }

    @Test
    void auditAndBusinessDataRollBackTogetherEvenAfterFlush() throws Exception {
        Tokens administrator = login("audit-admin");
        events.deleteAllInBatch();
        AtomicReference<UUID> id = new AtomicReference<>();
        withActor(administrator, () -> assertThrows(IllegalStateException.class,
                () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
                    Category category = categories.add(new Category("Rolled back category"));
                    id.set(category.getId());
                    entityManager.flush();
                    throw new IllegalStateException("Rollback probe");
                })));
        assertTrue(categories.getById(id.get()).isEmpty());
        assertEquals(0, events.count());
    }

    @Test
    void rejectedLoginEventSurvivesAnOuterTransactionRollback() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            writer.loginFailed("unknown-user");
            throw new IllegalStateException("Rollback probe");
        }));
        assertEquals("unknown-user", single(AuditEventType.LOGIN_FAILURE, null).getActorUsername());
    }

    @Test
    void domainChangesAreAuditedAndDeletingDataPreservesItsHistory() throws Exception {
        Tokens administrator = login("audit-admin");
        events.deleteAllInBatch();
        AtomicReference<Product> productRef = new AtomicReference<>();
        withActor(administrator, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            User buyer = users.add(new User("buyer", UserRole.USER));
            Category category = categories.add(new Category("Category"));
            Product product = products.add(new Product("AUDIT-SKU", "Product", category, new BigDecimal("10.00")));
            productRef.set(product);
            Warehouse warehouse = warehouses.add(new Warehouse("Warehouse"));
            inventories.add(new Inventory(product, warehouse, 5));
            Order order = orders.add(new Order(buyer, OrderStatus.CREATED));
            items.add(new OrderItem(order, product, 1, product.getPrice()));
            history.add(new AuditLog(order, admin, OrderStatus.CREATED, OrderStatus.APPROVED));
        }));
        Set<AuditEntityType> created = events.findAll().stream()
                .filter(event -> event.getEventType() == AuditEventType.CREATE).map(AuditEvent::getEntityType)
                .collect(Collectors.toSet());
        assertEquals(Set.of(AuditEntityType.USER, AuditEntityType.CATEGORY, AuditEntityType.PRODUCT,
                AuditEntityType.WAREHOUSE, AuditEntityType.INVENTORY, AuditEntityType.ORDER, AuditEntityType.ORDER_ITEM), created);
        assertEquals(1, events.findAll().stream().filter(event -> event.getEventType() == AuditEventType.ORDER_STATUS_CHANGED).count());
        for (AuditEvent event : events.findAll()) {
            assertEquals(admin.getId(), event.getActorId());
            assertEquals("audit-admin", event.getActorUsername());
        }

        Product product = productRef.get();
        withActor(administrator, () -> {
            product.setName("Updated product");
            products.update(product);
            for (OrderItem item : items.getByProductId(product.getId())) items.delete(item.getId());
            for (Inventory inventory : inventories.getByProductId(product.getId())) inventories.delete(inventory.getId());
            products.delete(product.getId());
        });
        assertTrue(products.getById(product.getId()).isEmpty());
        assertNotNull(single(AuditEventType.CREATE, product.getId()));
        assertNotNull(single(AuditEventType.UPDATE, product.getId()));
        assertNotNull(single(AuditEventType.DELETE, product.getId()));
    }

    @Test
    void userUpdatesPasswordChangesAndResetsHaveSafeDescriptions() throws Exception {
        Tokens administrator = login("audit-admin");
        Tokens employee = login("audit-worker");
        MvcResult updated = mvc.perform(put("/api/admin/users/" + worker.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(administrator)).header(CorrelationIdFilter.HEADER, "update-user-test")
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of(
                        "username", "audit-worker", "fullName", "Updated worker", "role", "USER", "enabled", true)))).andReturn();
        assertEquals(200, updated.getResponse().getStatus());
        AuditEvent update = single(AuditEventType.UPDATE, worker.getId());
        assertEquals(admin.getId(), update.getActorId());
        assertEquals("update-user-test", update.getCorrelationId());

        MvcResult changed = mvc.perform(put("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(employee))
                .header(CorrelationIdFilter.HEADER, "change-password-test").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD)))).andReturn();
        assertEquals(204, changed.getResponse().getStatus());
        AuditEvent change = single(AuditEventType.PASSWORD_CHANGED, worker.getId());
        assertEquals(worker.getId(), change.getActorId());
        assertEquals("change-password-test", change.getCorrelationId());
        MvcResult reset = mvc.perform(put("/api/admin/users/" + worker.getId() + "/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(administrator)).header(CorrelationIdFilter.HEADER, "reset-password-test")
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of("password", PASSWORD)))).andReturn();
        assertEquals(204, reset.getResponse().getStatus());
        assertEquals(admin.getId(), single(AuditEventType.PASSWORD_RESET, worker.getId()).getActorId());
        for (AuditEvent event : events.findAll()) {
            assertNoSecrets(event.getDescription());
            assertFalse(event.getDescription().contains(administrator.access()));
            assertFalse(event.getDescription().contains(administrator.refresh()));
            assertFalse(event.getDescription().contains(employee.access()));
            assertFalse(event.getDescription().contains(employee.refresh()));
        }
    }

    @Test
    void failedPasswordChangeAndNoOpUpdateDoNotProduceSuccessEvents() throws Exception {
        Tokens employee = login("audit-worker");
        Tokens administrator = login("audit-admin");
        MvcResult failed = mvc.perform(put("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(employee))
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of(
                        "currentPassword", "wrong-password", "newPassword", NEW_PASSWORD)))).andReturn();
        assertEquals(401, failed.getResponse().getStatus());
        MvcResult unchanged = mvc.perform(put("/api/admin/users/" + worker.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(administrator)).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("username", "audit-worker", "fullName", "audit-worker",
                        "role", "USER", "enabled", true)))).andReturn();
        assertEquals(200, unchanged.getResponse().getStatus());
        assertEquals(0, events.findAll().stream().filter(event -> event.getEventType() == AuditEventType.PASSWORD_CHANGED
                || event.getEventType() == AuditEventType.UPDATE).count());
    }

    @Test
    void logoutIsRecordedOnceAndRefreshReplayEventCommitsWithRevocation() throws Exception {
        Tokens original = login("audit-worker");
        MvcResult first = mvc.perform(post("/api/auth/logout").header(CorrelationIdFilter.HEADER, "first-logout")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody(original))).andReturn();
        assertEquals(204, first.getResponse().getStatus());
        assertEquals(204, mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(original))).andReturn().getResponse().getStatus());
        var logouts = events.findAll().stream().filter(event -> event.getEventType() == AuditEventType.LOGOUT).toList();
        assertEquals(1, logouts.size());
        assertEquals(worker.getId(), logouts.getFirst().getActorId());
        assertEquals("first-logout", logouts.getFirst().getCorrelationId());

        Tokens otherSession = login("audit-worker");
        Tokens rotated = tokens(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(otherSession))).andReturn());
        MvcResult replay = mvc.perform(post("/api/auth/refresh").header(CorrelationIdFilter.HEADER, "refresh-replay")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody(otherSession))).andReturn();
        assertEquals(401, replay.getResponse().getStatus());
        var revocations = events.findAll().stream().filter(event -> event.getEventType() == AuditEventType.SESSION_REVOKED).toList();
        assertEquals(1, revocations.size());
        assertEquals(worker.getId(), revocations.getFirst().getActorId());
        assertEquals("refresh-replay", revocations.getFirst().getCorrelationId());
        assertEquals(401, mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(rotated)))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void filtersSearchAndSortingAreAppliedOnTheServer() throws Exception {
        Tokens administrator = login("audit-admin");
        events.deleteAllInBatch();
        UUID productId = UUID.randomUUID();
        Instant firstTime = clock.instant();
        seed(AuditEventType.CREATE, AuditEntityType.PRODUCT, productId, admin.getId(), "Alpha",
                "Price 100% _literal_ \\ path", "filter-one");
        clock.advance(Duration.ofSeconds(1));
        seed(AuditEventType.UPDATE, AuditEntityType.USER, worker.getId(), worker.getId(), "Beta", "Changed user", "filter-two");
        clock.advance(Duration.ofSeconds(1));
        seed(AuditEventType.DELETE, AuditEntityType.PRODUCT, productId, worker.getId(), "Beta", "Deleted product", "filter-three");

        MvcResult all = audit(administrator, Map.of());
        assertEquals(3, json(all).get("totalElements").asInt());
        assertEquals("DELETE", json(all).get("content").get(0).get("eventType").asString());
        assertEquals("CREATE", json(audit(administrator, Map.of("direction", "asc"))).get("content").get(0).get("eventType").asString());
        assertEquals("Alpha", json(audit(administrator, Map.of("sort", "actorUsername", "direction", "asc")))
                .get("content").get(0).get("actorUsername").asString());

        MvcResult exact = audit(administrator, Map.of("actorId", admin.getId().toString(), "eventType", "CREATE",
                "entityType", "PRODUCT", "entityId", productId.toString(), "correlationId", "filter-one",
                "from", firstTime.toString(), "to", firstTime.toString()));
        assertEquals(1, json(exact).get("totalElements").asInt());
        assertEquals("filter-one", json(exact).get("content").get(0).get("correlationId").asString());
        assertEquals(2, json(audit(administrator, Map.of("actorId", worker.getId().toString()))).get("totalElements").asInt());
        for (String q : List.of("ALPHA", "%", "_literal_", "\\", "filter-one")) {
            assertEquals(1, json(audit(administrator, Map.of("q", q))).get("totalElements").asInt(), q);
        }
        assertEquals(0, json(audit(administrator, Map.of("q", "' OR 1=1 --"))).get("totalElements").asInt());
        assertEquals(0, json(audit(administrator, Map.of("entityId", UUID.randomUUID().toString()))).get("totalElements").asInt());
        assertEquals(0, json(audit(administrator, Map.of("page", "10"))).get("content").size());
        for (String sort : List.of("id", "actorId", "eventType", "entityType", "entityId", "correlationId")) {
            assertEquals(200, audit(administrator, Map.of("sort", sort)).getResponse().getStatus());
        }
        assertEquals(3, events.count());
    }

    @Test
    void paginationHasStableTieBreakingAndDoesNotDuplicateRecords() throws Exception {
        Tokens administrator = login("audit-admin");
        events.deleteAllInBatch();
        for (int i = 0; i < 4; i++) {
            seed(AuditEventType.UPDATE, AuditEntityType.USER, worker.getId(), admin.getId(), "Admin", "Update", "same-time");
        }
        MvcResult all = audit(administrator, Map.of());
        MvcResult first = audit(administrator, Map.of("page", "0", "size", "2"));
        MvcResult second = audit(administrator, Map.of("page", "1", "size", "2"));
        assertEquals(4, json(first).get("totalElements").asInt());
        assertEquals(2, json(first).get("totalPages").asInt());
        for (int i = 0; i < 2; i++) {
            assertEquals(json(all).get("content").get(i).get("id"), json(first).get("content").get(i).get("id"));
            assertEquals(json(all).get("content").get(i + 2).get("id"), json(second).get("content").get(i).get("id"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "sort=passwordHash", "direction=sideways",
            "actorId=not-a-uuid", "entityId=not-a-uuid", "eventType=UNKNOWN", "entityType=UNKNOWN", "from=invalid",
            "from=2026-11-19T00:00:00Z&to=2026-11-18T00:00:00Z", "page=2147483647&size=100"})
    void invalidAuditQueriesReturnSafeProblems(String parameters) throws Exception {
        Tokens administrator = login("audit-admin");
        var query = new java.util.HashMap<String, String>();
        for (String parameter : parameters.split("&")) {
            String[] pair = parameter.split("=", 2);
            query.put(pair[0], pair[1]);
        }
        MvcResult result = audit(administrator, query);
        assertEquals(400, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        assertEquals(result.getResponse().getHeader(CorrelationIdFilter.HEADER), json(result).get("correlationId").asString());
        assertNoSecrets(result.getResponse().getContentAsString());
    }

    @Test
    void longAndInjectionLikeParametersAreBoundedOrRejected() throws Exception {
        Tokens administrator = login("audit-admin");
        for (Map<String, String> query : List.of(Map.of("q", "x".repeat(256)), Map.of("correlationId", "x".repeat(65)),
                Map.of("sort", "occurredAt; DROP TABLE audit_events"))) {
            assertEquals(400, audit(administrator, query).getResponse().getStatus());
        }
        assertTrue(events.count() > 0);
    }

    @Test
    void auditIsReadOnlyAndRestrictedToUnrestrictedAdministrators() throws Exception {
        assertEquals(401, audit(null, Map.of()).getResponse().getStatus());
        assertEquals(403, audit(login("audit-worker"), Map.of()).getResponse().getStatus());
        assertEquals(403, audit(login("audit-manager"), Map.of()).getResponse().getStatus());
        User temporary = users.save(new User("temporary-admin", UserRole.ADMIN));
        credentials.save(new UserCredentials(temporary, encoder.encode(PASSWORD)));
        MvcResult restricted = audit(login("temporary-admin"), Map.of());
        assertEquals(403, restricted.getResponse().getStatus());
        assertEquals("PASSWORD_CHANGE_REQUIRED", json(restricted).get("code").asString());
        Tokens administrator = login("audit-admin");
        assertEquals(200, audit(administrator, Map.of()).getResponse().getStatus());
        assertEquals(405, mvc.perform(post("/api/admin/audit").header(HttpHeaders.AUTHORIZATION, bearer(administrator)))
                .andReturn().getResponse().getStatus());
        assertEquals(405, mvc.perform(delete("/api/admin/audit").header(HttpHeaders.AUTHORIZATION, bearer(administrator)))
                .andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(roles = "MANAGER", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void auditMethodSecurityCannotBeBypassed() {
        assertThrows(AccessDeniedException.class, () -> auditService.getEvents(
                new AuditFilter(null, null, null, null, null, null, null, null), 0, 20, "occurredAt", "desc"));
    }

    private void seed(AuditEventType type, AuditEntityType entityType, UUID entityId, UUID actorId,
                      String actor, String description, String correlationId) {
        MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        try {
            new TransactionTemplate(manager).executeWithoutResult(status ->
                    writer.recordAs(type, entityType, entityId, description, actorId, actor));
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    private String refreshBody(Tokens tokens) {
        return JSON.writeValueAsString(Map.of("refreshToken", tokens.refresh()));
    }

    private User permanentUser(String username, UserRole role) {
        User user = users.save(new User(username, role));
        UserCredentials account = new UserCredentials(user, encoder.encode(PASSWORD));
        account.setMustChangePassword(false);
        credentials.save(account);
        return user;
    }

    private Tokens login(String username) throws Exception {
        return tokens(loginAttempt(username, PASSWORD, UUID.randomUUID().toString()));
    }

    private MvcResult loginAttempt(String username, String password, String correlationId) throws Exception {
        return mvc.perform(post("/api/auth/login").header(CorrelationIdFilter.HEADER, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("username", username, "password", password)))).andReturn();
    }

    private Tokens tokens(MvcResult result) throws Exception {
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return new Tokens(json(result).get("accessToken").asString(), json(result).get("refreshToken").asString());
    }

    private MvcResult audit(Tokens tokens, Map<String, String> parameters) throws Exception {
        var request = get("/api/admin/audit");
        if (tokens != null) request.header(HttpHeaders.AUTHORIZATION, bearer(tokens));
        parameters.forEach((key, value) -> request.param(key, value));
        return mvc.perform(request).andReturn();
    }

    private AuditEvent single(AuditEventType type, UUID entityId) {
        var found = events.findAll().stream().filter(event -> event.getEventType() == type
                && java.util.Objects.equals(event.getEntityId(), entityId)).toList();
        assertEquals(1, found.size(), "Expected one " + type + " for " + entityId);
        return found.getFirst();
    }

    private void withActor(Tokens tokens, Runnable action) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(converter.convert(decoder.decode(tokens.access())));
        SecurityContextHolder.setContext(context);
        try { action.run(); }
        finally { SecurityContextHolder.setContext(previous); }
    }

    private JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertNoSecrets(String value) {
        for (String secret : List.of(PASSWORD, NEW_PASSWORD, "passwordHash", "password_hash", "{bcrypt}")) {
            assertFalse(value.contains(secret));
        }
    }

    private String bearer(Tokens tokens) { return "Bearer " + tokens.access(); }
    private record Tokens(String access, String refresh) { }
}
