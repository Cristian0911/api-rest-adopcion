package com.codigofacilito.adopcion.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final String USER = "USER";
    private static final String ADMIN = "ADMIN";

    /**
     * Consola H2, solo cuando está habilitada (perfil dev). Requiere ADMIN. Es una aplicación web
     * con formularios e iframes propios, por eso se desactiva CSRF y se permiten frames del mismo
     * origen únicamente para estas rutas.
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        return http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ADMIN))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .httpBasic(basic -> basic.authenticationEntryPoint(errorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }

    /**
     * API REST sin estado: cada solicitud envía sus credenciales Basic y no se usan cookies de
     * sesión, por lo que no hay sesión que un sitio externo pueda aprovechar y CSRF no aplica.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mascotas").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/mascotas/*").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/mascotas/*", "/api/usuarios/*").hasRole(ADMIN)
                        .requestMatchers("/api/**").hasAnyRole(USER, ADMIN)
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(errorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(SecurityProperties properties, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(properties.user().name())
                        .password(encoder.encode(properties.user().password()))
                        .roles(USER)
                        .build(),
                User.withUsername(properties.admin().name())
                        .password(encoder.encode(properties.admin().password()))
                        .roles(USER, ADMIN)
                        .build());
    }
}
