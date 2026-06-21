package com.zbkj.service.service.impl;

import com.zbkj.service.service.ArticleService;
import com.zbkj.service.service.UserTokenService;
import com.zbkj.service.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 微信用户表 服务实现类
 */
@Service
public class WechatUserServiceImpl implements WechatUserService {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserTokenService userTokenService;

}
