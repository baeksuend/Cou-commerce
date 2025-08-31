package com.backsuend.coucommerce.auth.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author rua
 */
public enum Role {
	BUYER,
	SELLER,
	ADMIN;

	//********* 권한 문제로 수정 / 권한 앞에 ROLE_ 붙음 ******
	@JsonCreator
	public static Role from(String value) {
		return Role.valueOf(value.replace("ROLE_", ""));
	}

	@JsonValue
	public String toValue() {
		return "ROLE_" + this.name();
	}
	//****************************************************
}