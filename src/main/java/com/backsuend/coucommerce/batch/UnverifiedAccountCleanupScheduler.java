package com.backsuend.coucommerce.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.backsuend.coucommerce.common.service.MdcLogging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedAccountCleanupScheduler {

	private final JobLauncher jobLauncher;
	private final Job unverifiedAccountCleanupJob;

	// 매일 새벽 3시에 실행
	@Scheduled(cron = "0 0 3 * * ?")
	public void cleanupUnverifiedAccounts() {
		/**
		 * 미인증 계정 정리 배치를 실행합니다.
		 * -
		 * MDC-CONTEXT:
		 * - 공통 필드: traceId, memberId (이메일), memberRole
		 * - jobName: 실행되는 배치 작업의 이름
		 * - runDate: 배치 작업 실행 날짜
		 * - cleanupThreshold: 미인증 계정 정리 기준 날짜/시간
		 */
		try (var ignored = MdcLogging.withContexts(Map.of(
			"jobName", "UnverifiedAccountCleanupJob",
			"runDate", LocalDate.now().toString(),
			"cleanupThreshold", LocalDateTime.now().minusHours(24).toString()
		))) {
			try {
				log.info("미인증 계정 정리 스케줄러 시작: {}", LocalDateTime.now());

				jobLauncher.run(
					unverifiedAccountCleanupJob,
					new JobParametersBuilder()
						.addString("runDate", LocalDate.now().toString()) // Unique job instance
						.addString("cleanupThreshold", LocalDateTime.now().minusHours(24).toString()) // 24시간 지난 계정
						.toJobParameters()
				);
				log.info("미인증 계정 정리 스케줄러 종료: {}", LocalDateTime.now());
			} catch (Exception e) {
				log.error("미인증 계정 정리 Job 실행 실패", e);
			}
		}
	}
}
