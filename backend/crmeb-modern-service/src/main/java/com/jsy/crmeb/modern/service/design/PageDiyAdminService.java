package com.jsy.crmeb.modern.service.design;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.design.dto.PageDiyResponse;
import com.jsy.crmeb.modern.service.design.dto.PageDiySaveRequest;
import com.jsy.crmeb.modern.service.design.mapper.PageDiyAdminMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PageDiyAdminService {
    private final PageDiyAdminMapper pageDiyMapper;

    public PageDiyAdminService(PageDiyAdminMapper pageDiyMapper) {
        this.pageDiyMapper = pageDiyMapper;
    }

    public PageResponse<PageDiyResponse> list(String name, Integer page, Integer limit) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        String keyword = decode(trimToNull(name));
        long total = pageDiyMapper.countList(keyword);
        List<PageDiyResponse> list = total <= 0 ? List.of() : pageDiyMapper.selectList(keyword, (safePage - 1) * safeLimit, safeLimit);
        list.forEach(this::normalizeAssets);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public PageDiyResponse info(Integer id) {
        PageDiyResponse response = id == null || id == 0 ? pageDiyMapper.selectDefault() : pageDiyMapper.selectInfo(id);
        if (response == null || Integer.valueOf(1).equals(response.getIsDel())) {
            throw new IllegalArgumentException("未找到对应模板信息");
        }
        normalizeAssets(response);
        return response;
    }

    public Integer defaultId() {
        PageDiyResponse response = pageDiyMapper.selectDefault();
        if (response == null) {
            throw new IllegalArgumentException("首页模板设置不正确");
        }
        return response.getId();
    }

    @Transactional
    public void setDefault(Integer id) {
        PageDiyResponse response = pageDiyMapper.selectInfo(id);
        if (response == null || Integer.valueOf(1).equals(response.getIsDel()) || !Integer.valueOf(0).equals(response.getMerId())) {
            throw new IllegalArgumentException("当前DIY模板不存在");
        }
        pageDiyMapper.clearDefault();
        if (pageDiyMapper.setDefault(id) != 1) {
            throw new IllegalArgumentException("设置首页失败");
        }
    }

    public void delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("请选择模板");
        }
        if (pageDiyMapper.deleteById(id) != 1) {
            throw new IllegalArgumentException("默认首页不能删除");
        }
    }

    @Transactional
    public PageDiyResponse copy(Integer id) {
        PageDiyResponse source = info(id);
        PageDiyResponse target = cloneForInsert(source);
        target.setName(nextCopyName(source.getName()));
        if (pageDiyMapper.insertPage(target) != 1) {
            throw new IllegalArgumentException("复制模板失败");
        }
        return info(target.getId());
    }

    @Transactional
    public PageDiyResponse saveBase(PageDiySaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请填写模板信息");
        }
        validateName(request.getName(), request.getId());
        applyDefaults(request);
        if (request.getId() == null) {
            PageDiyResponse page = new PageDiyResponse();
            page.setVersion("");
            page.setName(request.getName().trim());
            page.setTitle(trimOrDefault(request.getTitle(), "首页"));
            page.setCoverImage(clearPrefix(request.getCoverImage()));
            page.setTemplateName(trimOrDefault(request.getTemplateName(), ""));
            page.setValue(StringUtils.hasText(request.getValue()) ? request.getValue() : "{}");
            page.setStatus(request.getStatus());
            page.setType(request.getType());
            page.setIsShow(request.getIsShow());
            page.setIsBgColor(request.getIsBgColor());
            page.setIsBgPic(request.getIsBgPic());
            page.setIsDiy(request.getIsDiy());
            page.setColorPicker(trimOrDefault(request.getColorPicker(), ""));
            page.setBgPic(clearPrefix(request.getBgPic()));
            page.setBgTabVal(request.getBgTabVal());
            page.setReturnAddress(trimOrDefault(request.getReturnAddress(), "0"));
            page.setTitleBgColor(trimOrDefault(request.getTitleBgColor(), "1"));
            page.setTitleColor(trimOrDefault(request.getTitleColor(), "1"));
            page.setServiceStatus(request.getServiceStatus());
            page.setTextPosition(request.getTextPosition());
            pageDiyMapper.insertPage(page);
            return info(page.getId());
        }
        request.setName(request.getName().trim());
        request.setCoverImage(clearPrefix(request.getCoverImage()));
        request.setBgPic(clearPrefix(request.getBgPic()));
        if (pageDiyMapper.updateBase(request) != 1) {
            throw new IllegalArgumentException("未找到对应模板信息");
        }
        return info(request.getId());
    }

    private void validateName(String name, Integer id) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("请输入模板名称");
        }
        if (name.trim().length() > 50) {
            throw new IllegalArgumentException("模板名称不能超过50个字");
        }
        if (pageDiyMapper.countSameName(name.trim(), id) > 0) {
            throw new IllegalArgumentException("当前模板名称已经存在，请修改后再保存");
        }
    }

    private void applyDefaults(PageDiySaveRequest request) {
        request.setTitle(trimOrDefault(request.getTitle(), "首页"));
        request.setTemplateName(trimOrDefault(request.getTemplateName(), ""));
        request.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        request.setType(request.getType() == null ? 0 : request.getType());
        request.setIsShow(request.getIsShow() == null ? 1 : request.getIsShow());
        request.setIsBgColor(request.getIsBgColor() == null ? 0 : request.getIsBgColor());
        request.setIsBgPic(request.getIsBgPic() == null ? 0 : request.getIsBgPic());
        request.setIsDiy(request.getIsDiy() == null ? 0 : request.getIsDiy());
        request.setBgTabVal(request.getBgTabVal() == null ? 0 : request.getBgTabVal());
        request.setValue(StringUtils.hasText(request.getValue()) ? request.getValue() : "{}");
        request.setReturnAddress(trimOrDefault(request.getReturnAddress(), "0"));
        request.setTitleBgColor(trimOrDefault(request.getTitleBgColor(), "1"));
        request.setTitleColor(trimOrDefault(request.getTitleColor(), "1"));
        request.setServiceStatus(request.getServiceStatus() == null ? 1 : request.getServiceStatus());
        request.setTextPosition(request.getTextPosition() == null ? 0 : request.getTextPosition());
    }

    private PageDiyResponse cloneForInsert(PageDiyResponse source) {
        PageDiyResponse target = new PageDiyResponse();
        target.setVersion(source.getVersion() == null ? "" : source.getVersion());
        target.setTitle(source.getTitle() == null ? "首页" : source.getTitle());
        target.setCoverImage(clearPrefix(source.getCoverImage()));
        target.setTemplateName(source.getTemplateName() == null ? "" : source.getTemplateName());
        target.setValue(source.getValue() == null ? "{}" : source.getValue());
        target.setStatus(source.getStatus() == null ? 0 : source.getStatus());
        target.setType(source.getType() == null ? 0 : source.getType());
        target.setIsShow(source.getIsShow() == null ? 1 : source.getIsShow());
        target.setIsBgColor(source.getIsBgColor() == null ? 0 : source.getIsBgColor());
        target.setIsBgPic(source.getIsBgPic() == null ? 0 : source.getIsBgPic());
        target.setIsDiy(source.getIsDiy() == null ? 0 : source.getIsDiy());
        target.setColorPicker(source.getColorPicker() == null ? "" : source.getColorPicker());
        target.setBgPic(clearPrefix(source.getBgPic()));
        target.setBgTabVal(source.getBgTabVal() == null ? 0 : source.getBgTabVal());
        target.setReturnAddress(source.getReturnAddress() == null ? "0" : source.getReturnAddress());
        target.setTitleBgColor(source.getTitleBgColor() == null ? "1" : source.getTitleBgColor());
        target.setTitleColor(source.getTitleColor() == null ? "1" : source.getTitleColor());
        target.setServiceStatus(source.getServiceStatus() == null ? 1 : source.getServiceStatus());
        target.setTextPosition(source.getTextPosition() == null ? 0 : source.getTextPosition());
        return target;
    }

    private String nextCopyName(String sourceName) {
        String base = StringUtils.hasText(sourceName) ? sourceName.trim() : "微页面";
        for (int index = 1; index < 1000; index++) {
            String candidate = index == 1 ? base + "-副本" : base + "-副本" + index;
            if (pageDiyMapper.countSameName(candidate, null) == 0) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("复制模板名称生成失败");
    }

    private String decode(String value) {
        return StringUtils.hasText(value) ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String clearPrefix(String value) {
        String text = value == null ? "" : value.trim();
        return text.replaceAll("(https?://[^/]+)?/?crmebimage/", "crmebimage/");
    }

    private void normalizeAssets(PageDiyResponse response) {
        response.setCoverImage(normalizeAsset(response.getCoverImage()));
        response.setBgPic(normalizeAsset(response.getBgPic()));
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
            return text;
        }
        if (text.startsWith("crmebimage/")) {
            return "/" + text;
        }
        return text;
    }
}
