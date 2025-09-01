package com.backsuend.coucommerce.payment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.payment.entity.Payment;

/**
 * @author rua
 */

/**
 * Payment Repository
 * - 결제 엔티티 저장/조회
 * - 주문별, 회원별 결제 정보 조회 기능 제공
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/**
	 * 주문 ID로 결제 정보 조회
	 * @param orderId 주문 ID
	 * @return Payment 엔티티 (없으면 null)
	 */
	Payment findByOrderId(Long orderId);

	/**
	 * 회원 ID로 결제 내역 조회 (페이징)
	 * @param memberId 회원 ID
	 * @param pageable 페이징 정보
	 * @return Page<Payment>
	 */
	@Query("SELECT p FROM Payment p WHERE p.order.member.id = :memberId ORDER BY p.createdAt DESC")
	Page<Payment> findByOrderMemberId(@Param("memberId") Long memberId, Pageable pageable);

	/**
	 * 결제 상태별 조회 (관리자용)
	 * @param status 결제 상태
	 * @param pageable 페이징 정보
	 * @return Page<Payment>
	 */
	Page<Payment> findByStatus(com.backsuend.coucommerce.payment.entity.PaymentStatus status, Pageable pageable);
}