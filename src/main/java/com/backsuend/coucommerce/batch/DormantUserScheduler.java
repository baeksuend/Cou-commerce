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
public class DormantUserScheduler {

	private final JobLauncher jobLauncher;
	private final Job dormantUserJob;

	@Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
	public void runDormantUserJob() {
		/**
		 * 휴면 사용자 처리 배치를 실행합니다.
		 * -
		 * MDC-CONTEXT:
		 * - 공통 필드: traceId, memberId (이메일), memberRole
		 * - jobName: 실행되는 배치 작업의 이름
		 * - runDate: 배치 작업 실행 날짜
		 * - thresholdDate: 휴면 사용자 판단 기준 날짜
		 */
		try (var ignored = MdcLogging.withContexts(Map.of(
			"jobName", "DormantUserJob",
			"runDate", LocalDate.now().toString(),
			"thresholdDate", LocalDateTime.now().minusYears(1).toString()
		))) {
			try {
				log.info("휴면 사용자 배치 작업 시작...");
				jobLauncher.run(
					dormantUserJob,
					new JobParametersBuilder()
						.addString("runDate", LocalDate.now().toString())
						.addString("thresholdDate", LocalDateTime.now().minusYears(1).toString())
						.toJobParameters()
				);
				log.info("휴면 사용자 배치 작업 성공적으로 실행됨.");
			} catch (Exception e) {
				log.error("휴면 사용자 배치 작업 실행 실패", e);
			}
		}
	}
}
