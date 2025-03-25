package io.hhplus.tdd.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointRepository;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.validate.PointValidator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointValidator pointValidator;

    @InjectMocks
    private PointService pointService;

    private final long userId = 1L;

    @BeforeEach
    void setUp() {
        reset(pointRepository, pointValidator);
    }

    @Test
    void 사용자의_포인트를_정상_조회한다() {
        // given
        UserPoint userPoint = new UserPoint(userId, 1L, System.currentTimeMillis());
        when(pointRepository.findUserPointById(userPoint.id())).thenReturn(userPoint);
        when(pointValidator.validateUserPoint(userPoint)).thenReturn(true);

        // when
        UserPoint result = pointService.getUserPoint(userPoint.id());

        // then
        assertThat(result).isEqualTo(userPoint);
        verify(pointRepository).findUserPointById(userPoint.id());
        verify(pointValidator).validateUserPoint(userPoint);
    }

    // 유저의 포인트 이력 조회
    @Test
    void 사용자의_포인트_히스토리를_정상_조회한다() {
        // given
        List<PointHistory> pointHistories = new ArrayList<>();
        pointHistories.add(
            new PointHistory(1L, userId, 1L, TransactionType.CHARGE, System.currentTimeMillis()));
        pointHistories.add(
            new PointHistory(2L, userId, 1L, TransactionType.USE, System.currentTimeMillis()));
        when(pointRepository.findPointHistoryById(userId)).thenReturn(pointHistories);

        // when
        List<PointHistory> findHistories = pointService.getHistory(userId);

        // then
        assertThat(findHistories).isEqualTo(pointHistories);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1000})
    void 적립할_포인트가_0이하여서_포인트_적립시_포인트_충전_실패한다(long amount) {
        // given
        doThrow(new RuntimeException("적립할 포인트는 0보다 커야 함"))
            .when(pointValidator).validatePointAmountBelowZero(amount);

        // when, then
        assertThrows(RuntimeException.class, () -> pointService.chargePoint(userId, amount));
        // validatePointAmountBelowZero 호출 했음을 검증
        verify(pointValidator).validatePointAmountBelowZero(amount);
        // pointRepository.savePoint 호출하지 않았음을 검증
        verify(pointRepository, never()).savePoint(anyLong(), anyLong());
        // pointRepository.savePointHistory 호출하지 않았음을 검증
        verify(pointRepository, never()).savePointHistory(anyLong(), anyLong(), any());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 1000})
    void 포인트_충전_성공한다(long amount) {
        // given
        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        doNothing().when(pointValidator).validatePointAmountBelowZero(amount);
        when(pointRepository.savePoint(userId, amount)).thenReturn(userPoint);

        // when
        UserPoint chargeUserPoint = pointService.chargePoint(userId, amount);

        // then
        assertThat(chargeUserPoint).isEqualTo(userPoint);
        verify(pointValidator).validatePointAmountBelowZero(amount);
        verify(pointRepository).savePoint(userId, amount);
        verify(pointRepository).savePointHistory(userId, amount, TransactionType.CHARGE);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1000})
    void 사용할_포인트가_0이하여서_포인트_사용_실패한다(long amount) {
        // given
        doThrow(new RuntimeException("사용할 포인트는 0보다 커야 함"))
            .when(pointValidator).validatePointAmountBelowZero(amount);

        // when, then
        assertThrows(RuntimeException.class, () -> pointService.usePoint(userId, amount));
        // validatePointAmountBelowZero 호출 했음을 검증
        verify(pointValidator).validatePointAmountBelowZero(amount);
        // validateTotalPointAmount 호출 했음을 검증
        verify(pointValidator, never()).validateTotalPointAmount(anyLong(), anyLong());
        // pointRepository.savePoint 호출하지 않았음을 검증
        verify(pointRepository, never()).savePoint(anyLong(), anyLong());
        // pointRepository.savePointHistory 호출하지 않았음을 검증
        verify(pointRepository, never()).savePointHistory(anyLong(), anyLong(), any());
    }

    @ParameterizedTest
    @CsvSource(value = {"10,11", "1,1000"}, delimiter = ',')
    void 포인트_사용_후_포인트가_0미만이어서_포인트_사용_실패한다(long beforePoint, long usePoint) {
        // given
        UserPoint userPoint = new UserPoint(userId, beforePoint, System.currentTimeMillis());
        doNothing().when(pointValidator).validatePointAmountBelowZero(usePoint);
        when(pointRepository.findUserPointById(userId)).thenReturn(userPoint);
        doThrow(new RuntimeException("사용 후 포인트가 0 미만"))
            .when(pointValidator).validateTotalPointAmount(beforePoint, usePoint);

        // when, then
        assertThrows(RuntimeException.class, () -> pointService.usePoint(userId, usePoint));

        // validatePointAmountBelowZero 호출 했음을 검증
        verify(pointValidator).validatePointAmountBelowZero(usePoint);
        // validateTotalPointAmount 호출 했음을 검증
        verify(pointValidator).validateTotalPointAmount(beforePoint, usePoint);
        // pointRepository.savePoint 호출하지 않았음을 검증
        verify(pointRepository, never()).savePoint(anyLong(), anyLong());
        // pointRepository.savePointHistory 호출하지 않았음을 검증
        verify(pointRepository, never()).savePointHistory(anyLong(), anyLong(), any());
    }

    @ParameterizedTest
    @CsvSource(value = {"11,10", "1000,1"}, delimiter = ',')
    void 포인트_사용_성공(long beforePoint, long usePoint) {
        // given
        UserPoint userPoint = new UserPoint(userId, beforePoint, System.currentTimeMillis());
        doNothing().when(pointValidator).validatePointAmountBelowZero(usePoint);

        when(pointRepository.findUserPointById(userId)).thenReturn(userPoint);
        doNothing().when(pointValidator).validateTotalPointAmount(beforePoint, usePoint);

        UserPoint afterUserPoint = new UserPoint(userId, beforePoint - usePoint, System.currentTimeMillis());
        when(pointRepository.savePoint(userId, beforePoint - usePoint)).thenReturn(afterUserPoint);

        // when
        UserPoint useUserPoint = pointService.usePoint(userId, usePoint);

        // then
        assertThat(useUserPoint).isEqualTo(afterUserPoint);
        verify(pointValidator).validatePointAmountBelowZero(usePoint);
        verify(pointRepository).savePoint(userId, beforePoint - usePoint);
        verify(pointRepository).savePointHistory(userId, usePoint, TransactionType.USE);
    }

}