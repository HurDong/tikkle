package com.taesan.tikkle.domain.member.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.taesan.tikkle.domain.config.security.CustomUserDetails;
import com.taesan.tikkle.domain.member.entity.Member;
import com.taesan.tikkle.domain.member.repository.MemberRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetails.class);
	private final MemberRepository memberRepository;  // 사용자 정보를 로드할 Repository

	public CustomUserDetailsService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		logger.debug("CustomUserDetiailsService 도달");

		UUID memberId;
		try {
			memberId = UUID.fromString(username);
		} catch (IllegalArgumentException e) {
			throw new UsernameNotFoundException("Invalid UUID format: " + username);
		}

		// Repository에서 UUID로 사용자 정보 조회
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new UsernameNotFoundException("User not found with UUID: " + memberId));

		GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(member.getRole().toString());

		// 조회된 사용자를 CustomUserDetails로 변환하여 반환
		return new CustomUserDetails(member.getId(), List.of(grantedAuthority));
	}
}
