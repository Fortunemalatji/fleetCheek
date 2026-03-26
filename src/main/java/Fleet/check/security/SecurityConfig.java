package Fleet.check.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                
                // Inspections - Lookups readable by all authenticated
                .requestMatchers(HttpMethod.GET, "/api/inspections/checklist/templates").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/inspections/checklist/analytics", "/api/inspections/checklist/report", "/api/inspections/*/report")
                .hasAnyRole("DRIVER","SUPERVISOR","ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/inspections/*/defects", "/api/inspections/defects/open")
                .hasAnyRole("SUPERVISOR","ADMIN")
                
                // Lookups readable by all authenticated
                .requestMatchers(HttpMethod.GET, "/api/lookups/**").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                
                // Vehicles & Shipments readable by all authenticated
                .requestMatchers(HttpMethod.GET, "/api/vehicles/**", "/api/shipments/**").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                
                // Inspection end + override requires DRIVER, SUPERVISOR, SECURITY, or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/inspections/*/end").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                
                // Write operations (POST/PUT) for entities require ADMIN (except starting inspection/submitting checklist)
                .requestMatchers(HttpMethod.POST, "/api/inspections/start").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/inspections/*/checklist").hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/inspections/*/checklist/*/start", "/api/inspections/*/checklist/*/complete")
                .hasAnyRole("DRIVER","SUPERVISOR","SECURITY","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/inspections/*/checklist/*/resolve")
                .hasAnyRole("DRIVER","SUPERVISOR","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/**", "/api/vehicles/**", "/api/shipments/**").hasAnyRole("SUPERVISOR","ADMIN")
                .requestMatchers(HttpMethod.PUT, "/**").hasRole("ADMIN")
                
                // Delete requires ADMIN
                .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
