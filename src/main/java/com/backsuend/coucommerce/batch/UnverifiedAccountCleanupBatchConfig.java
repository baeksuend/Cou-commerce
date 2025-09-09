package com.backsuend.coucommerce.batch;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
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
public class UnverifiedAccountCleanupBatchConfig {

	private static final int CHUNK_SIZE = 100;

	private final EntityManager entityManager;

	@Bean
	public Job unverifiedAccountCleanupJob(JobRepository jobRepository,
		@Qualifier("unverifiedAccountCleanupStep") Step unverifiedAccountCleanupStep) {
		return new JobBuilder("unverifiedAccountCleanupJob", jobRepository)
			.start(unverifiedAccountCleanupStep)
			.build();
	}

	@Bean
	public Step unverifiedAccountCleanupStep(JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		@Qualifier("unverifiedAccountReader") JpaPagingItemReader<Member> unverifiedAccountReader,
		@Qualifier("unverifiedAccountWriter") ItemWriter<Member> unverifiedAccountWriter) {
		return new StepBuilder("unverifiedAccountCleanupStep", jobRepository)
			.<Member, Member>chunk(CHUNK_SIZE, transactionManager)
			.reader(unverifiedAccountReader)
			.writer(unverifiedAccountWriter)
			.build();
	}

	@Bean
	@StepScope
	public JpaPagingItemReader<Member> unverifiedAccountReader(EntityManagerFactory entityManagerFactory,
		@Value("#{jobParameters['cleanupThreshold']}") String cleanupThresholdStr) {
		LocalDateTime cleanupThreshold = LocalDateTime.parse(cleanupThresholdStr);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("status", MemberStatus.PENDING_VERIFICATION);
		parameters.put("cleanupThreshold", cleanupThreshold);

		return new JpaPagingItemReaderBuilder<Member>()
			.name("unverifiedAccountReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.queryString("SELECT m FROM Member m WHERE m.status = :status AND m.createdAt < :cleanupThreshold")
			.parameterValues(parameters)
			.build();
	}

	@Bean
	public ItemWriter<Member> unverifiedAccountWriter() {
		return items -> {
			List<Long> memberIds = items.getItems().stream()
				.map(Member::getId)
				.collect(Collectors.toList());

			if (memberIds.isEmpty()) {
				return;
			}

			log.info("Bulk deleting {} unverified members and their addresses. Member IDs: {}", memberIds.size(),
				memberIds);

			// Address Bulk Delete (JPQL)
			// Note: JPQL bulk operations do not cascade. We must delete related entities manually.
			String addressDeleteQuery = "DELETE FROM Address a WHERE a.member.id IN :memberIds";
			entityManager.createQuery(addressDeleteQuery)
				.setParameter("memberIds", memberIds)
				.executeUpdate();

			// Member Bulk Delete (JPQL)
			String memberDeleteQuery = "DELETE FROM Member m WHERE m.id IN :memberIds";
			entityManager.createQuery(memberDeleteQuery)
				.setParameter("memberIds", memberIds)
				.executeUpdate();
		};
	}
}
