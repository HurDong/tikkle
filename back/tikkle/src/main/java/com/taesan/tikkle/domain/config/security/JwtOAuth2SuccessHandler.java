package com.taesan.tikkle.domain.config.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taesan.tikkle.domain.member.service.RedisTokenService;
import com.taesan.tikkle.global.utils.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(JwtOAuth2SuccessHandler.class);
	private final JwtUtil jwtUtil;
	private final RedisTokenService redisTokenService;
	private final ObjectMapper objectMapper;

	@Value("${tikkle.redirect}")
	private String redirectUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();

		logger.debug("authenticaitonHandler 도달");
		UUID memberId = oAuth2User.getAttribute("memberId");

		Map<String, Object> claims = new HashMap<>();

		String jwt = jwtUtil.generateToken(claims, memberId.toString());
		String refreshToken = jwtUtil.generateRefreshToken(claims, memberId.toString());
		/*
			FIXME: 배포 환경에서는 setSecure를 true로 바꿔줘야 함.
		 */
		ResponseCookie jwtCookie
			= ResponseCookie.from("Authorization", "Bearer+" + jwt)
			.maxAge(60 * 60)
			.path("/")
			.httpOnly(false)
			.secure(false)
			.build();

		ResponseCookie refreshTokenCookie
			= ResponseCookie.from("refresh_token", refreshToken)
			.maxAge(60 * 60 * 24)
			.path("/")
			.httpOnly(false)
			.secure(false)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

		redisTokenService.storeRefreshToken(memberId, refreshToken, 1000 * 60 * 60 * 24);

		getRedirectStrategy().sendRedirect(request, response, redirectUrl + "/home");

		//		MemberResponse memberResponse = new MemberResponse(
		//			oAuth2User.getAttribute("memberId"),
		//			oAuth2User.getAttribute("username"),
		//			oAuth2User.getAttribute("nickname"),
		//			oAuth2User.getAttribute("email")
		//		);
		//
		//		// ApiResponse 객체 생성
		//		ApiResponse<MemberResponse> apiResponse
		//			= new ApiResponse<>(200, "회원 정보 불러오기에 성공했습니다.", memberResponse);
		//
		//		// 응답 헤더 설정
		//		response.setContentType("application/json");
		//		response.setCharacterEncoding("UTF-8");
		//		response.setStatus(HttpServletResponse.SC_OK);
		//
		//		// ObjectMapper를 사용해 ApiResponse 객체를 JSON으로 변환하여 응답으로 전송
		//		objectMapper.writeValue(response.getWriter(), apiResponse);
	}

}
