package com.jsy.crmeb.modern.service.upload.dto;

public record FileResultResponse(
        String fileName,
        String extName,
        Long fileSize,
        String url,
        String type) {
}
