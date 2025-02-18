package com.taesan.tikkle.domain.member.entity;

public enum Role {
    ROLE_USER("ROLE_USER"),
    ROLE_ADMIN("ROLE_ADMIN");

    final String role;

    Role(String role) {
        this.role = role;
    }
}
