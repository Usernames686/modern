package com.jsy.crmeb.modern.service.upload;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.upload.dto.AttachmentMoveRequest;
import com.jsy.crmeb.modern.service.upload.dto.AttachmentRequest;
import com.jsy.crmeb.modern.service.upload.entity.SystemAttachment;
import com.jsy.crmeb.modern.service.upload.mapper.SystemAttachmentMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemAttachmentService {
    private static final String DEFAULT_IMAGE_TYPES = "png,jpeg,jpg,audio/mpeg,text/plain,video/mp4,gif";

    private final SystemAttachmentMapper systemAttachmentMapper;

    public SystemAttachmentService(SystemAttachmentMapper systemAttachmentMapper) {
        this.systemAttachmentMapper = systemAttachmentMapper;
    }

    public PageResponse<SystemAttachment> list(Integer pid, String attType, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        QueryWrapper<SystemAttachment> query = new QueryWrapper<>();
        query.eq("pid", pid == null ? 0 : pid);
        List<String> types = splitCsv(StringUtils.hasText(attType) ? attType : DEFAULT_IMAGE_TYPES);
        if (!types.isEmpty()) {
            query.in("att_type", types);
        }
        query.orderByDesc("att_id");
        Page<SystemAttachment> result = systemAttachmentMapper.selectPage(Page.of(safePage, safeLimit), query);
        return new PageResponse<>(safePage, safeLimit, result.getTotal(), result.getRecords());
    }

    public SystemAttachment info(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id 参数不合法");
        }
        return systemAttachmentMapper.selectById(id);
    }

    @Transactional
    public void save(AttachmentRequest request) {
        SystemAttachment attachment = toEntity(request);
        attachment.setAttId(null);
        attachment.setImageType(attachment.getImageType() == null ? 1 : attachment.getImageType());
        attachment.setPid(attachment.getPid() == null ? 0 : attachment.getPid());
        systemAttachmentMapper.insert(attachment);
    }

    @Transactional
    public void update(Integer id, AttachmentRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id 参数不合法");
        }
        SystemAttachment attachment = toEntity(request);
        attachment.setAttId(id);
        attachment.setUpdateTime(LocalDateTime.now());
        systemAttachmentMapper.updateById(attachment);
    }

    @Transactional
    public void delete(String ids) {
        List<Integer> idList = splitIds(ids);
        if (idList.isEmpty()) {
            throw new IllegalArgumentException("ids 参数不合法");
        }
        systemAttachmentMapper.deleteBatchIds(idList);
    }

    @Transactional
    public void move(AttachmentMoveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        List<Integer> idList = splitIds(request.getAttrId());
        if (idList.isEmpty()) {
            throw new IllegalArgumentException("请选择要移动的附件");
        }
        Integer pid = request.getPid() == null ? 0 : request.getPid();
        UpdateWrapper<SystemAttachment> update = new UpdateWrapper<>();
        update.in("att_id", idList)
                .set("pid", pid)
                .set("update_time", LocalDateTime.now());
        systemAttachmentMapper.update(null, update);
    }

    private SystemAttachment toEntity(AttachmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        SystemAttachment attachment = new SystemAttachment();
        attachment.setAttId(request.getAttId());
        attachment.setName(request.getName());
        attachment.setAttDir(request.getAttDir() == null ? "" : request.getAttDir());
        attachment.setSattDir(request.getSattDir());
        attachment.setAttSize(request.getAttSize());
        attachment.setAttType(request.getAttType());
        attachment.setPid(request.getPid());
        attachment.setImageType(request.getImageType());
        return attachment;
    }

    private List<Integer> splitIds(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Integer::valueOf)
                .toList();
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
