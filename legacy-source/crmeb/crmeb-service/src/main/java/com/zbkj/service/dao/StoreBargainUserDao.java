package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.bargain.StoreBargainUser;

import java.util.List;

/**
 * 用户参与砍价表 Mapper 接口
 */
public interface StoreBargainUserDao extends BaseMapper<StoreBargainUser> {

    List<StoreBargainUser> selectHeaderList();
}
