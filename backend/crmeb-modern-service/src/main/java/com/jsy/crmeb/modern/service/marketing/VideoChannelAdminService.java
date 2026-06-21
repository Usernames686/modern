package com.jsy.crmeb.modern.service.marketing;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.mapper.VideoChannelAdminMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VideoChannelAdminService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final VideoChannelAdminMapper videoChannelMapper;

    public VideoChannelAdminService(VideoChannelAdminMapper videoChannelMapper) {
        this.videoChannelMapper = videoChannelMapper;
    }

    public PageResponse<Map<String, Object>> productList(Integer page, Integer limit, String keywords) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        String safeKeywords = safeKeywords(keywords);
        long total = videoChannelMapper.countVideoProducts(safeKeywords);
        List<Map<String, Object>> rows = videoChannelMapper
                .selectVideoProducts(safeKeywords, safeLimit, offset(safePage, safeLimit))
                .stream()
                .map(row -> normalize(row, "视频号商品"))
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    public PageResponse<Map<String, Object>> draftList(Integer page, Integer limit, String status, String keywords) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        String safeStatus = safeStatus(status);
        String safeKeywords = safeKeywords(keywords);
        long total = videoChannelMapper.countDraftProducts(safeStatus, safeKeywords);
        List<Map<String, Object>> rows = videoChannelMapper
                .selectDraftProducts(safeStatus, safeKeywords, safeLimit, offset(safePage, safeLimit))
                .stream()
                .map(row -> normalize(row, "本地草稿候选"))
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    private Map<String, Object> normalize(Map<String, Object> source, String sourceText) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.put("image", normalizeAsset(value(row.get("image"))));
        row.put("videoLink", normalizeAsset(value(row.get("videoLink"))));
        row.put("isShow", truthy(row.get("isShow")));
        row.put("isDel", truthy(row.get("isDel")));
        row.put("isRecycle", truthy(row.get("isRecycle")));
        row.put("addTimeText", addTimeText(row.get("addTime")));
        row.put("sourceText", sourceText);
        row.put("statusText", Boolean.TRUE.equals(row.get("isShow")) ? "已上架" : "未上架");
        row.put("externalActionText", "微信视频号同步/发布接口未配置");
        return row;
    }

    private int safePage(Integer page) {
        return page == null || page <= 0 ? 1 : page;
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 200);
    }

    private int offset(int page, int limit) {
        return (page - 1) * limit;
    }

    private String safeKeywords(String keywords) {
        return StringUtils.hasText(keywords) ? keywords.trim() : null;
    }

    private String safeStatus(String status) {
        if ("unshelved".equals(status) || "withVideo".equals(status)) {
            return status;
        }
        return "candidate";
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String addTimeText(Object value) {
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(number.longValue()), ZONE).format(DATE_TIME);
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
