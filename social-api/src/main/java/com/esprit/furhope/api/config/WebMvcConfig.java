package com.esprit.furhope.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiRequestLoggingInterceptor apiRequestLoggingInterceptor;

    public WebMvcConfig(ApiRequestLoggingInterceptor apiRequestLoggingInterceptor) {
        this.apiRequestLoggingInterceptor = apiRequestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiRequestLoggingInterceptor).addPathPatterns("/api/**");
    }
}
