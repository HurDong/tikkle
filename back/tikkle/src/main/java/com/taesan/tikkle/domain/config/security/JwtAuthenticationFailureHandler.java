package com.taesan.tikkle.domain.config.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFailureHandler implements AuthenticationFailureHandler {
	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFailureHandler.class);

	@Value("${tikkle.redirect}")
	private String redirectUrl;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception) throws IOException, ServletException {
		logger.error("Authentication failed: {}", exception.getMessage(), exception);
		response.sendRedirect(redirectUrl);
	}

}
