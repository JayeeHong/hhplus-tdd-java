package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.validate.PointValidator;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired private MockMvc mockMvc;

    @Mock private PointValidator pointValidator;
    @MockBean private PointService pointService;

    private UserPoint userPoint;
    private PointHistory pointHistory;

    @BeforeEach
    void setUp() {
        userPoint = new UserPoint(1L, 100L, System.currentTimeMillis());
        pointHistory = new PointHistory(1L, 1L, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
    }

    @Test
    void 포인트_조회시_성공() throws Exception {
        // given
        given(pointService.getUserPoint(1L)).willReturn(userPoint);

        // when, then
        mockMvc.perform(get("/api/v1/point/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.point").value(100L));
    }

    @Test
    void 포인트_히스토리_조회시_성공() throws Exception {
        // given
        given(pointService.getHistory(1L)).willReturn(Arrays.asList(pointHistory));

        // when, then
        mockMvc.perform(get("/api/v1/point/1/histories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].amount").value(1000L))
            .andExpect(jsonPath("$[0].type").value("CHARGE"));

        verify(pointService, times(1)).getHistory(1L);
    }

    @Test
    void 포인트_충전시_성공() throws Exception {
        // given
        given(pointService.chargePoint(1L, 500L)).willReturn(new UserPoint(1L, 1500L, System.currentTimeMillis()));

        // when, then
        mockMvc.perform(patch("/api/v1/point/1/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 500}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.point").value(1500L));
    }

    @Test
    void 포인트_사용시_성공() throws Exception {
        // Mocking 서비스 레이어
        given(pointService.usePoint(1L, 300L)).willReturn(new UserPoint(1L, 700L, System.currentTimeMillis()));

        mockMvc.perform(patch("/api/v1/point/1/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 300}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.point").value(700L));
    }
}