package org.example.coursework3.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.coursework3.dto.request.CreateBookingRequest;
import org.example.coursework3.dto.request.RejectRequest;
import org.example.coursework3.dto.request.RescheduleBookingRequest;
import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.PaymentDraft;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.entity.SpecialistStatus;
import org.example.coursework3.repository.BookingRepository;
import org.example.coursework3.repository.SlotRepository;
import org.example.coursework3.service.AlipayGatewayService;
import org.example.coursework3.service.AliyunMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/booking_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}",
        "spring.datasource.username=${TEST_DB_USERNAME:root}",
        "spring.datasource.password=${TEST_DB_PASSWORD:123456}",
        "aliyun.mail.access-key-id=test-ak",
        "aliyun.mail.access-key-secret=test-sk",
        "aliyun.mail.region=cn-hangzhou",
        "aliyun.mail.from-address=test@example.com",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc(addFilters = false)
class BookingIntegrationTest {

    private static final String CUSTOMER_AUTH = "Bearer token-u1";
    private static final String SPECIALIST_AUTH = "Bearer token-u3";
    private static final String CUSTOMER_ID = "u1";
    private static final String SPECIALIST_ID = "u3";
    private static final String SLOT_ID = "s1";
    private static final String NEW_SLOT_ID = "s2";
    private static final String PAYMENT_ID = "p1";
    private static final String PAYMENT_INTENT_ID = "intent-1";
    private static final String PAYMENT_DRAFT_KEY = "booking:payment:draft:";
    private static final String OUT_TRADE_KEY = "booking:payment:outTrade:";
    private static final String USER_UNPAID_KEY = "booking:payment:user:";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SlotRepository slotRepository;

    @MockBean(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @MockBean(name = "redisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private AliyunMailService aliyunMailService;

    @MockBean
    private AlipayGatewayService alipayGatewayService;

    private ValueOperations<String, Object> paymentValueOps;
    private ZSetOperations<String, Object> zSetOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUpRedisMocks() {
        ValueOperations<String, String> authValueOps = Mockito.mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(authValueOps);
        when(authValueOps.get("auth:token:token-u1")).thenReturn(CUSTOMER_ID);
        when(authValueOps.get("auth:token:token-u3")).thenReturn(SPECIALIST_ID);

        paymentValueOps = Mockito.mock(ValueOperations.class);
        zSetOps = Mockito.mock(ZSetOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(paymentValueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @BeforeEach
    void resetAndSeedDatabase() {
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
                "INSERT INTO users (id, name, email, password_hash, role, avatar, created_at, updated_at) VALUES (?,?,?,?,?,?,NOW(),NOW())",
                CUSTOMER_ID, "Alice", "alice@example.com", "hashed_pw_1", "Customer", null
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, role, avatar, created_at, updated_at) VALUES (?,?,?,?,?,?,NOW(),NOW())",
                SPECIALIST_ID, "Dr. Smith", "smith@example.com", "hashed_pw_3", "Specialist", null
        );
        jdbcTemplate.update(
                "INSERT INTO specialists (user_id, name, bio, price, status, created_at, updated_at) VALUES (?,?,?,?,?,NOW(),NOW())",
                SPECIALIST_ID, "Dr. Smith", "Psychologist", new BigDecimal("100.00"), SpecialistStatus.Active.name()
        );
        insertSlot(SLOT_ID, true);
        insertSlot(NEW_SLOT_ID, true);
    }

    //createsBookingAndLocksSlot_whenPaidDraftMatchesRequest
    @Test
    void createBooking() throws Exception {
        stubPaidDraft(PAYMENT_ID, paidDraft(CUSTOMER_ID, SPECIALIST_ID, SLOT_ID, true));

        mockMvc.perform(post("/bookings")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(SPECIALIST_ID, SLOT_ID, PAYMENT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.specialistId").value(SPECIALIST_ID))
                .andExpect(jsonPath("$.data.slotId").value(SLOT_ID))
                .andExpect(jsonPath("$.data.status").value(BookingStatus.Pending.name()));

        List<Booking> bookings = bookingRepository.findByCustomerId(CUSTOMER_ID);
        assertEquals(1, bookings.size());
        Booking booking = bookings.get(0);
        assertNotNull(booking.getId());
        assertEquals("test booking", booking.getNote());
        assertEquals(BookingStatus.Pending, booking.getStatus());
        assertFalse(slot(SLOT_ID).getAvailable());

        verify(redisTemplate).delete(PAYMENT_DRAFT_KEY + PAYMENT_INTENT_ID);
        verify(redisTemplate).delete(OUT_TRADE_KEY + PAYMENT_ID);
        verify(zSetOps).remove(USER_UNPAID_KEY + CUSTOMER_ID, PAYMENT_INTENT_ID);
    }

    //marksBookingCancelledAndReleasesSlot
    @Test
    void cancelBooking() throws Exception {
        insertPendingBooking("b-cancel", SLOT_ID);

        mockMvc.perform(post("/bookings/b-cancel/cancel")
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("b-cancel"))
                .andExpect(jsonPath("$.data.status").value(BookingStatus.Cancelled.name()));

        assertEquals(BookingStatus.Cancelled, booking("b-cancel").getStatus());
        assertTrue(slot(SLOT_ID).getAvailable());
    }

//  movesBookingToNewSlotAndReleasesOldSlot
    @Test
    void rescheduleBooking() throws Exception {
        insertPendingBooking("b-reschedule", SLOT_ID);
        RescheduleBookingRequest request = new RescheduleBookingRequest();
        request.setSlotId(NEW_SLOT_ID);

        mockMvc.perform(post("/bookings/b-reschedule/reschedule")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("b-reschedule"))
                .andExpect(jsonPath("$.data.status").value(BookingStatus.Pending.name()))
                .andExpect(jsonPath("$.data.slotId").value(NEW_SLOT_ID));

        Booking booking = booking("b-reschedule");
        assertEquals(BookingStatus.Pending, booking.getStatus());
        assertEquals(NEW_SLOT_ID, booking.getSlotId());
        assertTrue(slot(SLOT_ID).getAvailable());
        assertFalse(slot(NEW_SLOT_ID).getAvailable());
    }

    //marksBookingConfirmedAndKeepsSlotLocked
    @Test
    void specialistConfirmBooking() throws Exception {
        insertPendingBooking("b-confirm", SLOT_ID);

        mockMvc.perform(post("/specialist/bookings/b-confirm/confirm")
                        .header("Authorization", SPECIALIST_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("b-confirm"))
                .andExpect(jsonPath("$.data.status").value(BookingStatus.Confirmed.name()));

        assertEquals(BookingStatus.Confirmed, booking("b-confirm").getStatus());
        assertFalse(slot(SLOT_ID).getAvailable());
    }

    //marksBookingRejectedAndReleasesSlot
    @Test
    void specialistRejectBooking() throws Exception {
        insertPendingBooking("b-reject", SLOT_ID);
        RejectRequest request = new RejectRequest();
        request.setReason("Schedule conflict");

        mockMvc.perform(post("/specialist/bookings/b-reject/reject")
                        .header("Authorization", SPECIALIST_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("b-reject"))
                .andExpect(jsonPath("$.data.status").value(BookingStatus.Rejected.name()));

        Booking booking = booking("b-reject");
        assertEquals(BookingStatus.Rejected, booking.getStatus());
        assertEquals("Schedule conflict", booking.getNote());
        assertTrue(slot(SLOT_ID).getAvailable());
    }

    private void insertSlot(String slotId, boolean available) {
        jdbcTemplate.update(
                "INSERT INTO slots (id, specialist_id, start_time, end_time, available, amount, currency, duration, type, detail, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                slotId,
                SPECIALIST_ID,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                available,
                new BigDecimal("100.00"),
                "CNY",
                60,
                "online",
                "test slot"
        );
    }

    private void insertPendingBooking(String bookingId, String slotId) {
        jdbcTemplate.update("UPDATE slots SET available = false WHERE id = ?", slotId);
        jdbcTemplate.update(
                "INSERT INTO bookings (id, customer_id, specialist_id, slot_id, note, status, created_at, updated_at) VALUES (?,?,?,?,?,?,NOW(),NOW())",
                bookingId,
                CUSTOMER_ID,
                SPECIALIST_ID,
                slotId,
                "original note",
                BookingStatus.Pending.name()
        );
    }

    private Booking booking(String bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow();
    }

    private Slot slot(String slotId) {
        return slotRepository.findById(slotId).orElseThrow();
    }

    private void stubPaidDraft(String paymentId, PaymentDraft draft) {
        when(paymentValueOps.get(OUT_TRADE_KEY + paymentId)).thenReturn(PAYMENT_INTENT_ID);
        when(paymentValueOps.get(PAYMENT_DRAFT_KEY + PAYMENT_INTENT_ID)).thenReturn(draft);
    }

    private PaymentDraft paidDraft(String customerId, String specialistId, String slotId, boolean paid) {
        PaymentDraft draft = new PaymentDraft(PAYMENT_ID, PAYMENT_ID, 100.00, "CNY", customerId, specialistId, slotId);
        draft.setPaid(paid);
        return draft;
    }

    private CreateBookingRequest createRequest(String specialistId, String slotId, String paymentId) {
        return new CreateBookingRequest(specialistId, slotId, paymentId, "test booking");
    }
}
