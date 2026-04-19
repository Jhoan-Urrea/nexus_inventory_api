package com.example.nexus.config;

import com.example.nexus.modules.auth.security.AuthAccessDeniedHandler;
import com.example.nexus.modules.auth.security.AuthAuthenticationEntryPoint;
import com.example.nexus.modules.auth.security.CustomUserDetailsService;
import com.example.nexus.modules.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/activate-account",
            "/api/auth/resend-activation",
            "/api/auth/password/forgot",
            "/api/auth/password/verify",
            "/api/auth/password/reset"
    };

    /**
     * Public POST endpoints that must stay reachable without CSRF bootstrap.
     * Do not add /api/auth/password/change here because it remains authenticated.
     */
    private static final String[] CSRF_IGNORED_AUTH_ENDPOINTS = PUBLIC_AUTH_ENDPOINTS;

    /** Stripe envia POST sin JWT ni cookie de sesion; la firma va en {@code Stripe-Signature}. */
    private static final String STRIPE_WEBHOOK_PATH = "/api/payments/webhook/stripe";

    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final AuthAuthenticationEntryPoint authAuthenticationEntryPoint;
    private final AuthAccessDeniedHandler authAccessDeniedHandler;
    private final AppSecurityProperties appSecurityProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(appSecurityProperties.getCsrfCookieName());
        repository.setHeaderName(appSecurityProperties.getCsrfHeaderName());
        repository.setCookiePath(appSecurityProperties.getCsrfCookiePath());
        repository.setCookieCustomizer(builder -> builder
                .httpOnly(appSecurityProperties.isCsrfCookieHttpOnly())
                .path(appSecurityProperties.getCsrfCookiePath())
                .sameSite(appSecurityProperties.getCsrfCookieSameSite())
                .secure(appSecurityProperties.isCsrfCookieSecure()));
        return repository;
    }

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new SpaCsrfTokenRequestHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HttpSessionSecurityContextRepository securityContextRepository
    ) throws Exception {

        if (appSecurityProperties.isCsrfEnabled()) {
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(cookieCsrfTokenRepository())
                    .csrfTokenRequestHandler(csrfTokenRequestHandler())
                    .ignoringRequestMatchers(
                            Stream.concat(Arrays.stream(CSRF_IGNORED_AUTH_ENDPOINTS), Stream.of(STRIPE_WEBHOOK_PATH))
                                    .toArray(String[]::new)));
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/api/health", "/error").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/csrf").permitAll();
                    auth.requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.POST, STRIPE_WEBHOOK_PATH).permitAll();
                    auth.requestMatchers("/api/auth/**").authenticated();

                    if (appSecurityProperties.isPermitPublicLocations()) {
                        auth.requestMatchers("/api/locations/**").permitAll();
                    }
                    if (appSecurityProperties.isPermitSwaggerDocumentation()) {
                        auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                    } else {
                        auth.requestMatchers(SWAGGER_ENDPOINTS).hasRole("ADMIN");
                    }

                    if (appSecurityProperties.isActuatorAdminOnly()) {
                        if (appSecurityProperties.isActuatorHealthPublic()) {
                            auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                        }
                        auth.requestMatchers("/actuator/**").hasRole("ADMIN");
                    } else {
                        auth.requestMatchers("/actuator/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authAuthenticationEntryPoint)
                        .accessDeniedHandler(authAccessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
