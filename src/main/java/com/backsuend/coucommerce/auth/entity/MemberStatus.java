package com.backsuend.coucommerce.auth.entity;

public enum MemberStatus {
	ACTIVE, // 활성
	PENDING_VERIFICATION, // 이메일 인증 대기
	LOCKED, // 잠김
	DORMANT // 휴면
}
