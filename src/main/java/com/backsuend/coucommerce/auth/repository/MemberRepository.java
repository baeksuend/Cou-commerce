package com.backsuend.coucommerce.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backsuend.coucommerce.auth.entity.Member;

/**
 * @author rua
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
