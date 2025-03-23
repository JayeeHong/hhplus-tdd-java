package io.hhplus.tdd.point;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);
    private final PointRepository pointRepository;

    public UserPoint getUserPoint(long id) {
        return pointRepository.findUserById(id);
    }

    public List<PointHistory> getHistory(long id) {
        return pointRepository.findHistoryById(id);
    }

    public UserPoint chargePoint(long id, long amount) {
        UserPoint userPoint = pointRepository.savePoint(id, amount);
        if (userPoint == null) {
            log.error("포인트 충전 실패: id={} amount={}", id, amount);
            return null;
        }

        pointRepository.savePointHistory(id, amount, TransactionType.CHARGE);

        return userPoint;
    }

    public UserPoint userPoint(long id, long amount) {
        UserPoint userPoint = pointRepository.savePoint(id, amount);
        if (userPoint == null) {
            log.error("포인트 사용 실패: id={} amount={}", id, amount);
            return null;
        }

        pointRepository.savePointHistory(id, amount, TransactionType.USE);

        return userPoint;
    }

}
