package devmalik19.litrarr.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig
{
    @Bean
    @ConditionalOnBooleanProperty(value = "DISABLE_SECURITY", havingValue = true)
    public SecurityFilterChain disableSecurity(HttpSecurity httpSecurity)
    {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth->auth.anyRequest().permitAll());
        return httpSecurity.build();
    }

	@Bean
	@ConditionalOnBooleanProperty(value = "DISABLE_SECURITY", havingValue = false)
	public SecurityFilterChain disableSecurityForImages(HttpSecurity httpSecurity)
	{
		httpSecurity.authorizeHttpRequests(auth->
			auth
				.requestMatchers("/images/logos/**").permitAll()
				.anyRequest().authenticated()
		).formLogin(Customizer.withDefaults());
		return httpSecurity.build();
	}
}
