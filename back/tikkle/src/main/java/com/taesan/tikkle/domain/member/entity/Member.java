package com.taesan.tikkle.domain.member.entity;

import java.util.UUID;

import com.github.f4b6a3.ulid.UlidCreator;
import com.taesan.tikkle.domain.organization.entity.Organization;
import com.taesan.tikkle.global.entity.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/*
TODO: Mattermost OAuth 관련 정보 추가 필요
 */
public class Member extends AuditableEntity {

	@Id
	@Column(columnDefinition = "BINARY(16)")
	private UUID id = UlidCreator.getMonotonicUlid().toUuid();

	@ManyToOne
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	// @OneToOne(mappedBy = "member")
	// private Account account;

	@Column(length = 64)
	private String name;

	@Column(length = 64)
	private String nickname;

	@Column(length = 128)
	private String email;

	@Builder
	public Member(Organization organization,
		// Account account,
		String name,
		String nickname,
		String email) {

		this.organization = organization;
		// this.account = account;
		this.name = name;
		this.nickname = nickname;
		this.email = email;
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	public void changeEmail(String email) {
		this.email = email;
	}

}
