package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.combination.StorePink;

import java.util.List;

/**
 * 拼团表 Mapper 接口
 */
public interface StorePinkDao extends BaseMapper<StorePink> {

    List<StorePink> selectSizePink(Integer size);
}
