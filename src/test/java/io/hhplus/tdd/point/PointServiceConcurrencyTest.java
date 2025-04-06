package io.hhplus.tdd.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.validate.PointValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PointServiceConcurrencyTest {

    private PointService pointService;
    private PointRepository pointRepository;
    private PointValidator pointValidator;
    private final long userId = 1L;

    @BeforeEach
    void setUp() {
        pointRepository = mock(PointRepository.class);
        pointValidator = mock(PointValidator.class);
        pointService = new PointService(pointRepository, pointValidator);
    }

    @Test
    void 포인트_적립_동시_요청() throws InterruptedException {
        // given
        long amount = 10L;
        int threadCount = 10; // 동시에 요청할 스레드 갯수

        doNothing().when(pointValidator).validatePointAmountBelowZero(anyLong());
        when(pointRepository.savePoint(userId, amount))
            .thenAnswer(invocation -> {
                long id = invocation.getArgument(0);
                long point = invocation.getArgument(1);
                return new UserPoint(id, point, System.currentTimeMillis());
            });

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    pointService.chargePoint(userId, amount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날때까지 대기

        // then
        assertEquals(10, successCount.get());
        assertEquals(0, failCount.get());
        verify(pointRepository, times(threadCount)).savePoint(eq(userId), anyLong());
        verify(pointRepository, times(threadCount)).savePointHistory(eq(userId), eq(amount), eq(TransactionType.CHARGE));
    }

    @Test
    void 포인트_사용_동시_요청() throws InterruptedException {
        // given
        long initialPoint = 100L;
        long useAmount = 10L;
        int threadCount = 10; // 동시에 요청할 스레드 갯수

        UserPoint userPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        when(pointRepository.findUserPointById(userId)).thenReturn(userPoint);

        doNothing().when(pointValidator).validatePointAmountBelowZero(anyLong());
        doAnswer(invocation -> {
            long beforePoint = invocation.getArgument(0);
            long usePoint = invocation.getArgument(1);
            if (beforePoint - usePoint < 0) {
                throw new RuntimeException("사용 후 포인트가 0 미만");
            }

            return null;
        }).when(pointValidator).validateTotalPointAmount(anyLong(), anyLong());

        when(pointRepository.savePoint(anyLong(), anyLong()))
            .thenAnswer(invocation -> {
                long id = invocation.getArgument(0);
                long updatedPoint = invocation.getArgument(1);
                return new UserPoint(id, updatedPoint, System.currentTimeMillis());
            });

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날때까지 대기

        // then
        assertEquals(10, successCount.get());
        assertEquals(0, failCount.get());
        verify(pointRepository, times(threadCount)).savePoint(eq(userId), anyLong());
        verify(pointRepository, times(threadCount)).savePointHistory(eq(userId), eq(useAmount), eq(TransactionType.USE));
    }

}
