package com.jsy.crmeb.modern.front.config;

import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontWebConfiguration implements WebMvcConfigurer {
    private final CrmebRuntimeProperties runtimeProperties;

    public FrontWebConfiguration(CrmebRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authori-zation");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (runtimeProperties.getImagePath() != null && !runtimeProperties.getImagePath().isBlank()) {
            String uploadLocation = Path.of(runtimeProperties.getImagePath()).toAbsolutePath().normalize().toUri().toString();
            registry.addResourceHandler("/crmebimage/**").addResourceLocations(uploadLocation + "/crmebimage/");
            registry.addResourceHandler("/public/**").addResourceLocations(uploadLocation + "/public/");
        }
    }
}
