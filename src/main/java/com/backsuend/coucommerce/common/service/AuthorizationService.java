package com.backsuend.coucommerce.common.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.backsuend.coucommerce.auth.service.UserDetailsImpl;
import com.backsuend.coucommerce.common.exception.BusinessException;
import com.backsuend.coucommerce.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

	/**
	 * 현재 인증된 사용자가 관리자(ROLE_ADMIN) 권한을 가지고 있는지 확인하고, 없으면 예외를 발생시킵니다.
	 * @throws BusinessException 관리자 권한이 없는 경우
	 */
	public void authorizeAdmin() {
		if (!isAdmin()) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "관리자 권한이 필요합니다.");
		}
	}

	/**
	 * 현재 인증된 사용자가 특정 대상 사용자 ID와 일치하는지 또는 관리자 권한을 가지고 있는지 확인합니다.
	 * 일치하지 않거나 권한이 없는 경우 BusinessException을 발생시킵니다.
	 *
	 * @param targetUserId 작업 대상이 되는 사용자 ID
	 * @throws BusinessException 인증되지 않았거나 권한이 없는 경우
	 */
	public void authorizeCurrentUser(Long targetUserId) {
		UserDetailsImpl currentUser = getCurrentUser();
		Long currentUserId = currentUser.getId();

		boolean isAdmin = isAdmin(currentUser);

		if (!isAdmin && !currentUserId.equals(targetUserId)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 작업에 대한 권한이 없습니다.");
		}
	}

	/**
	 * 현재 인증된 사용자의 UserDetailsImpl 객체를 반환합니다.
	 * @return 현재 인증된 사용자의 UserDetailsImpl
	 * @throws BusinessException 인증되지 않은 사용자이거나 UserDetailsImpl 타입이 아닌 경우
	 */
	public UserDetailsImpl getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
			|| authentication.getPrincipal() instanceof String) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다.");
		}

		// UserDetailsImpl 타입으로 캐스팅하기 전에 instanceof 검사
		if (!(authentication.getPrincipal() instanceof UserDetailsImpl)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "인증된 사용자 정보가 올바르지 않습니다.");
		}

		return (UserDetailsImpl)authentication.getPrincipal();
	}

	/**
	 * 현재 인증된 사용자가 관리자(ROLE_ADMIN) 권한을 가지고 있는지 확인합니다.
	 * @return 관리자 권한 여부
	 */
	public boolean isAdmin() {
		UserDetailsImpl currentUser = getCurrentUser();
		return isAdmin(currentUser);
	}

	private boolean isAdmin(UserDetailsImpl userDetails) {
		return userDetails.getAuthorities().stream()
			.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}
}
