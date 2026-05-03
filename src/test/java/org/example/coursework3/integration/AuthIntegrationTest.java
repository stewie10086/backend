package org.example.coursework3.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.EmailLoginRequest;
import org.example.coursework3.dto.request.LoginRequest;
import org.example.coursework3.dto.request.RegisterRequest;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthIntegrationTest {

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
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar) VALUES (?,?,?,?,?,?)",
                "u2", "Bob", "bob@example.com", "$2a$10$ccRw3eseE.kKG8DYHtPnt.qah3vDo2qRHusxhO1xlNFZjV0PF4FPS", "Customer", null
        );
    }

    @Test
    void login_readsRealDbUser() throws Exception {
        ValueOperations<String, String> valueOps = mockRedis();
        when(valueOps.get("auth:user:u1")).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user.id").value("u1"))
                .andExpect(jsonPath("$.data.user.name").value("Alice"))
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    void loginByEmailCode_readsRealDbUser() throws Exception {
        ValueOperations<String, String> valueOps = mockRedis();
        when(valueOps.get("captcha:alice@example.com")).thenReturn("123456");
        when(valueOps.get("auth:user:u1")).thenReturn(null);

        EmailLoginRequest request = new EmailLoginRequest();
        request.setEmail("alice@example.com");
        request.setVerificationCode("123456");

        mockMvc.perform(post("/auth/login-by-email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"));
    }

    @Test
    void register_persistsUserToRealDb() throws Exception {
        ValueOperations<String, String> valueOps = mockRedis();
        when(valueOps.get("captcha:newuser@example.com")).thenReturn("999999");
        when(valueOps.get(startsWith("auth:user:"))).thenReturn(null);

        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("newuser@example.com");
        request.setPassword("new_pw");
        request.setVerificationCode("999999");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"));

        assertTrue(userRepository.findByEmail("newuser@example.com").isPresent());
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> mockRedis() {
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return valueOps;
    }
}
