package com.jsy.crmeb.modern.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.user.dto.SystemUserLevelResponse;
import com.jsy.crmeb.modern.service.user.dto.SystemUserLevelRequest;
import com.jsy.crmeb.modern.service.user.dto.UpdateUserLevelRequest;
import com.jsy.crmeb.modern.service.user.dto.UserGroupRequest;
import com.jsy.crmeb.modern.service.user.dto.UserGroupResponse;
import com.jsy.crmeb.modern.service.user.dto.UserIntegralRecordResponse;
import com.jsy.crmeb.modern.service.user.dto.UserIntegralSearchRequest;
import com.jsy.crmeb.modern.service.user.dto.UserOperateIntegralMoneyRequest;
import com.jsy.crmeb.modern.service.user.dto.UserResponse;
import com.jsy.crmeb.modern.service.user.dto.UserSearchRequest;
import com.jsy.crmeb.modern.service.user.dto.UserTagRequest;
import com.jsy.crmeb.modern.service.user.dto.UserTagResponse;
import com.jsy.crmeb.modern.service.user.dto.UserUpdateRequest;
import com.jsy.crmeb.modern.service.user.dto.UserUpdateSpreadRequest;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.mapper.UserFinanceMapper;
import com.jsy.crmeb.modern.service.user.mapper.UserIntegralRecordMapper;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import com.jsy.crmeb.modern.service.user.mapper.UserOptionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserAdminService {
    private final UserMapper userMapper;
    private final UserOptionMapper optionMapper;
    private final UserFinanceMapper financeMapper;
    private final UserIntegralRecordMapper integralRecordMapper;

    public UserAdminService(
            UserMapper userMapper,
            UserOptionMapper optionMapper,
            UserFinanceMapper financeMapper,
            UserIntegralRecordMapper integralRecordMapper) {
        this.userMapper = userMapper;
        this.optionMapper = optionMapper;
        this.financeMapper = financeMapper;
        this.integralRecordMapper = integralRecordMapper;
    }

    public PageResponse<UserResponse> list(UserSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        QueryWrapper<User> query = buildQuery(request, true);
        query.select(
                "uid",
                "real_name",
                "birthday",
                "card_id",
                "mark",
                "partner_id",
                "group_id",
                "tag_id",
                "nickname",
                "avatar",
                "sex",
                "country",
                "phone",
                "add_ip",
                "last_ip",
                "now_money",
                "brokerage_price",
                "integral",
                "experience",
                "sign_num",
                "status",
                "level",
                "spread_uid",
                "spread_time",
                "user_type",
                "is_promoter",
                "pay_count",
                "spread_count",
                "addres",
                "adminid",
                "login_type",
                "create_time",
                "update_time",
                "last_login_time",
                "clean_time",
                "subscribe",
                "is_logoff");
        Page<User> userPage = userMapper.selectPage(new Page<>(page, limit), query);
        List<UserGroupResponse> groups = optionMapper.selectGroups();
        List<UserTagResponse> tags = optionMapper.selectTags();
        Map<Integer, String> groupMap = groups.stream().collect(Collectors.toMap(UserGroupResponse::getId, UserGroupResponse::getGroupName));
        Map<Integer, String> tagMap = tags.stream().collect(Collectors.toMap(UserTagResponse::getId, UserTagResponse::getName));
        List<Integer> spreadUids = userPage.getRecords().stream()
                .map(User::getSpreadUid)
                .filter(uid -> uid != null && uid > 0)
                .distinct()
                .toList();
        Map<Integer, User> spreadUserMap = spreadUids.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(spreadUids).stream().collect(Collectors.toMap(User::getUid, Function.identity()));
        List<UserResponse> list = userPage.getRecords().stream()
                .map(user -> toResponse(user, groupMap, tagMap, spreadUserMap))
                .toList();
        return new PageResponse<>(page, limit, userPage.getTotal(), list);
    }

    public PageResponse<UserIntegralRecordResponse> integralRecords(UserIntegralSearchRequest request) {
        UserIntegralSearchRequest safeRequest = request == null ? new UserIntegralSearchRequest() : request;
        int page = safeRequest.getPage() == null || safeRequest.getPage() <= 0 ? 1 : safeRequest.getPage();
        int limit = safeRequest.getLimit() == null || safeRequest.getLimit() <= 0 ? 20 : Math.min(safeRequest.getLimit(), 100);
        String keywords = emptyToNull(safeRequest.getKeywords());
        String[] dateRange = parseIntegralDateLimit(safeRequest.getDateLimit());
        long total = integralRecordMapper.countAdminRecords(safeRequest.getUid(), keywords, dateRange[0], dateRange[1]);
        List<UserIntegralRecordResponse> list = total <= 0
                ? List.of()
                : integralRecordMapper.selectAdminRecords(
                        safeRequest.getUid(),
                        keywords,
                        dateRange[0],
                        dateRange[1],
                        (page - 1) * limit,
                        limit);
        return new PageResponse<>(page, limit, total, list);
    }

    public PageResponse<UserGroupResponse> groups(int page, int limit) {
        List<UserGroupResponse> all = optionMapper.selectGroups();
        return slice(page, limit, all);
    }

    public PageResponse<UserTagResponse> tags(int page, int limit) {
        List<UserTagResponse> all = optionMapper.selectTags();
        return slice(page, limit, all);
    }

    public UserGroupResponse groupInfo(Integer id) {
        validateId(id, "分组id不能为空");
        UserGroupResponse group = optionMapper.selectGroupById(id);
        if (group == null) {
            throw new IllegalArgumentException("用户分组不存在");
        }
        return group;
    }

    public UserTagResponse tagInfo(Integer id) {
        validateId(id, "标签id不能为空");
        UserTagResponse tag = optionMapper.selectTagById(id);
        if (tag == null) {
            throw new IllegalArgumentException("用户标签不存在");
        }
        return tag;
    }

    @Transactional
    public void saveGroup(UserGroupRequest request) {
        String groupName = validateGroupName(request);
        if (optionMapper.countGroupName(groupName, null) > 0) {
            throw new IllegalArgumentException("分组名称已存在");
        }
        optionMapper.insertGroup(groupName);
    }

    @Transactional
    public void saveTag(UserTagRequest request) {
        String name = validateTagName(request);
        if (optionMapper.countTagName(name, null) > 0) {
            throw new IllegalArgumentException("标签名称已存在");
        }
        optionMapper.insertTag(name);
    }

    @Transactional
    public void updateGroup(Integer id, UserGroupRequest request) {
        groupInfo(id);
        String groupName = validateGroupName(request);
        if (optionMapper.countGroupName(groupName, id) > 0) {
            throw new IllegalArgumentException("分组名称已存在");
        }
        optionMapper.updateGroup(id, groupName);
    }

    @Transactional
    public void updateTag(Integer id, UserTagRequest request) {
        tagInfo(id);
        String name = validateTagName(request);
        if (optionMapper.countTagName(name, id) > 0) {
            throw new IllegalArgumentException("标签名称已存在");
        }
        optionMapper.updateTag(id, name);
    }

    @Transactional
    public void deleteGroup(Integer id) {
        groupInfo(id);
        if (optionMapper.deleteGroup(id) > 0) {
            userMapper.clearGroupId(String.valueOf(id));
        }
    }

    @Transactional
    public void deleteTag(Integer id) {
        tagInfo(id);
        if (optionMapper.deleteTag(id) > 0) {
            userMapper.clearTagId(String.valueOf(id));
        }
    }

    public List<SystemUserLevelResponse> levels() {
        return optionMapper.selectLevels();
    }

    public SystemUserLevelResponse levelInfo(Integer id) {
        validateId(id, "等级id不能为空");
        SystemUserLevelResponse level = optionMapper.selectLevel(id);
        if (level == null) {
            throw new IllegalArgumentException("等级不存在");
        }
        return level;
    }

    @Transactional
    public void saveLevel(SystemUserLevelRequest request) {
        LevelData levelData = normalizeLevel(request, null);
        if (optionMapper.countLevelName(levelData.name(), null) > 0) {
            throw new IllegalArgumentException("用户等级名称重复");
        }
        if (optionMapper.countLevelGrade(levelData.grade(), null) > 0) {
            throw new IllegalArgumentException("用户等级级别重复");
        }
        validateExperience(levelData.experience(), levelData.grade(), null);
        optionMapper.insertLevel(levelData.name(), levelData.experience(), levelData.isShow(), levelData.grade(), levelData.discount(), levelData.icon());
    }

    @Transactional
    public void updateLevel(Integer id, SystemUserLevelRequest request) {
        levelInfo(id);
        LevelData levelData = normalizeLevel(request, id);
        if (optionMapper.countLevelName(levelData.name(), id) > 0) {
            throw new IllegalArgumentException("用户等级名称重复");
        }
        if (optionMapper.countLevelGrade(levelData.grade(), id) > 0) {
            throw new IllegalArgumentException("用户等级级别重复");
        }
        validateExperience(levelData.experience(), levelData.grade(), id);
        optionMapper.updateLevel(id, levelData.name(), levelData.experience(), levelData.isShow(), levelData.grade(), levelData.discount(), levelData.icon());
        optionMapper.deleteUserLevelRecords(id);
        userMapper.clearLevelByLevelId(id);
    }

    @Transactional
    public void deleteLevel(Integer id) {
        levelInfo(id);
        if (optionMapper.deleteLevel(id) > 0) {
            optionMapper.deleteUserLevelRecords(id);
            userMapper.clearLevelByLevelId(id);
        }
    }

    @Transactional
    public void updateLevelShow(Integer id, Boolean isShow) {
        levelInfo(id);
        if (optionMapper.updateLevelShow(id, isShow) > 0 && !Boolean.TRUE.equals(isShow)) {
            optionMapper.deleteUserLevelRecords(id);
            userMapper.clearLevelByLevelId(id);
        }
    }

    public UserResponse info(Integer id) {
        User user = getExistingUser(id);
        List<UserGroupResponse> groups = optionMapper.selectGroups();
        List<UserTagResponse> tags = optionMapper.selectTags();
        Map<Integer, String> groupMap = groups.stream().collect(Collectors.toMap(UserGroupResponse::getId, UserGroupResponse::getGroupName));
        Map<Integer, String> tagMap = tags.stream().collect(Collectors.toMap(UserTagResponse::getId, UserTagResponse::getName));
        Map<Integer, User> spreadUserMap = user.getSpreadUid() == null || user.getSpreadUid() <= 0
                ? Map.of()
                : Map.of(user.getSpreadUid(), getExistingUser(user.getSpreadUid()));
        return toResponse(user, groupMap, tagMap, spreadUserMap);
    }

    @Transactional
    public void update(UserUpdateRequest request, Integer id) {
        Integer uid = id != null && id > 0 ? id : request.getUid();
        User user = getExistingUser(uid);
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (request.getIsPromoter() == null) {
            throw new IllegalArgumentException("是否为推广员不能为空");
        }
        userMapper.updateBasic(
                user.getUid(),
                emptyToNull(request.getRealName()),
                emptyToNull(request.getBirthday()),
                emptyToNull(request.getCardId()),
                emptyToNull(request.getMark()),
                Boolean.TRUE.equals(request.getStatus()) ? 1 : 0,
                emptyToNull(request.getAddres()),
                emptyToNull(request.getGroupId()),
                emptyToNull(request.getTagId()),
                Boolean.TRUE.equals(request.getIsPromoter()) ? 1 : 0);
    }

    @Transactional
    public void updateSpread(UserUpdateSpreadRequest request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("请选择用户");
        }
        if (request.getSpreadUid() == null || request.getSpreadUid() <= 0) {
            throw new IllegalArgumentException("请选择推广人");
        }
        if (request.getUserId().equals(request.getSpreadUid())) {
            throw new IllegalArgumentException("推广人不能为当前用户");
        }
        User user = getExistingUser(request.getUserId());
        if (request.getSpreadUid().equals(user.getSpreadUid())) {
            throw new IllegalArgumentException("当前推广人已经是所选人");
        }
        User spreadUser = getExistingUser(request.getSpreadUid());
        if (request.getUserId().equals(spreadUser.getSpreadUid())) {
            throw new IllegalArgumentException("当前用户已是推广人的上级");
        }
        Integer oldSpreadUid = user.getSpreadUid();
        userMapper.updateSpread(user.getUid(), spreadUser.getUid(), LocalDateTime.now());
        userMapper.updateSpreadCount(spreadUser.getUid(), 1);
        if (oldSpreadUid != null && oldSpreadUid > 0) {
            userMapper.updateSpreadCount(oldSpreadUid, -1);
        }
    }

    @Transactional
    public void clearSpread(Integer id) {
        User user = getExistingUser(id);
        Integer oldSpreadUid = user.getSpreadUid();
        if (oldSpreadUid == null || oldSpreadUid <= 0) {
            return;
        }
        userMapper.clearSpread(user.getUid());
        userMapper.updateSpreadCount(oldSpreadUid, -1);
    }

    @Transactional
    public void updatePhone(Integer id, String phone) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式错误，请输入正确的手机号");
        }
        User user = getExistingUser(id);
        if (phone.equals(user.getPhone())) {
            throw new IllegalArgumentException("手机号与之前一致");
        }
        Long exists = userMapper.selectCount(new QueryWrapper<User>().eq("phone", phone).ne("uid", id));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("此手机号码已被注册");
        }
        userMapper.updatePhone(id, phone);
    }

    @Transactional
    public void group(String ids, String groupId) {
        if (!StringUtils.hasText(ids)) {
            throw new IllegalArgumentException("会员编号不能为空");
        }
        if (!StringUtils.hasText(groupId)) {
            throw new IllegalArgumentException("分组id不能为空");
        }
        List<Integer> uidList = parseUidList(ids);
        assertUsersExist(uidList);
        for (Integer uid : uidList) {
            userMapper.updateGroupId(uid, groupId);
        }
    }

    @Transactional
    public void tag(String ids, String tagId) {
        if (!StringUtils.hasText(ids)) {
            throw new IllegalArgumentException("会员编号不能为空");
        }
        if (!StringUtils.hasText(tagId)) {
            throw new IllegalArgumentException("标签id不能为空");
        }
        List<Integer> uidList = parseUidList(ids);
        assertUsersExist(uidList);
        for (Integer uid : uidList) {
            userMapper.updateTagId(uid, tagId);
        }
    }

    @Transactional
    public void operateFunds(UserOperateIntegralMoneyRequest request) {
        if (request.getUid() == null || request.getUid() <= 0) {
            throw new IllegalArgumentException("请输入正确的uid");
        }
        BigDecimal moneyValue = request.getMoneyValue() == null ? BigDecimal.ZERO : request.getMoneyValue();
        int integralValue = request.getIntegralValue() == null ? 0 : request.getIntegralValue();
        if (moneyValue.compareTo(BigDecimal.ZERO) <= 0 && integralValue <= 0) {
            throw new IllegalArgumentException("修改值不能小于等于0");
        }
        if (!Integer.valueOf(1).equals(request.getMoneyType()) && !Integer.valueOf(2).equals(request.getMoneyType())) {
            throw new IllegalArgumentException("请选择正确的余额类型");
        }
        if (!Integer.valueOf(1).equals(request.getIntegralType()) && !Integer.valueOf(2).equals(request.getIntegralType())) {
            throw new IllegalArgumentException("请选择正确的积分类型");
        }
        User user = getExistingUser(request.getUid());
        BigDecimal nowMoney = user.getNowMoney() == null ? BigDecimal.ZERO : user.getNowMoney();
        int integral = user.getIntegral() == null ? 0 : user.getIntegral();
        BigDecimal nextMoney = nowMoney;
        int nextIntegral = integral;

        if (moneyValue.compareTo(BigDecimal.ZERO) > 0) {
            if (Integer.valueOf(1).equals(request.getMoneyType())) {
                nextMoney = nowMoney.add(moneyValue);
                if (nextMoney.compareTo(new BigDecimal("99999999.99")) > 0) {
                    throw new IllegalArgumentException("余额添加后不能大于99999999.99");
                }
                financeMapper.insertUserBill(user.getUid(), 1, "system_add", moneyValue, nextMoney, "后台操作增加了" + moneyValue + "余额");
            } else {
                nextMoney = nowMoney.subtract(moneyValue);
                if (nextMoney.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("余额扣减后不能小于0");
                }
                financeMapper.insertUserBill(user.getUid(), 0, "system_sub", moneyValue, nextMoney, "后台操作减少了" + moneyValue + "余额");
            }
        }

        if (integralValue > 0) {
            if (Integer.valueOf(1).equals(request.getIntegralType())) {
                nextIntegral = integral + integralValue;
                if (nextIntegral > 99999999) {
                    throw new IllegalArgumentException("积分添加后不能大于99999999");
                }
                financeMapper.insertIntegralRecord(user.getUid(), 1, integralValue, nextIntegral, "后台操作增加了" + integralValue + "积分");
            } else {
                nextIntegral = integral - integralValue;
                if (nextIntegral < 0) {
                    throw new IllegalArgumentException("积分扣减后不能小于0");
                }
                financeMapper.insertIntegralRecord(user.getUid(), 2, integralValue, nextIntegral, "后台操作减少了" + integralValue + "积分");
            }
        }
        userMapper.updateMoneyAndIntegral(user.getUid(), nextMoney, nextIntegral);
    }

    @Transactional
    public void updateUserLevel(UpdateUserLevelRequest request) {
        if (request.getUid() == null || request.getUid() <= 0) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        if (request.getLevelId() == null || request.getLevelId() <= 0) {
            throw new IllegalArgumentException("等级id不能为空");
        }
        User user = getExistingUser(request.getUid());
        if (Integer.valueOf(request.getLevelId()).equals(user.getLevel())) {
            throw new IllegalArgumentException("用户等级与修改前相同");
        }
        SystemUserLevelResponse level = optionMapper.selectLevelById(request.getLevelId());
        if (level == null) {
            throw new IllegalArgumentException("系统会员等级不存在，请先配置");
        }
        userMapper.updateLevel(user.getUid(), request.getLevelId());
        financeMapper.insertUserLevel(
                user.getUid(),
                level.getId(),
                level.getGrade(),
                "尊敬的用户 " + (user.getNickname() == null ? "" : user.getNickname())
                        + ",在" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        + "管理员调整会员等级成为" + level.getName(),
                level.getDiscount());
    }

    private <T> PageResponse<T> slice(int page, int limit, List<T> all) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : limit;
        int start = Math.min((safePage - 1) * safeLimit, all.size());
        int end = Math.min(start + safeLimit, all.size());
        return new PageResponse<>(safePage, safeLimit, all.size(), all.subList(start, end));
    }

    private User getExistingUser(Integer uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new IllegalArgumentException("对应用户不存在");
        }
        return user;
    }

    private void validateId(Integer id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateExperience(Integer experience, Integer grade, Integer ignoreId) {
        if (grade != null && grade > 1) {
            SystemUserLevelResponse prev = optionMapper.selectLevels().stream()
                    .filter(item -> item.getGrade() != null && item.getGrade() < grade && (ignoreId == null || !ignoreId.equals(item.getId())))
                    .reduce((a, b) -> a.getGrade() >= b.getGrade() ? a : b)
                    .orElse(null);
            if (prev != null && prev.getExperience() != null && prev.getExperience() >= experience) {
                throw new IllegalArgumentException("当前等级的经验不能比上一级别的经验低");
            }
        }
        SystemUserLevelResponse next = optionMapper.selectLevels().stream()
                .filter(item -> item.getGrade() != null && item.getGrade() > grade && (ignoreId == null || !ignoreId.equals(item.getId())))
                .reduce((a, b) -> a.getGrade() <= b.getGrade() ? a : b)
                .orElse(null);
        if (next != null && next.getExperience() != null && next.getExperience() <= experience) {
            throw new IllegalArgumentException("当前等级的经验不能比下一级别的经验高");
        }
    }

    private LevelData normalizeLevel(SystemUserLevelRequest request, Integer id) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("等级名称不能为空");
        }
        if (request.getExperience() == null) {
            throw new IllegalArgumentException("等级经验不能为空");
        }
        if (request.getGrade() == null || request.getGrade() <= 0) {
            throw new IllegalArgumentException("会员等级不能为空");
        }
        if (request.getDiscount() == null || request.getDiscount() <= 0 || request.getDiscount() > 100) {
            throw new IllegalArgumentException("折扣值不能小于1且不能大于100");
        }
        if (!StringUtils.hasText(request.getIcon())) {
            throw new IllegalArgumentException("会员图标不能为空");
        }
        return new LevelData(
                request.getName().trim(),
                request.getExperience(),
                request.getGrade(),
                request.getDiscount(),
                clearImagePrefix(request.getIcon()),
                request.getIsShow() == null ? Boolean.TRUE : request.getIsShow());
    }

    private record LevelData(String name, Integer experience, Integer grade, Integer discount, String icon, Boolean isShow) {}

    private String clearImagePrefix(String value) {
        String text = value == null ? "" : value.trim();
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.startsWith("http://") || text.startsWith("https://")) {
            int index = text.indexOf("/crmebimage/");
            return index >= 0 ? text.substring(index + 1) : text;
        }
        return text.startsWith("/") ? text.substring(1) : text;
    }

    private String validateGroupName(UserGroupRequest request) {
        String groupName = request == null ? null : request.getGroupName();
        if (!StringUtils.hasText(groupName)) {
            throw new IllegalArgumentException("请填写分组名称");
        }
        String value = groupName.trim();
        if (value.length() > 64) {
            throw new IllegalArgumentException("用户分组名称不能超过64个字符");
        }
        return value;
    }

    private String validateTagName(UserTagRequest request) {
        String name = request == null ? null : request.getName();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("请填写标签名称");
        }
        String value = name.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("标签名称不能超过50个字符");
        }
        return value;
    }

    private List<Integer> parseUidList(String ids) {
        List<Integer> uidList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Integer::parseInt)
                .distinct()
                .toList();
        if (uidList.isEmpty()) {
            throw new IllegalArgumentException("会员编号不能为空");
        }
        return uidList;
    }

    private void assertUsersExist(List<Integer> uidList) {
        if (userMapper.selectBatchIds(uidList).size() != uidList.size()) {
            throw new IllegalArgumentException("没有找到用户信息");
        }
    }

    private QueryWrapper<User> buildQuery(UserSearchRequest request, boolean includeOrder) {
        QueryWrapper<User> query = new QueryWrapper<>();
        if (request.getIsPromoter() != null) {
            query.eq("is_promoter", request.getIsPromoter());
        }
        if (StringUtils.hasText(request.getGroupId())) {
            query.in("group_id", splitIds(request.getGroupId()));
        }
        if (StringUtils.hasText(request.getLabelId())) {
            List<String> ids = splitIds(request.getLabelId());
            query.and(wrapper -> {
                boolean first = true;
                for (String id : ids) {
                    if (first) {
                        wrapper.apply("FIND_IN_SET({0}, tag_id)", id);
                        first = false;
                    } else {
                        wrapper.or().apply("FIND_IN_SET({0}, tag_id)", id);
                    }
                }
            });
        }
        if (StringUtils.hasText(request.getLevel())) {
            query.in("level", splitIds(request.getLevel()));
        }
        if (StringUtils.hasText(request.getUserType())) {
            switch (request.getUserType()) {
                case "wechat" -> query.exists("select 1 from eb_user_token ut where ut.uid = eb_user.uid and ut.type = 1");
                case "routine" -> query.exists("select 1 from eb_user_token ut where ut.uid = eb_user.uid and ut.type = 2");
                case "h5" -> query.eq("user_type", "h5");
                default -> {
                }
            }
        }
        if (StringUtils.hasText(request.getSex())) {
            query.eq("sex", request.getSex());
        }
        if (StringUtils.hasText(request.getCountry())) {
            query.eq("country", request.getCountry());
            if (StringUtils.hasText(request.getCity())) {
                String province = stripRegionSuffix(request.getProvince());
                String city = stripRegionSuffix(request.getCity());
                query.like("addres", province + "," + city);
            }
        }
        if (StringUtils.hasText(request.getPayCount())) {
            int payCount = Integer.parseInt(request.getPayCount());
            if (payCount <= 0) {
                query.eq("pay_count", 0);
            } else {
                query.ge("pay_count", payCount);
            }
        }
        if (request.getStatus() != null) {
            query.eq("status", request.getStatus());
        }
        applyDateLimit(query, request.getDateLimit(), request.getAccessType());
        if (StringUtils.hasText(request.getKeywords())) {
            String keyword = request.getKeywords().trim();
            query.and(wrapper -> wrapper
                    .like("phone", keyword)
                    .or().like("nickname", keyword)
                    .or().like("mark", keyword));
        }
        if (includeOrder) {
            query.orderByDesc("uid");
        }
        return query;
    }

    private void applyDateLimit(QueryWrapper<User> query, String dateLimit, Integer accessType) {
        if (!StringUtils.hasText(dateLimit)) {
            return;
        }
        String[] parts = dateLimit.trim().split("\\s*,\\s*|\\s+-\\s+");
        if (parts.length < 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = LocalDate.parse(parts[0].trim(), formatter).atStartOfDay();
        LocalDateTime end = LocalDate.parse(parts[1].trim(), formatter).plusDays(1).atStartOfDay().minusNanos(1);
        int type = accessType == null ? 0 : accessType;
        if (type == 1) {
            query.between("create_time", start, end).apply("create_time = last_login_time");
        } else if (type == 2) {
            query.between("last_login_time", start, end);
        } else if (type == 3) {
            query.notBetween("last_login_time", start, end);
        } else {
            query.between("last_login_time", start, end);
        }
    }

    private String[] parseIntegralDateLimit(String dateLimit) {
        if (!StringUtils.hasText(dateLimit)) {
            return new String[] { null, null };
        }
        String[] parts = dateLimit.trim().split("\\s*,\\s*|\\s+-\\s+");
        if (parts.length < 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            return new String[] { null, null };
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(parts[0].trim(), formatter);
        LocalDate endDate = LocalDate.parse(parts[1].trim(), formatter);
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("开始时间不能大于结束时间！");
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
        return new String[] { start.toString().replace('T', ' '), end.toString().replace('T', ' ') };
    }

    private UserResponse toResponse(
            User user,
            Map<Integer, String> groupMap,
            Map<Integer, String> tagMap,
            Map<Integer, User> spreadUserMap) {
        UserResponse response = new UserResponse();
        response.setUid(user.getUid());
        response.setRealName(user.getRealName());
        response.setBirthday(user.getBirthday());
        response.setCardId(user.getCardId());
        response.setMark(user.getMark());
        response.setPartnerId(user.getPartnerId());
        response.setGroupId(user.getGroupId());
        response.setGroupName(namesFromIds(user.getGroupId(), groupMap));
        response.setTagId(user.getTagId());
        response.setTagName(namesFromIds(user.getTagId(), tagMap));
        response.setNickname(user.getNickname());
        response.setAvatar(normalizeAsset(user.getAvatar()));
        response.setSex(user.getSex() == null ? 0 : user.getSex());
        response.setCountry(user.getCountry());
        response.setPhone(maskMobile(user.getPhone()));
        response.setAddIp(user.getAddIp());
        response.setLastIp(user.getLastIp());
        response.setNowMoney(user.getNowMoney());
        response.setBrokeragePrice(user.getBrokeragePrice());
        response.setIntegral(user.getIntegral());
        response.setExperience(user.getExperience());
        response.setSignNum(user.getSignNum());
        response.setStatus(Integer.valueOf(1).equals(user.getStatus()));
        response.setLevel(user.getLevel());
        response.setSpreadUid(user.getSpreadUid());
        response.setSpreadTime(user.getSpreadTime());
        User spreadUser = spreadUserMap.get(user.getSpreadUid());
        response.setSpreadNickname(spreadUser == null ? "无" : spreadUser.getNickname());
        response.setUserType(user.getUserType());
        response.setIsPromoter(Integer.valueOf(1).equals(user.getIsPromoter()));
        response.setPayCount(user.getPayCount());
        response.setSpreadCount(user.getSpreadCount());
        response.setAddres(user.getAddres());
        response.setAdminid(user.getAdminid());
        response.setLoginType(user.getLoginType());
        response.setUpdateTime(user.getUpdateTime());
        response.setCreateTime(user.getCreateTime());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setCleanTime(user.getCleanTime());
        response.setSubscribe(Integer.valueOf(1).equals(user.getSubscribe()));
        return response;
    }

    private String namesFromIds(String value, Map<Integer, String> nameMap) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::safeParseInteger)
                .filter(id -> id != null)
                .map(nameMap::get)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private Integer safeParseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<String> splitIds(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String stripRegionSuffix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace("省", "").replace("市", "");
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String maskMobile(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        return "/" + trimmed;
    }
}
