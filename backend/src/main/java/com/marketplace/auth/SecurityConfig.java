package com.marketplace.auth;

import com.marketplace.AppProps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter, AppProps props)
      throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsSource(props)))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories").permitAll()
            .requestMatchers("/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(
            (req, res, ex) -> res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED)))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private CorsConfigurationSource corsSource(AppProps props) {
    CorsConfiguration c = new CorsConfiguration();
    String origin = props.corsOrigin() == null || props.corsOrigin().isBlank()
        ? "http://localhost:3000" : props.corsOrigin().trim();
    if (origin.contains("*")) {
      // dev / LAN / tunnel access. JWT lives in the Authorization header, no
      // cookies, so a wildcard origin is safe here.
      c.setAllowedOriginPatterns(java.util.List.of("*"));
    } else {
      c.setAllowedOrigins(java.util.Arrays.stream(origin.split(","))
          .map(String::trim).filter(s -> !s.isEmpty()).toList());
    }
    c.setAllowedMethods(java.util.List.of("GET", "POST", "OPTIONS"));
    c.setAllowedHeaders(java.util.List.of("*"));
    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/api/**", c);
    return src;
  }
}
