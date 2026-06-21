package com.jsy.crmeb.modern.service.store.dto;

import com.jsy.crmeb.modern.service.admin.dto.SystemAdminListResponse;
import com.jsy.crmeb.modern.service.store.entity.SystemStore;
import java.time.LocalDateTime;

public class SystemStoreStaffResponse {
    private Integer id;
    private Integer uid;
    private String avatar;
    private SystemAdminListResponse user;
    private Integer storeId;
    private SystemStore systemStore;
    private String staffName;
    private String phone;
    private Integer verifyStatus;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public SystemAdminListResponse getUser() {
        return user;
    }

    public void setUser(SystemAdminListResponse user) {
        this.user = user;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public SystemStore getSystemStore() {
        return systemStore;
    }

    public void setSystemStore(SystemStore systemStore) {
        this.systemStore = systemStore;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(Integer verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
