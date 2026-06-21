package com.jsy.crmeb.modern.admin;

import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.jsy.crmeb.modern")
@EnableConfigurationProperties(CrmebRuntimeProperties.class)
@MapperScan("com.jsy.crmeb.modern.service.**.mapper")
public class CrmebModernAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrmebModernAdminApplication.class, args);
    }
}
