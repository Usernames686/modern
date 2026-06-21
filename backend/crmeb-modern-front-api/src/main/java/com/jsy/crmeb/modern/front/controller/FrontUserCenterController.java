package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthService;
import com.jsy.crmeb.modern.service.front.FrontUserCenterService;
import com.jsy.crmeb.modern.service.front.dto.FrontRechargeRequest;
import com.jsy.crmeb.modern.service.front.dto.FrontUserExtractRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontUserCenterController {
    private final FrontAuthService frontAuthService;
    private final FrontUserCenterService userCenterService;

    public FrontUserCenterController(FrontAuthService frontAuthService, FrontUserCenterService userCenterService) {
        this.frontAuthService = frontAuthService;
        this.userCenterService = userCenterService;
    }

    @GetMapping("/api/front/user/balance")
    public ApiResponse<Map<String, Object>> balance(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.balance(uid(request)));
    }

    @GetMapping("/api/front/recharge/bill/record")
    public ApiResponse<PageResponse<Map<String, Object>>> billRecord(
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "12") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.billRecord(uid(request), type, page, limit));
    }

    @GetMapping("/api/front/recharge/index")
    public ApiResponse<Map<String, Object>> rechargeConfig(HttpServletRequest request) {
        uid(request);
        return ApiResponse.ok(userCenterService.rechargeConfig());
    }

    @PostMapping("/api/front/recharge/routine")
    public ApiResponse<Map<String, Object>> routineRecharge(
            @RequestBody FrontRechargeRequest rechargeRequest,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.rechargeDryRun(uid(request), rechargeRequest, "routine"));
    }

    @PostMapping("/api/front/recharge/wechat")
    public ApiResponse<Map<String, Object>> wechatRecharge(
            @RequestBody FrontRechargeRequest rechargeRequest,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.rechargeDryRun(uid(request), rechargeRequest, "public"));
    }

    @PostMapping("/api/front/recharge/wechat/app")
    public ApiResponse<Map<String, Object>> wechatAppRecharge(
            @RequestBody FrontRechargeRequest rechargeRequest,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.rechargeDryRun(uid(request), rechargeRequest, "weixinApp"));
    }

    @GetMapping("/api/front/integral/user")
    public ApiResponse<Map<String, Object>> integralUser(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.integralUser(uid(request)));
    }

    @GetMapping("/api/front/integral/list")
    public ApiResponse<PageResponse<Map<String, Object>>> integralList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.integralList(uid(request), page, limit));
    }

    @GetMapping("/api/front/menu/user")
    public ApiResponse<Map<String, Object>> menuUser() {
        return ApiResponse.ok(userCenterService.menuUser());
    }

    @GetMapping("/api/front/user/sign/config")
    public ApiResponse<Object> signConfig() {
        return ApiResponse.ok(userCenterService.signConfig());
    }

    @GetMapping("/api/front/user/sign/list")
    public ApiResponse<PageResponse<Map<String, Object>>> signList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.signList(uid(request), page, limit));
    }

    @GetMapping("/api/front/user/sign/month")
    public ApiResponse<PageResponse<Map<String, Object>>> signMonthList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "8") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.signMonthList(uid(request), page, limit));
    }

    @GetMapping("/api/front/user/sign/get")
    public ApiResponse<Map<String, Object>> signToday(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.signToday(uid(request)));
    }

    @PostMapping("/api/front/user/sign/user")
    public ApiResponse<Map<String, Object>> signUser(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.signInfo(uid(request)));
    }

    @GetMapping("/api/front/user/sign/integral")
    public ApiResponse<Map<String, Object>> sign(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.sign(uid(request)));
    }

    @GetMapping("/api/front/user/level/grade")
    public ApiResponse<Object> userLevelGrade() {
        return ApiResponse.ok(userCenterService.userLevelGrade());
    }

    @GetMapping("/api/front/user/expList")
    public ApiResponse<PageResponse<Map<String, Object>>> experienceList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.experienceList(uid(request), page, limit));
    }

    @GetMapping("/api/front/commission")
    public ApiResponse<Map<String, Object>> commission(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.commission(uid(request)));
    }

    @GetMapping("/api/front/spread/count/{type}")
    public ApiResponse<Map<String, BigDecimal>> spreadCount(
            @PathVariable("type") int type,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.spreadCount(uid(request), type));
    }

    @GetMapping("/api/front/spread/commission/detail")
    public ApiResponse<PageResponse<Map<String, Object>>> brokerageRecords(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.brokerageRecords(uid(request), page, limit));
    }

    @GetMapping("/api/front/extract/record")
    public ApiResponse<PageResponse<Map<String, Object>>> extractRecords(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.extractRecords(uid(request), page, limit));
    }

    @GetMapping("/api/front/extract/user")
    public ApiResponse<Map<String, Object>> extractUser(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.extractUser(uid(request)));
    }

    @GetMapping("/api/front/extract/bank")
    public ApiResponse<List<String>> extractBank(HttpServletRequest request) {
        uid(request);
        return ApiResponse.ok(userCenterService.extractBank());
    }

    @PostMapping("/api/front/extract/cash")
    public ApiResponse<Boolean> extractCash(@RequestBody FrontUserExtractRequest extractRequest, HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.extractCash(uid(request), extractRequest));
    }

    @GetMapping("/api/front/spread/order")
    public ApiResponse<Map<String, Object>> spreadOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.spreadOrders(uid(request), page, limit));
    }

    @GetMapping("/api/front/rank")
    public ApiResponse<Object> spreadRank(
            @RequestParam(value = "type", defaultValue = "week") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        uid(request);
        return ApiResponse.ok(userCenterService.spreadRank(type, page, limit));
    }

    @GetMapping("/api/front/brokerage_rank")
    public ApiResponse<Object> brokerageRank(
            @RequestParam(value = "type", defaultValue = "week") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        uid(request);
        return ApiResponse.ok(userCenterService.brokerageRank(type, page, limit));
    }

    @GetMapping("/api/front/user/brokerageRankNumber")
    public ApiResponse<Integer> brokerageRankNumber(
            @RequestParam(value = "type", defaultValue = "week") String type,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.brokerageRankNumber(uid(request), type));
    }

    @GetMapping("/api/front/user/spread/banner")
    public ApiResponse<Object> spreadBanner(HttpServletRequest request) {
        uid(request);
        return ApiResponse.ok(userCenterService.spreadBanner());
    }

    @PostMapping("/api/front/user/set_visit")
    public ApiResponse<Boolean> setVisit(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        String token = readToken(request);
        Integer currentUid = frontAuthService.tokenIsExist(token) ? frontAuthService.requireUidByToken(token) : 0;
        Integer visitType = intValue(body == null ? null : body.get("visitType"));
        if (visitType == null) {
            visitType = intValue(body == null ? null : body.get("visit_type"));
        }
        return ApiResponse.ok(userCenterService.recordVisit(currentUid, visitType));
    }

    @GetMapping("/api/front/user/bindSpread")
    public ApiResponse<Boolean> bindSpread(
            @RequestParam(value = "spreadPid", required = false) Integer spreadPid,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.bindSpread(uid(request), spreadPid));
    }

    @GetMapping("/api/front/spread/people/count")
    public ApiResponse<Map<String, Object>> spreadPeopleCount(HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.spreadPeopleCount(uid(request)));
    }

    @GetMapping("/api/front/spread/people")
    public ApiResponse<PageResponse<Map<String, Object>>> spreadPeople(
            @RequestParam(value = "grade", defaultValue = "0") int grade,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortKey", required = false) String sortKey,
            @RequestParam(value = "isAsc", defaultValue = "DESC") String isAsc,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(userCenterService.spreadPeople(uid(request), grade, keyword, sortKey, isAsc, page, limit));
    }

    private Integer uid(HttpServletRequest request) {
        return frontAuthService.requireUidByToken(readToken(request));
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader(FrontAuthService.TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        return token;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
