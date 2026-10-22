package com.lias.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.GrantedAuthority;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final com.lias.manager.repository.UserRepository userRepository;

    public SecurityConfig(com.lias.manager.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Accès public et assets
                .requestMatchers("/", "/home", "/teams", "/members", "/events", "/calendar", "/calendar/**", "/publications", "/public-events", "/public-publications", "/profile/photo/**", "/uploads/**", "/login", "/register", "/adhesions/apply", "/adhesion-apply", "/adhesion-status", "/adhesion/status", "/css/**", "/js/**", "/images/**", "/search").permitAll()
                
                // Doctorant
                .requestMatchers("/doctorant/**").hasRole("DOCTORANT")
                
                // Member equipment accessed by members only
                .requestMatchers("/member/equipment", "/member/equipment/**").hasRole("MEMBER")
                
                // Member conventions read-only access
                .requestMatchers("/member/conventions", "/member/conventions/**").hasAnyRole("MEMBER", "ASSOCIATE", "ADMIN", "DIRECTOR")
                
                // Member documents read-only access
                .requestMatchers("/member/documents", "/member/documents/**").hasAnyRole("MEMBER", "DOCTORANT", "ASSOCIATE", "DIRECTOR")
                
                // Member PV read-only access
                .requestMatchers("/member/pv", "/member/pv/**").hasAnyRole("MEMBER", "DOCTORANT", "ASSOCIATE", "DIRECTOR")
                
                // Internal Communication
                .requestMatchers("/communication", "/communication/**").hasAnyRole("MEMBER", "DOCTORANT", "ASSOCIATE", "DIRECTOR", "ADMIN")
                
                // Permanent Member
                .requestMatchers("/member/**").hasRole("MEMBER")
                
                // Associate Member
                .requestMatchers("/associate/**").hasRole("ASSOCIATE")
                
                // Director
                .requestMatchers("/director/**").hasRole("DIRECTOR")
                
                // Admin
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        http.addFilterAfter(new jakarta.servlet.Filter() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, jakarta.servlet.FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {
                jakarta.servlet.http.HttpServletRequest req = (jakarta.servlet.http.HttpServletRequest) request;
                jakarta.servlet.http.HttpServletResponse res = (jakarta.servlet.http.HttpServletResponse) response;
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    String uri = req.getRequestURI();
                    if (!uri.equals("/change-password") && !uri.startsWith("/css/") && !uri.startsWith("/js/") && !uri.startsWith("/images/") && !uri.equals("/logout") && !uri.equals("/login")) {
                        String username = auth.getName();
                        var userOpt = userRepository.findByUsername(username)
                                .or(() -> userRepository.findByEmail(username));
                        if (userOpt.isPresent() && userOpt.get().isPasswordNeedChange()) {
                            res.sendRedirect("/change-password");
                            return;
                        }
                    }
                }
                chain.doFilter(request, response);
            }
        }, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            var userOpt = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username));
            if (userOpt.isPresent() && userOpt.get().isPasswordNeedChange()) {
                response.sendRedirect("/change-password");
                return;
            }

            var roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            
            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard");
            } else if (roles.contains("ROLE_DIRECTOR")) {
                response.sendRedirect("/director/dashboard");
            } else if (roles.contains("ROLE_MEMBER")) {
                response.sendRedirect("/member/dashboard");
            } else if (roles.contains("ROLE_ASSOCIATE")) {
                response.sendRedirect("/associate/dashboard");
            } else if (roles.contains("ROLE_DOCTORANT")) {
                response.sendRedirect("/doctorant/dashboard");
            } else {
                response.sendRedirect("/home");
            }
        };
    }
}
