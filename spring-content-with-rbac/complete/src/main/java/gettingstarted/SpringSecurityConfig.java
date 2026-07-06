package gettingstarted;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SpringSecurityConfig {

	@SuppressWarnings("deprecation")
	@Bean
	public UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager(
				User.withDefaultPasswordEncoder().username("paul").password("warren").roles("READER", "AUTHOR").build(),
				User.withDefaultPasswordEncoder().username("eric").password("wimp").roles("READER").build());
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(pathPattern(HttpMethod.GET, "/files/*/content")).hasRole("READER")
						.requestMatchers(pathPattern(HttpMethod.PUT, "/files/*/content")).hasRole("AUTHOR")
						.requestMatchers(pathPattern(HttpMethod.DELETE, "/files/*/content")).hasRole("AUTHOR")
						.anyRequest().permitAll())
				.httpBasic(Customizer.withDefaults())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}
}
