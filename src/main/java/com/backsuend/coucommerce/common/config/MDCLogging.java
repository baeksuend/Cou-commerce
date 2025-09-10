package com.backsuend.coucommerce.common.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;

public class MDCLogging {

	public static MDCContext withContext(String key, String value) {
		return new MDCContext(key, value);
	}

	public static MDCContext withContexts(Map<String, String> contextMap) {
		return new MDCContext(contextMap);
	}

	public static class MDCContext implements AutoCloseable {
		private final Map<String, String> oldValues = new HashMap<>();
		private final Map<String, String> newKeys = new HashMap<>();

		public MDCContext(String key, String value) {
			this(Map.of(key, value));
		}

		public MDCContext(Map<String, String> contextMap) {
			contextMap.forEach((key, value) -> {
				oldValues.put(key, MDC.get(key));  // 기존 값 저장
				MDC.put(key, value);
				newKeys.put(key, value);
			});
		}

		@Override
		public void close() {
			newKeys.keySet().forEach(key -> {
				if (oldValues.get(key) != null) {
					MDC.put(key, oldValues.get(key));
				} else {
					MDC.remove(key);
				}
			});
		}
	}
}
