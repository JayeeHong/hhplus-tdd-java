package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.service.PointService;
import io.hhplus.tdd.point.dto.UserPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    @GetMapping("{id}")
    public UserPoint point(@PathVariable("id") long id) {
        log.info("포인트 조회 요청: id={}", id);
        return pointService.getUserPoint(id);
    }

    @GetMapping("{id}/histories")
    public List<PointHistory> history(@PathVariable long id) {
        log.info("포인트 충전/이용 내역 조회: id={}", id);
        return pointService.getHistory(id);
    }

    @PatchMapping("{id}/charge")
    public UserPoint charge(@PathVariable("id") long id, @RequestBody long amount) {
        log.info("포인트 충전: id={} amount={}", id, amount);
        return pointService.chargePoint(id, amount);
    }

    @PatchMapping("{id}/use")
    public UserPoint use(@PathVariable("id") long id, @RequestBody long amount) {
        log.info("포인트 사용: id={} amount={}", id, amount);
        return pointService.usePoint(id, amount);
    }
}
