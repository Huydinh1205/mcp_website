package com.marketplace.web;

import com.marketplace.AppProps;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final AppProps props;

  public WebConfig(AppProps props) {
    this.props = props;
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(props.corsOrigin() == null ? "http://localhost:3000" : props.corsOrigin())
        .allowedMethods("GET", "POST", "OPTIONS");
  }
}
