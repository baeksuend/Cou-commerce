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
}