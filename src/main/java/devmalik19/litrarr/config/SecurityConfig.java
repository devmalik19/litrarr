package devmalik19.litrarr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig
{
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, Environment env) throws Exception
    {
        if (isSecurityEnabled(env))
        {
            httpSecurity.authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/images/**", "/css/**", "/js/**", "/cache/**").permitAll()
                    .anyRequest().authenticated()
            ).formLogin(Customizer.withDefaults());
        }
        else
        {
            httpSecurity.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return httpSecurity.build();
    }

    private boolean isSecurityEnabled(Environment env)
    {
        String user = env.getProperty("APP_USER");
        String password = env.getProperty("APP_PASSWORD");
        return !"admin".equals(user) || !"admin".equals(password);
    }
}
