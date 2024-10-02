package com.taesan.tikkle.domain.account.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taesan.tikkle.domain.account.dto.response.AccountResponse;
import com.taesan.tikkle.domain.account.service.AccountService;
import com.taesan.tikkle.global.annotations.AuthedUsername;
import com.taesan.tikkle.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@GetMapping
	public ResponseEntity<ApiResponse<AccountResponse>> getAccountInfo(
		@AuthedUsername UUID username) {
		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.success("계좌 조회에 성공했습니다.", AccountResponse.from(accountService.fetchAccount(username))));
	}
}
