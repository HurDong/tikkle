package com.taesan.tikkle.domain.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taesan.tikkle.domain.config.security.CustomLogoutFilter;
import com.taesan.tikkle.domain.config.security.JwtAuthenticationFailureHandler;
import com.taesan.tikkle.domain.config.security.JwtAuthenticationFilter;
import com.taesan.tikkle.domain.config.security.JwtOAuth2SuccessHandler;
import com.taesan.tikkle.domain.member.service.CustomOAuth2UserService;
import com.taesan.tikkle.domain.member.service.CustomUserDetailsService;
import com.taesan.tikkle.domain.member.service.RedisTokenService;
import com.taesan.tikkle.global.utils.JwtUtil;

/**
 * TODO: 클라이언트-서버 API 연동 시 사후적 수정 필요
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final JwtOAuth2SuccessHandler jwtOAuth2SuccessHandler;
	private final JwtUtil jwtUtil;
	private final CustomUserDetailsService customUserDetailsService;
	private final RedisTokenService redisTokenService;
	private final ObjectMapper objectMapper;
	private final JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler;

	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
		JwtOAuth2SuccessHandler jwtOAuth2SuccessHandler,
		JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService, RedisTokenService redisTokenService,
		ObjectMapper objectMapper, JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler) {
		this.customOAuth2UserService = customOAuth2UserService;
		this.jwtOAuth2SuccessHandler = jwtOAuth2SuccessHandler;
		this.jwtUtil = jwtUtil;
		this.customUserDetailsService = customUserDetailsService;
		this.redisTokenService = redisTokenService;
		this.objectMapper = objectMapper;
		this.jwtAuthenticationFailureHandler = jwtAuthenticationFailureHandler;
	}

	/*
		NOTE: 내부 Thymeleaf 테스트를 위한 코드
	 */
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return web -> {
			web.ignoring()
				.requestMatchers("/favicon.ico", "/error", "/"); // 필터를 타면 안되는 경로
		};
	}

	@Bean
	static RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
				.role("ADMIN").implies("USER")
				.build();
	}


	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		final String[] allowedOrigins = {"http://localhost:5500", "http://localhost:3000",
			"http://127.0.0.1:5500", "http://127.0.0.1:3000", "https://j11a501.p.ssafy.io",
			"http://localhost:9092", "http://127.0.0.1:9092",
			"http://localhost:8065", "http://127.0.0.1:8065"};

		final String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
		configuration.setAllowedMethods(Arrays.asList(allowedMethods));
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(Arrays.asList("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
		CorsConfigurationSource corsConfigurationSource) throws Exception {

		http
			.cors((cors) -> cors.configurationSource(corsConfigurationSource))
			.csrf((csrf) -> csrf.disable());

		http
			.formLogin((auth) -> auth.disable());

		// TODO: 개발 환경을 위한 패턴이므로 추후 작성 필요
//		String[] requestMatcherPatterns = new String[] {
//			"/api/v1/**", "/ws/**", "/oauth2/**", "/", "/login/**", "/static/**", "/home"
//		};

		String[] requestMatcherPatterns = new String[] {
				"/api/v1/login", "/oauth2/**", "/login/**", "/static/**"
		};

		http
			.authorizeHttpRequests((auth) -> auth
				.requestMatchers(requestMatcherPatterns).permitAll()
				//				.requestMatchers("/api/v1/manager").hasRole("MANAGER")
				.anyRequest().authenticated())
			.oauth2Login(oauth2 ->
				oauth2
					// .loginPage("/api/v1/login")
					.successHandler(jwtOAuth2SuccessHandler)
					.failureHandler(jwtAuthenticationFailureHandler)
					.authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorization"))
					.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
					.tokenEndpoint(tokenEndpoint -> tokenEndpoint
						.accessTokenResponseClient(accessTokenResponseClient())  // JWT 서명 검증 비활성화
					)
			)
			.logout(logout -> logout
				.logoutUrl("/api/v1/logout")
				.clearAuthentication(true)
				.invalidateHttpSession(true)
				.deleteCookies("Authorization", "refresh_token") // 쿠키 삭제
				.permitAll()
			)
			.cors((cors) -> cors.configurationSource(corsConfigurationSource))
			.csrf((csrf) -> csrf.disable());

		http
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http
			.addFilterBefore(new JwtAuthenticationFilter(jwtUtil,
					customOAuth2UserService, customUserDetailsService, redisTokenService, objectMapper),
				UsernamePasswordAuthenticationFilter.class)
			.addFilterAt(new CustomLogoutFilter(redisTokenService, jwtUtil), LogoutFilter.class);

		return http.build();
	}

	/*
		NOTE: Mattermost에서 JWK이 지원되지 않아 기본 검증 사용 (OIDC ID 대비)
	 */
	private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
		return new DefaultAuthorizationCodeTokenResponseClient();  // 기본 검증 사용
	}

}
