package com.backsuend.coucommerce.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
	Optional<Address> findByMemberId(Long memberId);

	Optional<Address> findByMember(Member member);
}
