package com.taesan.tikkle.domain.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.taesan.tikkle.global.resolver.AuthedUsernameArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${file.upload.image-dir}")
	private String imageUploadDir;

	private final AuthedUsernameArgumentResolver currentUserIdArgumentResolver;

	public WebConfig(AuthedUsernameArgumentResolver currentUserIdArgumentResolver) {
		this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserIdArgumentResolver);
	}

	/*
		NOTE: 컨트롤러를 통한 이미지 처리를 위한 주석 처리
	 */

	// @Override
	// public void addResourceHandlers(ResourceHandlerRegistry registry) {
	// 	registry.addResourceHandler("/images/**")
	// 		.addResourceLocations("file:" + imageUploadDir + "/");
	// }

}
