package com.jsy.crmeb.modern.service.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jsy.crmeb.modern.service.product.dto.CategoryRequest;
import com.jsy.crmeb.modern.service.product.dto.CategoryTreeResponse;
import com.jsy.crmeb.modern.service.product.entity.Category;
import com.jsy.crmeb.modern.service.product.mapper.CategoryMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductCatalogService {
    private final CategoryMapper categoryMapper;

    public ProductCatalogService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryTreeResponse> getTree(Integer type, Integer status, String name) {
        List<Category> categories = queryCategories(type, status, null, name);
        if (StringUtils.hasText(name) && !categories.isEmpty()) {
            categories = withSearchParents(categories, type, status);
        }
        return buildTree(categories.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    public List<CategoryTreeResponse> getList(Integer type, Integer status, Integer pid, String name) {
        return queryCategories(type, status, pid, name).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CategoryTreeResponse> getByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        QueryWrapper<Category> query = new QueryWrapper<>();
        query.in("id", ids).orderByDesc("sort").orderByAsc("id");
        return categoryMapper.selectList(query).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CategoryTreeResponse info(Integer id) {
        Category category = requiredCategory(id);
        return toResponse(category);
    }

    @Transactional
    public boolean create(CategoryRequest request) {
        validateRequest(request, false);
        String path = getPathByPid(request.getPid());
        ensureUniqueName(request.getName(), request.getType(), path, null);
        Category category = fromRequest(request);
        category.setPath(path);
        return categoryMapper.insert(category) > 0;
    }

    @Transactional
    public boolean update(Integer id, CategoryRequest request) {
        validateId(id);
        validateRequest(request, true);
        Category current = requiredCategory(id);
        if (isChildOf(request.getPid(), id)) {
            throw new IllegalArgumentException("不能选择自己的子级作为父级");
        }
        String path = getPathByPid(request.getPid());
        ensureUniqueName(request.getName(), request.getType(), path, id);
        Category category = fromRequest(request);
        category.setId(id);
        category.setPath(path);
        boolean updated = categoryMapper.updateById(category) > 0;
        if (updated) {
            updateDescendantPaths(current, category);
            if (!Boolean.TRUE.equals(request.getStatus())) {
                updateStatusByPid(id, false);
            } else {
                updateParentStatusById(id);
            }
        }
        return updated;
    }

    @Transactional
    public boolean delete(Integer id) {
        requiredCategory(id);
        QueryWrapper<Category> childQuery = new QueryWrapper<>();
        childQuery.like("path", "/" + id + "/");
        if (categoryMapper.selectCount(childQuery) > 0) {
            throw new IllegalArgumentException("当前分类下有子类，请先删除子类！");
        }
        return categoryMapper.deleteById(id) > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id) {
        Category category = requiredCategory(id);
        boolean nextStatus = !Integer.valueOf(1).equals(category.getStatus());
        Category update = new Category();
        update.setId(id);
        update.setStatus(nextStatus ? 1 : 0);
        return categoryMapper.updateById(update) > 0;
    }

    private List<Category> queryCategories(Integer type, Integer status, Integer pid, String name) {
        QueryWrapper<Category> query = new QueryWrapper<>();
        query.eq("type", type == null ? 1 : type);
        if (status != null && status >= 0) {
            query.eq("status", status);
        }
        if (pid != null) {
            query.eq("pid", pid);
        }
        if (StringUtils.hasText(name)) {
            query.like("name", name.trim());
        }
        query.orderByDesc("sort").orderByAsc("id");
        return categoryMapper.selectList(query);
    }

    public String getCategoryNames(String cateId) {
        if (!StringUtils.hasText(cateId)) {
            return "";
        }
        List<Integer> ids = new ArrayList<>();
        for (String item : cateId.split(",")) {
            if (StringUtils.hasText(item)) {
                ids.add(Integer.valueOf(item.trim()));
            }
        }
        if (ids.isEmpty()) {
            return "";
        }
        return categoryMapper.selectBatchIds(ids).stream().map(Category::getName).collect(Collectors.joining(","));
    }

    private Category fromRequest(CategoryRequest request) {
        Category category = new Category();
        category.setPid(request.getPid());
        category.setName(request.getName().trim());
        category.setType(request.getType());
        category.setUrl(valueOrEmpty(request.getUrl()));
        category.setExtra(clearImagePrefix(request.getExtra()));
        category.setStatus(Boolean.TRUE.equals(request.getStatus()) ? 1 : 0);
        category.setSort(request.getSort());
        return category;
    }

    private void validateRequest(CategoryRequest request, boolean update) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (request.getPid() == null || request.getPid() < 0) {
            throw new IllegalArgumentException("请选择父级分类");
        }
        if (StringUtils.hasText(request.getName())) {
            if (request.getName().trim().length() > 50) {
                throw new IllegalArgumentException("分类名称不能超过50个字符");
            }
        } else {
            throw new IllegalArgumentException("分类名称必须填写");
        }
        if (request.getType() == null || request.getType() < 1 || request.getType() > 7) {
            throw new IllegalArgumentException("类型必须选择");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("状态必须选择");
        }
        if (request.getSort() == null || request.getSort() < 0) {
            throw new IllegalArgumentException("排序数字必须大于等于0");
        }
        if (request.getPid() > 0) {
            Category parent = categoryMapper.selectById(request.getPid());
            if (parent == null) {
                throw new IllegalArgumentException("父级分类不存在");
            }
            if (!Objects.equals(parent.getType(), request.getType())) {
                throw new IllegalArgumentException("父级分类类型不一致");
            }
        }
        if (update && request.getId() != null && Objects.equals(request.getId(), request.getPid())) {
            throw new IllegalArgumentException("父级不能选择自己");
        }
    }

    private Category requiredCategory(Integer id) {
        validateId(id);
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id 参数不合法");
        }
    }

    private void ensureUniqueName(String name, Integer type, String path, Integer ignoreId) {
        QueryWrapper<Category> query = new QueryWrapper<>();
        query.eq("name", name.trim()).eq("type", type).eq("path", path);
        if (ignoreId != null) {
            query.ne("id", ignoreId);
        }
        if (categoryMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("此分类已存在");
        }
    }

    private String getPathByPid(Integer pid) {
        if (pid == null || pid <= 0) {
            return "/0/";
        }
        Category parent = requiredCategory(pid);
        return parent.getPath() + pid + "/";
    }

    private boolean isChildOf(Integer pid, Integer id) {
        if (pid == null || pid <= 0 || id == null) {
            return false;
        }
        Category parent = categoryMapper.selectById(pid);
        return parent != null && parent.getPath() != null && parent.getPath().contains("/" + id + "/");
    }

    private void updateStatusByPid(Integer pid, boolean status) {
        Category update = new Category();
        update.setStatus(status ? 1 : 0);
        UpdateWrapper<Category> wrapper = new UpdateWrapper<>();
        wrapper.like("path", "/" + pid + "/");
        categoryMapper.update(update, wrapper);
    }

    private void updateParentStatusById(Integer id) {
        Category category = categoryMapper.selectById(id);
        if (category == null || !StringUtils.hasText(category.getPath())) {
            return;
        }
        for (Integer parentId : idsFromPath(category.getPath())) {
            if (parentId > 0) {
                Category parent = new Category();
                parent.setId(parentId);
                parent.setStatus(1);
                categoryMapper.updateById(parent);
            }
        }
    }

    private void updateDescendantPaths(Category oldCategory, Category newCategory) {
        String oldPrefix = oldCategory.getPath() + oldCategory.getId() + "/";
        String newPrefix = newCategory.getPath() + newCategory.getId() + "/";
        if (oldPrefix.equals(newPrefix)) {
            return;
        }
        QueryWrapper<Category> query = new QueryWrapper<>();
        query.like("path", "/" + oldCategory.getId() + "/");
        for (Category child : categoryMapper.selectList(query)) {
            if (StringUtils.hasText(child.getPath()) && child.getPath().startsWith(oldPrefix)) {
                Category update = new Category();
                update.setId(child.getId());
                update.setPath(newPrefix + child.getPath().substring(oldPrefix.length()));
                categoryMapper.updateById(update);
            }
        }
    }

    private List<Integer> idsFromPath(String path) {
        List<Integer> ids = new ArrayList<>();
        if (!StringUtils.hasText(path)) {
            return ids;
        }
        for (String value : path.split("/")) {
            if (StringUtils.hasText(value)) {
                ids.add(Integer.valueOf(value));
            }
        }
        return ids;
    }

    private List<Category> withSearchParents(List<Category> categories, Integer type, Integer status) {
        Map<Integer, Category> byId = categories.stream().collect(Collectors.toMap(Category::getId, item -> item, (left, right) -> left));
        List<Integer> missingParentIds = categories.stream()
                .map(Category::getPid)
                .filter(pid -> pid != null && pid > 0 && !byId.containsKey(pid))
                .distinct()
                .toList();
        if (missingParentIds.isEmpty()) {
            return categories;
        }
        QueryWrapper<Category> query = new QueryWrapper<>();
        query.in("id", missingParentIds).eq("type", type == null ? 1 : type);
        if (status != null && status >= 0) {
            query.eq("status", status);
        }
        List<Category> merged = new ArrayList<>(categories);
        merged.addAll(categoryMapper.selectList(query));
        return merged;
    }

    private String clearImagePrefix(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.startsWith("http://") || text.startsWith("https://")) {
            int index = text.indexOf("/crmebimage/");
            return index >= 0 ? text.substring(index + 1) : text;
        }
        return text.startsWith("/") ? text.substring(1) : text;
    }

    private String valueOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<CategoryTreeResponse> buildTree(List<CategoryTreeResponse> categories) {
        Map<Integer, List<CategoryTreeResponse>> childrenByPid = categories.stream()
                .collect(Collectors.groupingBy(CategoryTreeResponse::getPid));
        List<CategoryTreeResponse> roots = new ArrayList<>(childrenByPid.getOrDefault(0, List.of()));
        roots.forEach(root -> attachChildren(root, childrenByPid));
        return sortCategories(roots);
    }

    private void attachChildren(CategoryTreeResponse parent, Map<Integer, List<CategoryTreeResponse>> childrenByPid) {
        List<CategoryTreeResponse> children = new ArrayList<>(childrenByPid.getOrDefault(parent.getId(), List.of()));
        children.forEach(child -> attachChildren(child, childrenByPid));
        parent.setChild(sortCategories(children));
    }

    private List<CategoryTreeResponse> sortCategories(List<CategoryTreeResponse> categories) {
        return categories.stream()
                .sorted(Comparator
                        .comparing((CategoryTreeResponse category) -> Objects.requireNonNullElse(category.getSort(), 0)).reversed()
                        .thenComparing(CategoryTreeResponse::getId))
                .collect(Collectors.toList());
    }

    private CategoryTreeResponse toResponse(Category category) {
        CategoryTreeResponse response = new CategoryTreeResponse();
        response.setId(category.getId());
        response.setPid(category.getPid());
        response.setPath(category.getPath());
        response.setName(category.getName());
        response.setType(category.getType());
        response.setUrl(category.getUrl());
        response.setExtra(category.getExtra());
        response.setStatus(Integer.valueOf(1).equals(category.getStatus()));
        response.setSort(category.getSort());
        return response;
    }
}
