package com.han.fakeNowcoder.config;

import com.han.fakeNowcoder.controller.interceptor.DataStatisticsInterceptor;
import com.han.fakeNowcoder.controller.interceptor.LoginTicketInterceptor;
import com.han.fakeNowcoder.controller.interceptor.MessageCountInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Autowired private LoginTicketInterceptor loginTicketInterceptor;

  //  @Autowired private LoginRequiredInterceptor loginRequiredInterceptor;

  @Autowired private MessageCountInterceptor messageCountInterceptor;

  @Autowired private DataStatisticsInterceptor dataStatisticsInterceptor;

  //  @Autowired private AlphaInterceptor alphaInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(loginTicketInterceptor)
        .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

    //    registry
    //        .addInterceptor(loginRequiredInterceptor)
    //        .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

    registry
        .addInterceptor(messageCountInterceptor)
        .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

    registry
        .addInterceptor(dataStatisticsInterceptor)
        .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
  }
}
