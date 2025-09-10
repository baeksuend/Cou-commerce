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
public class DormantUserScheduler {

	private final JobLauncher jobLauncher;
	private final Job dormantUserJob;

	@Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
	public void runDormantUserJob() {
		try {
			log.info("Starting dormant user job...");
			jobLauncher.run(
				dormantUserJob,
				new JobParametersBuilder()
					.addString("runDate", LocalDate.now().toString())
					.addString("thresholdDate", LocalDateTime.now().minusYears(1).toString())
					.toJobParameters()
			);
			log.info("Dormant user job launched with runDate: {} and thresholdDate: {}", LocalDate.now().toString(), LocalDateTime.now().minusYears(1).toString());
			log.info("Dormant user job finished successfully.");
		} catch (Exception e) {
			log.error("Failed to run dormant user job", e);
		}
	}
}
