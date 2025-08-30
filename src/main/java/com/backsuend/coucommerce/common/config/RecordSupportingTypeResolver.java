package com.backsuend.coucommerce.common.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Jackson ObjectMapper가 Java Record 타입을 직렬화/역직렬화할 때,
 * 기본적으로 타입 정보를 포함하지 않아 역직렬화 시 LinkedHashMap 등으로 변환되는 문제를 해결한다.
 * 이 커스텀 TypeResolver는 Record 타입에 대해 강제로 `@class`와 같은 타입 정보를 JSON에 포함시켜
 * 올바른 객체로 역직렬화될 수 있도록 지원한다.
 */
public class RecordSupportingTypeResolver extends DefaultTypeResolverBuilder {

	public RecordSupportingTypeResolver(DefaultTyping t, PolymorphicTypeValidator ptv) {
		super(t, ptv);
	}

	@Override
	public boolean useForType(JavaType t) {
		// record 타입일 경우 강제로 타입 정보를 포함하도록 true 반환
		if (t.isRecordType()) {
			return true;
		}
		return super.useForType(t);
	}
}
