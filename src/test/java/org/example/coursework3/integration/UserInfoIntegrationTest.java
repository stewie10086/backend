package org.example.coursework3.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.entity.User;
import org.example.coursework3.repository.UserRepository;
import org.example.coursework3.service.AliyunMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/booking_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=123456",
        "aliyun.mail.access-key-id=test-ak",
        "aliyun.mail.access-key-secret=test-sk",
        "aliyun.mail.region=cn-hangzhou",
        "aliyun.mail.from-address=test@example.com"
})
@AutoConfigureMockMvc(addFilters = false)
class UserInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private AliyunMailService aliyunMailService;

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
                "u1", "Alice", "alice@example.com", "$2a$10$ccRw3eseE.kKG8DYHtPnt.qah3vDo2qRHusxhO1xlNFZjV0PF4FPS", "Customer", null
        );
    }

    @Test
    void getMe_readsUserFromRealDb() throws Exception {
        ValueOperations<String, String> valueOps = mockRedis();
        when(valueOps.get("auth:token:token-u1")).thenReturn("u1");

        mockMvc.perform(get("/me").header("Authorization", "Bearer token-u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("u1"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    void patchMe_updatesUserInRealDb() throws Exception {
        ValueOperations<String, String> valueOps = mockRedis();
        when(valueOps.get("auth:token:token-u1")).thenReturn("u1");

        UpdateSelfInfoRequest request = new UpdateSelfInfoRequest();
        request.setName("Alice Updated");
        request.setAvatar("https://example.com/avatars/u1-new.png");

        mockMvc.perform(patch("/me")
                        .header("Authorization", "Bearer token-u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice Updated"))
                .andExpect(jsonPath("$.data.avatar").value("https://example.com/avatars/u1-new.png"));

        User user = userRepository.findById("u1");
        assertEquals("Alice Updated", user.getName());
        assertEquals("https://example.com/avatars/u1-new.png", user.getAvatar());
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> mockRedis() {
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return valueOps;
    }
}
