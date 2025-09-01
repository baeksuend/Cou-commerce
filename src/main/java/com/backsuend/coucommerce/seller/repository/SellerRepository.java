package com.backsuend.coucommerce.seller.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.seller.entity.Seller;
import com.backsuend.coucommerce.seller.entity.SellerStatus;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
	Optional<Seller> findByMember(Member member);

	boolean existsByMember(Member member);

	List<Seller> findByStatus(SellerStatus status);
}
