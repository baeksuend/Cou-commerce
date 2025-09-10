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

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.member.repository.MemberRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DormantUserBatch 통합 테스트")
class DormantUserBatchIntegrationTest {

	@Autowired
	private PlatformTransactionManager transactionManager;

	private TransactionTemplate transactionTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	@Qualifier("dormantUserJob")
	private Job dormantUserJob;
	@PersistenceContext
	private EntityManager entityManager; // 추가

	@BeforeEach
	void setUp() {
		transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("휴면 전환 배치잡 실행 시, 1년 이상 활동하지 않은 ACTIVE 회원은 DORMANT 상태로 변경되어야 한다.")
	void dormantUserJob_shouldChangeOldActiveUsersToDormant() throws Exception {
		// Given (전제)
		LocalDateTime now = LocalDateTime.now();
		log.info("Test started at: {}", now);

		// 휴면 전환 대상: 13개월 전 가입, 13개월 전 마지막 로그인
		Member memberToBecomeDormant = Member.builder()
			.email("dormant.user@example.com")
			.password("password")
			.name("Dormant User")
			.phone("010-1234-5678")
			.role(Role.BUYER)
			.status(MemberStatus.ACTIVE)
			.lastLoggedInAt(now.minusMonths(13))
			.build();
		log.info("Member to become dormant: email={}, status={}, createdAt={}, lastLoggedInAt={}",
			memberToBecomeDormant.getEmail(), memberToBecomeDormant.getStatus(),
			memberToBecomeDormant.getCreatedAt(), memberToBecomeDormant.getLastLoggedInAt());

		// 휴면 전환 대상이 아님: 13개월 전 가입, 1개월 전 마지막 로그인
		Member memberToStayActive = Member.builder()
			.email("active.user@example.com")
			.password("password")
			.name("Active User")
			.phone("010-8765-4321")
			.role(Role.BUYER)
			.status(MemberStatus.ACTIVE)
			.lastLoggedInAt(now.minusMonths(1))
			.build();
		log.info("Member to stay active: email={}, status={}, createdAt={}, lastLoggedInAt={}",
			memberToStayActive.getEmail(), memberToStayActive.getStatus(),
			memberToStayActive.getCreatedAt(), memberToStayActive.getLastLoggedInAt());

		memberRepository.saveAll(List.of(memberToBecomeDormant, memberToStayActive));

		// 트랜잭션 내에서 flush, clear 처리 및 createdAt 수동 설정
		transactionTemplate.execute(status -> {
			memberRepository.flush();

			// DB에서 다시 로드하여 createdAt을 수동으로 설정
			Member loadedDormantUser = memberRepository.findByEmail("dormant.user@example.com").orElseThrow();
			Member loadedActiveUser = memberRepository.findByEmail("active.user@example.com").orElseThrow();

			setCreatedAtUsingReflection(loadedDormantUser, now.minusMonths(13));
			setCreatedAtUsingReflection(loadedActiveUser, now.minusMonths(13));

			entityManager.merge(loadedDormantUser);
			entityManager.merge(loadedActiveUser);

			entityManager.flush(); // 변경사항 DB에 반영
			entityManager.clear(); // 영속성 컨텍스트 초기화
			log.info("EntityManager flushed and cleared after saving initial members and setting createdAt.");
			return null;
		});

		// When (실행)
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("runDate", now.toString())
			.addString("thresholdDate",
				now.minusYears(1).toLocalDate().atStartOfDay().toString()) // "2024-09-09T00:00:00"
			.toJobParameters();
		log.info("Job parameters prepared: runDate={}, thresholdDate={}",
			jobParameters.getString("runDate"), jobParameters.getString("thresholdDate"));

		jobLauncherTestUtils.setJob(this.dormantUserJob);
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

		Member dormantUser = memberRepository.findByEmail("dormant.user@example.com").orElseThrow();
		Member activeUser = memberRepository.findByEmail("active.user@example.com").orElseThrow();
		log.info("Retrieved dormantUser: email={}, status={}, createdAt={}, lastLoggedInAt={}",
			dormantUser.getEmail(), dormantUser.getStatus(), dormantUser.getCreatedAt(),
			dormantUser.getLastLoggedInAt());
		log.info("Retrieved activeUser: email={}, status={}, createdAt={}, lastLoggedInAt={}",
			activeUser.getEmail(), activeUser.getStatus(), activeUser.getCreatedAt(), activeUser.getLastLoggedInAt());

		assertThat(dormantUser.getStatus()).isEqualTo(MemberStatus.DORMANT);
		assertThat(activeUser.getStatus()).isEqualTo(MemberStatus.ACTIVE);
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