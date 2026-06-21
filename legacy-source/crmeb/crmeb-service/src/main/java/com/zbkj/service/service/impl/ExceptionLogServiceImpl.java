package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.exception.ExceptionLog;
import com.zbkj.service.dao.ExceptionLogDao;
import com.zbkj.service.service.ExceptionLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * ExceptionLogServiceImpl 接口实现


 */
@Service
public class ExceptionLogServiceImpl extends ServiceImpl<ExceptionLogDao, ExceptionLog> implements ExceptionLogService {

    @Resource
    private ExceptionLogDao dao;

}

