package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointRepository {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 유저의 포인트 조회
    public UserPoint findUserById(long id) {
        return userPointTable.selectById(id);
    }

    // 유저의 포인트 히스토리 조회
    public List<PointHistory> findHistoryById(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    // 유저의 포인트 저장 또는 업데이트
    public UserPoint savePoint(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }

    // 유저의 포인트 히스토리 저장
    public PointHistory savePointHistory(long id, long amount, TransactionType type) {
        return pointHistoryTable.insert(id, amount, type, System.currentTimeMillis());
    }

}
