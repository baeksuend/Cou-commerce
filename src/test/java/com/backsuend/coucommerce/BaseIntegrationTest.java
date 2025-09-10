package com.backsuend.coucommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.backsuend.coucommerce.auth.dto.LoginRequest;
import com.backsuend.coucommerce.auth.dto.SignupRequest;
import com.backsuend.coucommerce.auth.entity.Member;
import com.backsuend.coucommerce.auth.entity.Role;
import com.backsuend.coucommerce.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@TestPropertySource(properties = "spring.batch.job.enabled=false")
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected MemberRepository memberRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected StringRedisTemplate redisTemplate;

	@Autowired
	protected EntityManager entityManager;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@BeforeEach
	void clearRedisData() {
		redisTemplate.getConnectionFactory().getConnection().flushAll();
	}

	protected String registerAndLogin(String email, String password, String name, String phone) throws Exception {
		// 1. Register
		SignupRequest signupRequest = new SignupRequest(
			email, password, name, phone,
			"04538", "서울특별시 중구 세종대로 110", "101호"
		);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupRequest)))
			.andReturn();

		// 2. Login
		LoginRequest loginRequest = new LoginRequest(email, password);
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		String responseString = result.getResponse().getContentAsString();
		return objectMapper.readTree(responseString).get("data").get("accessToken").asText();
	}

	protected String login(String email, String password) throws Exception {
		LoginRequest loginRequest = new LoginRequest(email, password);
		System.out.println("login loginRequest=" + loginRequest);
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();
		System.out.println("login 222");

		String responseString = result.getResponse().getContentAsString();
		System.out.println("login responseString ==" + responseString);
		return objectMapper.readTree(responseString).get("data").get("accessToken").asText();
	}

	protected Member createMember(String email, String password, Role role) {
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("Test User")
			.phone("010-1234-5678")
			.role(role)
			.status(com.backsuend.coucommerce.auth.entity.MemberStatus.ACTIVE)
			.build();
		return memberRepository.save(member);
	}

	protected String getRefreshToken(String email, String password) throws Exception {
		LoginRequest loginRequest = new LoginRequest(email, password);
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		String responseString = result.getResponse().getContentAsString();
		return objectMapper.readTree(responseString).get("data").get("refreshToken").asText();
	}

	protected Member findMemberInNewTransaction(Long memberId) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		return transactionTemplate.execute(status -> memberRepository.findById(memberId).orElse(null));
	}
}
