package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointRepository;
import io.hhplus.tdd.point.dto.TransactionType;
import io.hhplus.tdd.point.dto.UserPoint;
import io.hhplus.tdd.point.validate.PointValidator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);
    private final PointRepository pointRepository;
    private final PointValidator pointValidator;

    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public UserPoint getUserPoint(long id) {

        if (id < 1L) {
            throw new IllegalArgumentException("유효하지 않은 id 값");
        }

        UserPoint userPoint = pointRepository.findUserPointById(id);
        boolean validUserPoint = pointValidator.validateUserPoint(userPoint);
        if (!validUserPoint) {
            log.error("포인트 조회 결과 없음: id={}", id);
            return UserPoint.empty(0L);
        }
        
        return userPoint;
    }

    public List<PointHistory> getHistory(long id) {
        return pointRepository.findPointHistoryById(id);
    }

    public UserPoint chargePoint(long id, long amount) {

        lockMap.putIfAbsent(id, new ReentrantLock());
        ReentrantLock lock = lockMap.get(id);

        lock.lock();

        try {
            // 적립할 포인트 0 이하인지 체크
            pointValidator.validatePointAmountBelowZero(amount);

            UserPoint userPoint = pointRepository.savePoint(id, amount);
            pointRepository.savePointHistory(id, amount, TransactionType.CHARGE);

            return userPoint;
        } finally {
            lock.unlock();
            lockMap.remove(id);
        }

    }

    public UserPoint usePoint(long id, long amount) {

        lockMap.putIfAbsent(id, new ReentrantLock());
        ReentrantLock lock = lockMap.get(id);

        lock.lock();

        try {
            // 사용할 포인트 0 이하인지 체크
            pointValidator.validatePointAmountBelowZero(amount);

            // 포인트를 썼을 때 0 이상인지 체크
            UserPoint findUserPoint = pointRepository.findUserPointById(id);
            pointValidator.validateTotalPointAmount(findUserPoint.point(), amount);

            UserPoint userPoint = pointRepository.savePoint(id, findUserPoint.point() - amount);
            pointRepository.savePointHistory(id, amount, TransactionType.USE);

            return userPoint;
        } finally {
            lock.unlock();
            lockMap.remove(id); // 사용이 끝난 락 삭제
        }
    }
}
