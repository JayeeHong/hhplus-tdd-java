package io.hhplus.tdd.point;

import io.hhplus.tdd.point.dto.TransactionType;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
}
