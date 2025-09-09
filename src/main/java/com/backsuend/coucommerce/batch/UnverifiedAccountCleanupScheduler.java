package com.backsuend.coucommerce.batch;

import java.time.LocalDateTime;
import java.time.LocalDate;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
