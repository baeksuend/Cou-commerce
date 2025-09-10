package com.backsuend.coucommerce.batch;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.MemberStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DormantUserBatchConfig {

	private static final int CHUNK_SIZE = 1; // 청크 사이즈 1로 변경

	@Bean
	public Job dormantUserJob(JobRepository jobRepository, @Qualifier("dormantUserStep") Step dormantUserStep) {
		return new JobBuilder("dormantUserJob", jobRepository)
			.start(dormantUserStep)
			.build();
	}

	@Bean
	public Step dormantUserStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
		@Qualifier("dormantUserReader") JpaPagingItemReader<Member> reader, ItemProcessor<Member, Member> processor,
		@Qualifier("dormantUserWriter") JpaItemWriter<Member> writer) {
		return new StepBuilder("dormantUserStep", jobRepository)
			.<Member, Member>chunk(CHUNK_SIZE, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<Member> dormantUserReader(EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['thresholdDate']}") String thresholdDateStr) {
		log.info("DormantUserReader: Received thresholdDateStr = {}", thresholdDateStr);
		LocalDateTime threshold = LocalDateTime.parse(thresholdDateStr);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("status", MemberStatus.ACTIVE);
		parameters.put("threshold", threshold);

		String queryString = "SELECT m FROM Member m WHERE m.status = :status AND m.createdAt < :threshold AND (m.lastLoggedInAt IS NULL OR m.lastLoggedInAt < :threshold)";
		log.info("DormantUserReader: 상태 = {} 및 임계값 = {}로 쿼리 중", MemberStatus.ACTIVE, threshold);
		log.info("DormantUserReader: 쿼리 문자열 = {}", queryString);

		JpaPagingItemReader<Member> reader = new JpaPagingItemReaderBuilder<Member>()
			.name("dormantUserReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.queryString(queryString)
			.parameterValues(parameters)
			.saveState(false)
			.build();

		reader.setSaveState(false);
		return reader;
	}

	@Bean
	public ItemProcessor<Member, Member> dormantUserProcessor() {
		return member -> {
			log.info("DormantUserProcessor: 이메일: {}, 현재 상태: {}인 회원 처리 중", member.getEmail(),
				member.getStatus());
			if (member.getStatus() == MemberStatus.ACTIVE) {
				member.updateStatus(MemberStatus.DORMANT);
				log.info("DormantUserProcessor: 회원 {}의 상태가 휴면으로 업데이트되었습니다.", member.getEmail());
			}
			return member;
		};
	}

	@Bean
	public JpaItemWriter<Member> dormantUserWriter(EntityManagerFactory entityManagerFactory) {
		JpaItemWriter<Member> jpaItemWriter = new JpaItemWriter<Member>() {
			@Override
			public void write(Chunk<? extends Member> chunk) {
				log.info("DormantUserWriter: {}개 항목 작성 중.", chunk.size());
				chunk.forEach(
					member -> log.info("DormantUserWriter: 회원: {} 상태: {} 작성 중", member.getEmail(),
						member.getStatus()));
				super.write(chunk);
			}
		};
		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
		jpaItemWriter.setUsePersist(false);  // merge 사용
		return jpaItemWriter;
	}
}
