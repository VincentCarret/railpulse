package altn72.railpulse.config;

import altn72.railpulse.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder()));
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(AuthService authService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(authService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsPasswordService(authService);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/incidents/*/modifier")
                        .hasRole("RESPONSABLE_EXPLOITATION")
                        .requestMatchers(HttpMethod.GET, "/incidents/*/affecter")
                        .hasRole("RESPONSABLE_EXPLOITATION")
                        .requestMatchers(HttpMethod.POST, "/incidents/*/affecter")
                        .hasRole("RESPONSABLE_EXPLOITATION")
                        .requestMatchers(HttpMethod.POST, "/incidents/*")
                        .hasRole("RESPONSABLE_EXPLOITATION")
                        .requestMatchers(HttpMethod.GET, "/stations", "/stations/**", "/equipements/**", "/incidents/**")
                        .hasAnyRole("AGENT", "RESPONSABLE_EXPLOITATION")
                        .requestMatchers(HttpMethod.POST, "/equipements/*/incidents")
                        .hasAnyRole("AGENT", "RESPONSABLE_EXPLOITATION")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login")
                        .failureUrl("/login?error")
                        .defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .build();
    }
}
