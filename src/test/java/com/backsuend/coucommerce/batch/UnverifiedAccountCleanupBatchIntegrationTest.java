package com.backsuend.coucommerce.batch;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.batch.core.BatchStatus.*;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.backsuend.coucommerce.auth.entity.Address;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.backsuend.coucommerce.member.repository.AddressRepository; // Assuming AddressRepository exists

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UnverifiedAccountCleanupBatch 통합 테스트")
class UnverifiedAccountCleanupBatchIntegrationTest {

	@Autowired
	private PlatformTransactionManager transactionManager;

	private TransactionTemplate transactionTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private AddressRepository addressRepository; // Inject AddressRepository
	@Autowired
	@Qualifier("unverifiedAccountCleanupJob")
	private Job unverifiedAccountCleanupJob;
	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@AfterEach
	void tearDown() {
		addressRepository.deleteAll(); // Delete addresses first due to foreign key
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("미인증 계정 정리 배치잡 실행 시, 특정 기간이 지난 PENDING_VERIFICATION 회원은 삭제되어야 한다.")
	void unverifiedAccountCleanupJob_shouldDeleteOldUnverifiedAccounts() throws Exception {
		// Given (전제)
		LocalDateTime now = LocalDateTime.now();
		log.info("Test started at: {}", now);

		// 삭제 대상: 13개월 전 가입, PENDING_VERIFICATION 상태
		Member memberToDelete = Member.builder()
			.email("delete.user@example.com")
			.password("password")
			.name("Delete User")
			.phone("010-1111-2222")
			.role(Role.BUYER)
			.status(MemberStatus.PENDING_VERIFICATION)
			.build();
		
		// 삭제 대상이 아님: 1개월 전 가입, PENDING_VERIFICATION 상태 (cleanupThreshold보다 최신)
		Member memberToKeepRecent = Member.builder()
			.email("keep.recent.user@example.com")
			.password("password")
			.name("Keep Recent User")
			.phone("010-3333-4444")
			.role(Role.BUYER)
			.status(MemberStatus.PENDING_VERIFICATION)
			.build();

		// 삭제 대상이 아님: 13개월 전 가입, ACTIVE 상태
		Member memberToKeepActive = Member.builder()
			.email("keep.active.user@example.com")
			.password("password")
			.name("Keep Active User")
			.phone("010-5555-6666")
			.role(Role.BUYER)
			.status(MemberStatus.ACTIVE)
			.build();

		memberRepository.saveAll(List.of(memberToDelete, memberToKeepRecent, memberToKeepActive));

		// Address 생성 및 저장
		Address addressToDelete = Address.builder()
			.member(memberToDelete)
			.postalCode("12345")
			.roadName("Delete Road")
			.detail("Delete Detail")
			.build();
		Address addressToKeepRecent = Address.builder()
			.member(memberToKeepRecent)
			.postalCode("67890")
			.roadName("Keep Road Recent")
			.detail("Keep Detail Recent")
			.build();
		Address addressToKeepActive = Address.builder()
			.member(memberToKeepActive)
			.postalCode("10101")
			.roadName("Keep Road Active")
			.detail("Keep Detail Active")
			.build();
		addressRepository.saveAll(List.of(addressToDelete, addressToKeepRecent, addressToKeepActive));


		// 트랜잭션 내에서 flush, clear 처리 및 createdAt 수동 설정
		transactionTemplate.execute(status -> {
			memberRepository.flush();
			addressRepository.flush();

			// DB에서 다시 로드하여 createdAt을 수동으로 설정
			Member loadedMemberToDelete = memberRepository.findByEmail("delete.user@example.com").orElseThrow();
			Member loadedMemberToKeepRecent = memberRepository.findByEmail("keep.recent.user@example.com").orElseThrow();
			Member loadedMemberToKeepActive = memberRepository.findByEmail("keep.active.user@example.com").orElseThrow();

			setCreatedAtUsingReflection(loadedMemberToDelete, now.minusMonths(13));
			setCreatedAtUsingReflection(loadedMemberToKeepRecent, now.minusMonths(1)); // cleanupThreshold보다 최신
			setCreatedAtUsingReflection(loadedMemberToKeepActive, now.minusMonths(13));

			entityManager.merge(loadedMemberToDelete);
			entityManager.merge(loadedMemberToKeepRecent);
			entityManager.merge(loadedMemberToKeepActive);

			entityManager.flush(); // 변경사항 DB에 반영
			entityManager.clear(); // 영속성 컨텍스트 초기화
			log.info("EntityManager flushed and cleared after saving initial members/addresses and setting createdAt.");
			return null;
		});
		
		log.info("Member to delete: email={}, status={}, createdAt={}",
			memberRepository.findByEmail("delete.user@example.com").orElseThrow().getEmail(),
			memberRepository.findByEmail("delete.user@example.com").orElseThrow().getStatus(),
			memberRepository.findByEmail("delete.user@example.com").orElseThrow().getCreatedAt());
		log.info("Member to keep recent: email={}, status={}, createdAt={}",
			memberRepository.findByEmail("keep.recent.user@example.com").orElseThrow().getEmail(),
			memberRepository.findByEmail("keep.recent.user@example.com").orElseThrow().getStatus(),
			memberRepository.findByEmail("keep.recent.user@example.com").orElseThrow().getCreatedAt());
		log.info("Member to keep active: email={}, status={}, createdAt={}",
			memberRepository.findByEmail("keep.active.user@example.com").orElseThrow().getEmail(),
			memberRepository.findByEmail("keep.active.user@example.com").orElseThrow().getStatus(),
			memberRepository.findByEmail("keep.active.user@example.com").orElseThrow().getCreatedAt());


		// When (실행)
		LocalDateTime cleanupThreshold = now.minusMonths(6); // 6개월 이상된 미인증 계정 삭제
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("cleanupThreshold", cleanupThreshold.toString())
			.toJobParameters();
		log.info("Job parameters prepared: cleanupThreshold={}", jobParameters.getString("cleanupThreshold"));

		jobLauncherTestUtils.setJob(this.unverifiedAccountCleanupJob);
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
		log.info("Job execution completed with status: {}", jobExecution.getStatus());

		// 배치 실행 후 flush, clear 다시 수행
		transactionTemplate.execute(status -> {
			entityManager.flush();
			entityManager.clear();
			log.info("EntityManager flushed and cleared after job execution.");
			return null;
		});

		// Then (검증)
		assertThat(jobExecution.getStatus()).isEqualTo(COMPLETED);

		// 삭제 대상 회원이 삭제되었는지 확인
		assertThat(memberRepository.findByEmail("delete.user@example.com")).isEmpty();
		// 삭제 대상 회원의 주소도 삭제되었는지 확인
		assertThat(addressRepository.findByMemberId(memberToDelete.getId())).isEmpty(); // Assuming findByMemberId exists

		// 유지 대상 회원이 남아있는지 확인
		assertThat(memberRepository.findByEmail("keep.recent.user@example.com")).isPresent();
		assertThat(memberRepository.findByEmail("keep.active.user@example.com")).isPresent();
		// 유지 대상 회원의 주소도 남아있는지 확인
		assertThat(addressRepository.findByMemberId(memberToKeepRecent.getId())).isPresent();
		assertThat(addressRepository.findByMemberId(memberToKeepActive.getId())).isPresent();
	}

	private void setCreatedAtUsingReflection(Member member, LocalDateTime createdAt) {
		try {
			java.lang.reflect.Field field = com.backsuend.coucommerce.common.entity.BaseTimeEntity.class.getDeclaredField(
				"createdAt");
			field.setAccessible(true);
			field.set(member, createdAt);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.error("Failed to set createdAt using reflection for member: {}", member.getEmail(), e);
			throw new RuntimeException("Failed to set createdAt using reflection", e);
		}
	}

	@TestConfiguration
	static class BatchTestConfig {
		@Bean
		public JobLauncherTestUtils jobLauncherTestUtils() {
			return new JobLauncherTestUtils();
		}
	}
}
