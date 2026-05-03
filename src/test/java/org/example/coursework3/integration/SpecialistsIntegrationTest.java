package org.example.coursework3.integration;

import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.SpecialistStatus;
import org.example.coursework3.repository.SpecialistsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/booking_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}",
        "spring.datasource.username=${TEST_DB_USERNAME:root}",
        "spring.datasource.password=${TEST_DB_PASSWORD:123456}",
})
@AutoConfigureMockMvc(addFilters = false)
class SpecialistsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpecialistsRepository specialistsRepository;

    @BeforeEach
    void resetAndSeed() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        jdbcTemplate.execute("TRUNCATE TABLE booking_history");
        jdbcTemplate.execute("TRUNCATE TABLE bookings");
        jdbcTemplate.execute("TRUNCATE TABLE slots");
        jdbcTemplate.execute("TRUNCATE TABLE specialist_expertise");
        jdbcTemplate.execute("TRUNCATE TABLE pricing");
        jdbcTemplate.execute("TRUNCATE TABLE specialists");
        jdbcTemplate.execute("TRUNCATE TABLE expertise");
        jdbcTemplate.execute("TRUNCATE TABLE users");

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");


        // 1. users
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u1", "Alice", "alice@example.com", "hashed_pw_1", "Customer", null
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u2", "Bob", "bob@example.com", "hashed_pw_2", "Customer", null
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u3", "Dr. Smith", "smith@example.com", "hashed_pw_3", "Specialist", null
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u4", "Dr. Lee", "lee@example.com", "hashed_pw_4", "Specialist", null
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u5", "Admin", "admin@example.com", "hashed_pw_5", "Admin", null
        );

        // 2. specialists
        jdbcTemplate.update(
                "INSERT INTO specialists (user_id, name, bio, price, status, created_at, updated_at) VALUES (?,?,?,?,?, NOW(), NOW())",
                "u3", "Dr. Smith", "Psychologist with 10 years experience", new BigDecimal("100.00"), SpecialistStatus.Active.name()
        );
        jdbcTemplate.update(
                "INSERT INTO specialists (user_id, bio, name, price, status, created_at, updated_at) VALUES (?,?,?,?,?, NOW(), NOW())",
                "u4", "Career consultant and coach", "Dr. Lee", new BigDecimal("80.00"), SpecialistStatus.Active.name()
        );

        // 3. expertise
        jdbcTemplate.update(
                "INSERT INTO expertise (id, name, description) VALUES (?,?,?)",
                "e1", "Mental Health", "Psychological counseling"
        );
        jdbcTemplate.update(
                "INSERT INTO expertise (id, name, description) VALUES (?,?,?)",
                "e2", "Career Advice", "Career planning and coaching"
        );
        jdbcTemplate.update(
                "INSERT INTO expertise (id, name, description) VALUES (?,?,?)",
                "e3", "Stress Management", "Stress relief techniques"
        );

        // 4. specialist_expertise
        jdbcTemplate.update(
                "INSERT INTO specialist_expertise (specialist_id, expertise_id) VALUES (?,?)", "u3", "e1"
        );
        jdbcTemplate.update(
                "INSERT INTO specialist_expertise (specialist_id, expertise_id) VALUES (?,?)", "u3", "e3"
        );
        jdbcTemplate.update(
                "INSERT INTO specialist_expertise (specialist_id, expertise_id) VALUES (?,?)", "u4", "e2"
        );

        // 5. slots
        jdbcTemplate.update(
                "INSERT INTO slots (id, specialist_id, start_time, end_time, available, amount, currency, duration, type, detail, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?, NOW(), NOW())",
                "s1", "u3",
                LocalDateTime.of(2026, 3, 25, 10, 0),
                LocalDateTime.of(2026, 3, 25, 11, 0),
                true, new BigDecimal("100.00"), "CNY", 60, "online", "10:00 session"
        );
        jdbcTemplate.update(
                "INSERT INTO slots (id, specialist_id, start_time, end_time, available, amount, currency, duration, type, detail, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?, NOW(), NOW())",
                "s2", "u3",
                LocalDateTime.of(2026, 3, 25, 11, 0),
                LocalDateTime.of(2026, 3, 25, 12, 0),
                true, new BigDecimal("100.00"), "CNY", 60, "online", "11:00 session"
        );
        jdbcTemplate.update(
                "INSERT INTO slots (id, specialist_id, start_time, end_time, available, amount, currency, duration, type, detail, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?, NOW(), NOW())",
                "s3", "u4",
                LocalDateTime.of(2026, 3, 25, 14, 0),
                LocalDateTime.of(2026, 3, 25, 15, 0),
                true, new BigDecimal("80.00"), "CNY", 60, "offline", "14:00 session"
        );

        // 6. bookings
        jdbcTemplate.update(
                "INSERT INTO bookings (id, customer_id, specialist_id, slot_id, note, status, created_at, updated_at) VALUES (?,?,?,?,?,?, NOW(), NOW())",
                "b1", "u1", "u3", "s1", "Need help with anxiety", BookingStatus.Confirmed.name()
        );
        jdbcTemplate.update(
                "INSERT INTO bookings (id, customer_id, specialist_id, slot_id, note, status, created_at, updated_at) VALUES (?,?,?,?,?,?, NOW(), NOW())",
                "b2", "u2", "u4", "s3", "Career switch advice", BookingStatus.Pending.name()
        );

        // 7. booking_history
        jdbcTemplate.update(
                "INSERT INTO booking_history (id, booking_id, status, reason) VALUES (?,?,?,?)",
                "h1", "b1", "Pending", "Initial booking"
        );
        jdbcTemplate.update(
                "INSERT INTO booking_history (id, booking_id, status, reason) VALUES (?,?,?,?)",
                "h2", "b1", "Confirmed", "Specialist accepted"
        );
        jdbcTemplate.update(
                "INSERT INTO booking_history (id, booking_id, status, reason) VALUES (?,?,?,?)",
                "h3", "b2", "Pending", "Waiting for confirmation"
        );

        // 8. pricing
        jdbcTemplate.update(
                "INSERT INTO pricing (id, specialist_id, duration, type, amount, currency, detail) VALUES (?,?,?,?,?,?,?)",
                "p1", "u3", 60, "online", new BigDecimal("100.00"), "USD", "1 hour online session"
        );
        jdbcTemplate.update(
                "INSERT INTO pricing (id, specialist_id, duration, type, amount, currency, detail) VALUES (?,?,?,?,?,?,?)",
                "p2", "u3", 30, "online", new BigDecimal("60.00"), "USD", "30 min quick session"
        );
        jdbcTemplate.update(
                "INSERT INTO pricing (id, specialist_id, duration, type, amount, currency, detail) VALUES (?,?,?,?,?,?,?)",
                "p3", "u4", 60, "offline", new BigDecimal("80.00"), "USD", "Face-to-face consultation"
        );
    }

    @Test
    void getSpecialists_returnsPagedDataFromDb() throws Exception {
        mockMvc.perform(get("/specialists")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[*].id", hasItems("u3", "u4")));
    }

    @Test
    void getSpecialists_filtersByExpertiseId_e1() throws Exception {
        mockMvc.perform(get("/specialists")
                        .param("expertiseId", "e1")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("u3"));
    }

    @Test
    void getSpecialists_filtersByExpertiseId_e2() throws Exception {
        mockMvc.perform(get("/specialists")
                        .param("expertiseId", "e2")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("u4"));
    }

    @Test
    void getSpecialistDetail_returnsDetailFor_u3() throws Exception {
        mockMvc.perform(get("/specialists/u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u3"))
                .andExpect(jsonPath("$.data.name").value("Dr. Smith"))
                .andExpect(jsonPath("$.data.bio").value("Psychologist with 10 years experience"))
                .andExpect(jsonPath("$.data.price").value(100.00))
                .andExpect(jsonPath("$.data.expertise[*].id", hasItems("e1", "e3")));
    }

    @Test
    void getSpecialistSlots_returnsSlotsFor_u3() throws Exception {
        mockMvc.perform(get("/specialists/u3/slots")
                        .param("date", "2026-03-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].slotId").value("s1"))
                .andExpect(jsonPath("$.data[1].slotId").value("s2"));
    }

    @Test
    void getSpecialistSlots_returnsSlotsFor_u4() throws Exception {
        mockMvc.perform(get("/specialists/u4/slots")
                        .param("date", "2026-03-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].slotId").value("s3"));
    }

    @Test
    void countSpecialists_isCorrect() {
        long count = specialistsRepository.count();
        assertEquals(2, count);
    }
}
