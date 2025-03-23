package io.hhplus.tdd.point.validate;

import io.hhplus.tdd.point.dto.UserPoint;

public class PointValidator {

    public boolean validateUserPoint(UserPoint userPoint) {
        if (userPoint == null) {
            return false;
        }

        return true;
    }

    public void validatePointAmountBelowZero(long amount) {
        if (amount <= 0) {
            throw new RuntimeException("포인트가 0 이하");
        }
    }

    public void validateTotalPointAmount(long beforePoint, long usePoint) {
        if (beforePoint - usePoint < 0) {
            throw new RuntimeException("사용 후 포인트가 0 미만");
        }
    }

}
