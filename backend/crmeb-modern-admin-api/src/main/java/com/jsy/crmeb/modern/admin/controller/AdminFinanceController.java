package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.finance.FinanceAdminService;
import com.jsy.crmeb.modern.service.finance.dto.BrokerageRecordRequest;
import com.jsy.crmeb.modern.service.finance.dto.BrokerageRecordResponse;
import com.jsy.crmeb.modern.service.finance.dto.ExtractBalanceResponse;
import com.jsy.crmeb.modern.service.finance.dto.FundsMonitorRequest;
import com.jsy.crmeb.modern.service.finance.dto.MonitorResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractSearchRequest;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractUpdateRequest;
import com.jsy.crmeb.modern.service.finance.dto.UserRechargeResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserRechargeSearchRequest;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminFinanceController {
    private final FinanceAdminService financeAdminService;

    public AdminFinanceController(FinanceAdminService financeAdminService) {
        this.financeAdminService = financeAdminService;
    }

    @GetMapping("/api/admin/user/topUpLog/list")
    public ApiResponse<PageResponse<UserRechargeResponse>> rechargeList(@ModelAttribute UserRechargeSearchRequest request) {
        return ApiResponse.ok(financeAdminService.rechargeList(request));
    }

    @PostMapping("/api/admin/user/topUpLog/balance")
    public ApiResponse<Map<String, BigDecimal>> rechargeBalance() {
        return ApiResponse.ok(financeAdminService.rechargeBalance());
    }

    @GetMapping("/api/admin/finance/founds/monitor/list")
    public ApiResponse<PageResponse<MonitorResponse>> fundMonitorList(@ModelAttribute FundsMonitorRequest request) {
        return ApiResponse.ok(financeAdminService.fundMonitorList(request));
    }

    @GetMapping("/api/admin/finance/founds/monitor/brokerage/record")
    public ApiResponse<PageResponse<BrokerageRecordResponse>> brokerageRecord(@ModelAttribute BrokerageRecordRequest request) {
        return ApiResponse.ok(financeAdminService.brokerageRecords(request));
    }

    @GetMapping("/api/admin/finance/apply/list")
    public ApiResponse<PageResponse<UserExtractResponse>> extractList(@ModelAttribute UserExtractSearchRequest request) {
        return ApiResponse.ok(financeAdminService.extractList(request));
    }

    @PostMapping("/api/admin/finance/apply/balance")
    public ApiResponse<ExtractBalanceResponse> extractBalance(@RequestParam(value = "dateLimit", required = false) String dateLimit) {
        return ApiResponse.ok(financeAdminService.extractBalance(dateLimit));
    }

    @PostMapping("/api/admin/finance/apply/update")
    public ApiResponse<Void> updateExtract(@ModelAttribute UserExtractUpdateRequest request) {
        financeAdminService.updateExtract(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/finance/apply/apply")
    public ApiResponse<Void> applyExtract(
            @RequestParam("id") Integer id,
            @RequestParam("status") Integer status,
            @RequestParam(value = "backMessage", required = false) String backMessage) {
        financeAdminService.applyExtract(id, status, backMessage);
        return ApiResponse.ok(null);
    }
}
