package com.listitup.api.config;

import com.listitup.api.security.SetupInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SetupInterceptor setupInterceptor;

    public WebMvcConfig(SetupInterceptor setupInterceptor) {
        this.setupInterceptor = setupInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(setupInterceptor);
    }
}
