package com.zbkj.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 基础配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "crmeb")
public class CrmebConfig {
    // 当前代码版本
    private String version;
    // 待部署域名
    private String domain;
    // #请求微信接口中专服务器
    private String wechatApiUrl;
    // #微信js api系列是否开启调试模式
    private boolean wechatJsApiDebug;
    // #微信js api是否是beta版本
    private boolean wechatJsApiBeta;
    // #是否同步config表数据到redis
    private boolean asyncConfig;
    // #是否同步小程序公共模板库
    private boolean asyncWeChatProgramTempList;
    // 本地图片路径配置
    private String imagePath;
    // 是否演示站点 所有手机号码都会掩码
    private Boolean demoSite;
    // 活动边框缓存周期
    private Integer activityStyleCachedTime;
    // 活动边框参加 指定商品参加上限
    private Integer selectProductLimit;

    // 不过滤任何数据的url配置
    private List<String> ignored;


    @Override
    public String toString() {
        return "CrmebConfig{" +
                "version='" + version + '\'' +
                ", domain='" + domain + '\'' +
                ", wechatApiUrl='" + wechatApiUrl + '\'' +
                ", asyncConfig=" + asyncConfig +
                ", imagePath='" + imagePath + '\'' +
                ", activityStyleCachedTime=" + activityStyleCachedTime +
                ", selectProductLimit=" + selectProductLimit +
                ", ignored=" + ignored +
                '}';
    }
}
