package com.backsuend.coucommerce.sellerregistration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistration;
import com.backsuend.coucommerce.sellerregistration.entity.SellerRegistrationStatus;

@Repository
public interface SellerRegistrationRepository
	extends JpaRepository<SellerRegistration, Long>, SellerRegistrationRepositoryCustom {
	Optional<SellerRegistration> findByMember(Member member);

	boolean existsByMember(Member member);

	List<SellerRegistration> findByStatus(SellerRegistrationStatus status);
}
