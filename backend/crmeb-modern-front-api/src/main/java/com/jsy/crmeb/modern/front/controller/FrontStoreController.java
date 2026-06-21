package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.front.FrontCommerceService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontStoreController {
    private final FrontCommerceService commerceService;

    public FrontStoreController(FrontCommerceService commerceService) {
        this.commerceService = commerceService;
    }

    @PostMapping("/api/front/store/list")
    public ApiResponse<Map<String, Object>> list(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(commerceService.storeList(body));
    }
}
