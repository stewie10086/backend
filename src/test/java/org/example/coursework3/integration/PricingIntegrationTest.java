package org.example.coursework3.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.PricingQuoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/booking_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=123456",
})
@AutoConfigureMockMvc(addFilters = false)
class PricingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;


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

        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u3", "Dr. Smith", "smith@example.com", "hashed_pw_3", "Specialist", null
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u4", "Dr. Lee", "lee@example.com", "hashed_pw_4", "Specialist", null
        );
        jdbcTemplate.update(
                "INSERT INTO specialists (user_id, bio, price, status) VALUES (?,?,?,?)",
                "u3", "Psychologist with 10 years experience", 100.00, "Active"
        );
        jdbcTemplate.update(
                "INSERT INTO specialists (user_id, bio, price, status) VALUES (?,?,?,?)",
                "u4", "Career consultant and coach", 80.00, "Active"
        );
        jdbcTemplate.update(
                "INSERT INTO pricing (id, specialist_id, duration, type, amount, currency, detail) VALUES (?,?,?,?,?,?,?)",
                "p1", "u3", 60, "online", 100.00, "USD", "1 hour online session"
        );
        jdbcTemplate.update(
                "INSERT INTO pricing (id, specialist_id, duration, type, amount, currency, detail) VALUES (?,?,?,?,?,?,?)",
                "p3", "u4", 60, "offline", 80.00, "USD", "Face-to-face consultation"
        );
    }

    @Test
    void quoteP1_readsRealDbPricing() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("u3", 60, "online");
        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-any")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].amount").value(100.0))
                .andExpect(jsonPath("$.data[0].currency").value("USD"))
                .andExpect(jsonPath("$.data[0].detail").value("1 hour online session"));
    }

    @Test
    void quoteP3_readsRealDbPricing() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("u4", 60, "offline");
        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-any")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].amount").value(80.0))
                .andExpect(jsonPath("$.data[0].detail").value("Face-to-face consultation"));
    }

    @Test
    void quoteBlankSpecialistId_returnsUnauthorized() throws Exception {
        PricingQuoteRequest request = new PricingQuoteRequest("", 60, "online");
        mockMvc.perform(post("/pricing/quote")
                        .header("Authorization", "Bearer token-any")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("specialistId is required"));
    }
}
