# PRD: Cart, Order, Payment 관측성(로깅/모니터링) 적용

## 1) 소개/개요
- 목적: Cart, Order, Payment 도메인에 운영 가시성을 확보하여 오류 탐지/원인분석, 비즈니스 전환 가시화, 성능/지연 모니터링, 감사 추적을 강화한다.
- 적용 범위: Cart, Order, Payment 도메인(현 프로젝트 내 공통 Logback/MDC 기반 로깅 유지). 메트릭·트레이싱·알림은 이번 범위에서 제외(대시보드 임계치 라인만 표시, 향후 알림 연계는 계획 문서화).
- 배포 환경: Prod 즉시 적용(A). 로컬 개발환경은 별도로 존재(C)하나, 본 PRD는 Prod 적용을 대상으로 한다.
- 수집/시각화: JSON 구조화 로그(Logback + logstash-logback-encoder) → Filebeat → Logstash → Elasticsearch → Kibana(기존 인덱스 패턴 유지: `cou-commerce-YYYY.MM.dd`).

## 2) 목표(측정 가능)
- [G1] 모든 HTTP 요청/응답에 대해 상관관계 ID(`traceId`)와 사용자 식별 해시(`memberId`=해시값) 포함하여 JSON 로그가 100% 수집·색인된다.
- [G2] Cart/Order/Payment 각 도메인 맥락(MDC)이 주요 흐름에 자동 주입되어, Kibana에서 도메인별 필터링과 트랜잭션 추적이 가능하다.
- [G3] 개인정보/결제정보는 로깅 금지 또는 비식별화(사용자 식별자는 해시만). 보존기간 30일 유지.
- [G4] 도메인별 대시보드 3개(Cart/Order/Payment)를 제공하고, 핵심 지표에 임계치 라인을 표시한다(알림 미연계).
- [G5] 샘플링 100% 유지(에러/경고/정보 모두 수집). 수집 누락률 1% 이하.

## 3) 사용자 스토리
- [S1] 온콜 엔지니어로서, 결제 실패/지연 증가 시 대시보드에서 원인을 5분 내 추정할 수 있다(로그 기반, 알림은 차후 연계).
- [S2] 백엔드 개발자로서, 주문 생성/취소/환불 흐름에서 주문 ID, 상태 전이, 총액 등을 로그 필터로 추적할 수 있다.
- [S3] PM으로서, 장바구니→주문→결제 퍼널 전환율과 단계별 오류 사유를 일/주 단위로 확인할 수 있다(로그 집계로 근사).
- [S4] 보안 담당자로서, 로그에 이메일/카드번호 등 PII가 남지 않음을 확인할 수 있다(해시/마스킹 적용, 정책 준수).

## 4) 기능 요구사항(Functional Requirements)
1. 요청 단위 상관관계
   - 1.1 모든 요청에 `traceId`를 생성/전파하고 MDC에 저장한다. 기본 이름은 기존 스키마(`traceId`)를 유지한다.
   - 1.2 응답/에러 로그에서도 동일 `traceId`로 상관관계를 보장한다.
2. 사용자 식별 비식별화(PII 정책 B)
   - 2.1 인증된 요청의 사용자 식별자는 평문 저장 금지. `memberId` 키에는 HMAC-SHA256(secret)으로 해시된 값만 저장한다(현 스키마 키 이름 유지, 값만 해시).
   - 2.2 해시 시크릿은 환경변수(예: `LOG_USER_HASH_SECRET`)로 주입한다.
3. HTTP 접근 로그(Access Log)
   - 3.1 모든 요청 완료 시 INFO 레벨 한 줄 로그를 남긴다. 필수 필드: `http.method`, `http.path`, `http.status`, `latency_ms`, `traceId`, `memberId(해시)`.
   - 3.2 IP/UA는 기본 비수집. 필요 시 IP는 해시 또는 /24 마스킹 정책 별도 합의 후 도입.
4. 도메인 맥락 MDC 자동 주입(현 스키마 유지)
   - 4.1 Cart: `cartId`(선택), `productId`, `quantity`, `action(add|update|remove)`를 가능하면 주입한다.
   - 4.2 Order: `orderId`, `status`, `totalAmount`를 주요 흐름 지점에 주입한다.
   - 4.3 Payment: `paymentId`, `method`, `status`, `orderId`를 주요 흐름 지점에 주입한다.
   - 4.4 주입/해제는 AOP/Interceptor로 감싼 블록 내에서 수행하고, finally에서 반드시 해제한다.
5. 오류 로깅 표준화
   - 5.1 전역 예외 처리에서 `traceId`, `path`, 표준 에러코드, 메시지를 포함한 JSON을 반환 및 로그에 남긴다(현 구현 유지 확인).
   - 5.2 비즈니스 예외 발생 시 도메인 MDC가 함께 기록되도록 보장한다.
6. 로깅 포맷/수집/색인
   - 6.1 JSON 구조화 로그 유지(LogstashEncoder + includeMdc=true). 파일 경로 및 롤링 정책은 현행 유지.
   - 6.2 Filebeat→Logstash→Elasticsearch 파이프라인 유지. 인덱스 네이밍은 `cou-commerce-%{+YYYY.MM.dd}` 유지.
   - 6.3 보존기간 30일(파일 롤링 및 ES 인덱스 ILM/수동 삭제 중 택1, 현 환경 기준 유지).
7. 결제 필드 로깅(범위 확대 요청 반영)
   - 7.1 결제 시 저장 가능한 필드: `provider/mock`, `transactionId(마스킹/토큰)`, `amount`, `result(success|failed)`, `reason_code/message(민감정보 제외)`, `orderId`, `paymentId`.
   - 7.2 카드 번호 등 민감정보는 미수집. 카드 브랜드만 허용.
8. 샘플링/레벨
   - 8.1 샘플링은 전부 100% 수집. 에러/경고/정보 모두 대상.
9. 대시보드(Kibana)
   - 9.1 Cart/Order/Payment 도메인별 3개 대시보드 제공.
   - 9.2 패널 예시: 트래픽, 오류율, p50/p95/p99 지연, 단계별 성공/실패 분포, 상위 에러코드/메시지, 사용자/주문/결제 ID 기준 TopN.
   - 9.3 임계치 라인만 표시(알림 미연계). 향후 알림 연계는 계획에 포함.

## 5) 비범위(Non-Goals)
- Prometheus/Grafana 등 메트릭 도입, OpenTelemetry 분산추적 도입, 알림 시스템 연동은 본 차수에서 제외(계획만 문서화).
- 인덱스 네이밍/파이프라인 구조 변경, 로그 스키마 전면 개편은 제외(현 스키마 유지, 샘플 공유 후 미세조정).
- 운영 조직/온콜 체계 확정은 추후 합의(계획 섹션에 옵션 형태 기재).

## 6) 설계 고려사항(Design)
- 로깅 프레임워크: Logback + logstash-logback-encoder(JSON). MDC 사용.
- 기존 구성 확인:
  - 요청 필터에서 `traceId`, `memberId(이메일)`, `memberRole` MDC 주입: `src/main/java/com/backsuend/coucommerce/common/filter/LoggingFilter.java`.
  - 전역 예외 응답에 `traceId` 포함: `src/main/java/com/backsuend/coucommerce/common/exception/GlobalExceptionHandler.java`.
  - 도메인 컨텍스트 유틸 존재(현재 미사용):
    - Cart: `src/main/java/com/backsuend/coucommerce/cart/logging/CartLogContext.java`
    - Order: `src/main/java/com/backsuend/coucommerce/order/logging/OrderLogContext.java`
    - Payment: `src/main/java/com/backsuend/coucommerce/payment/logging/PaymentLogContext.java`
- 제안 변경(what 중심):
  - LoggingFilter의 `memberId` 값은 이메일 대신 HMAC-SHA256 해시로 저장(키 이름은 유지, 값만 해시). 환경변수 시크릿 사용.
  - HTTP Access Log 전용 필터/인터셉터 추가: 완료 시점에 `latency_ms`, `http.status` 포함하여 INFO 1줄 기록.
  - 컨트롤러/서비스 진입부에 도메인 MDC 자동 주입(AOP/Interceptor). 유틸 클래스 즉시 활용하고 finally에서 해제.

## 7) 기술 고려사항(Technical)
- 보안/규정: 한국 기준 개인정보보호 준수. PCI-DSS 고려하되 민감 결제정보 미수집 원칙 유지.
- 보존기간: 파일/ES 30일. ES는 인덱스 수동 삭제 또는 ILM 정책 검토(현 환경 유지 시 수동 관리).
- 인덱스 패턴: `cou-commerce-%{+YYYY.MM.dd}` 유지(Logstash 출력 설정 현행).
- 성능 오버헤드: 필터/인터셉터 추가에 따른 오버헤드는 수 ms 수준 예상. 샘플링 100%여도 JSON 직렬화 비용은 허용 범위.
- 구성 관리: 환경 변수 `LOG_USER_HASH_SECRET` 주입 필요. 미설정 시 안전 폴백(로깅 비활성/익명 처리) 고려.

## 8) 성공 지표(이번 차수: 계획)
- MTTR 30% 감소(로그 상관관계/도메인 컨텍스트로 원인 파악 시간 단축).
- 이상 발생→대시보드에서 5분 내 추적 가능(알림은 차후 연계).
- 퍼널 모니터링 가능(장바구니→주문→결제 단계별 성공/실패율 시계열 제공).

## 9) 수용 기준(이번 차수: 계획)
- A1 요청/응답 한 건당 1개의 INFO 접근 로그가 생성되고, `traceId`/`latency_ms`/`http.status`/`memberId(해시)`가 포함된다.
- A2 Cart/Order/Payment 대표 플로우에서 도메인 MDC가 로그에 최소 1회 이상 등장한다.
- A3 Kibana에서 도메인별 대시보드 접근 가능, 임계치 라인이 표시된다.
- A4 로그에 이메일/카드번호 등 PII가 남지 않는다(샘플 검수 통과).
- A5 보존기간 30일 확인(롤링/인덱스 존재 확인).

## 10) 핵심 임계치(코드 분석 기반 제안; 대시보드 라인만)
- 환경 가정: Cart는 Redis I/O 중심, Order/Payment는 DB I/O 위주. 외부 PG 연동 없음(모의 메소드), 따라서 지연은 내부 처리 한정.
- 제안 값(조정 가능):
  - Cart API p95 지연: 200ms
  - Order 생성(POST /orders) p95 지연: 800ms
  - Order 조회/취소 p95 지연: 300ms
  - Payment 승인(POST /payments) p95 지연: 500ms
  - Payment 조회 p95 지연: 300ms
  - 주문 생성 오류율(회귀; 5분 윈도우): > 2%
  - 결제 성공률(15분 윈도우): < 99.0%
- 근거: 현재 결제는 외부 PG 호출 없이 DB 상태 전이만 수행하며, 장바구니는 Redis Hash I/O 기반이므로 상대적으로 낮은 지연 기대.

## 11) 대시보드 구성(도메인별 3개)
- Cart 대시보드
  - 트래픽(요청 수), p50/p95/p99 지연, 오류율, add/update/remove 분포, productId TopN(샘플)
- Order 대시보드
  - 주문 생성/조회/취소 트래픽, p95 지연, 오류율, 상태전이(PLACED→PAID→SHIPPED→COMPLETED/REFUNDED) 추이, 실패 사유 TopN
- Payment 대시보드
  - 승인/실패 건수, p95 지연, 성공률, 실패 사유 코드 분포, refund 요청/승인 추이
- 공통: 임계치 라인 표시만(알림은 차후 연계 계획 반영)

## 12) 오픈 이슈/질문
- 사용자 해시 키 관리: `LOG_USER_HASH_SECRET` 비밀 관리 위치(Prod 시크릿 매니저/환경변수) 확정 필요.
- IP/UA 수집 여부: 기본 미수집. 보안/분석 목적 필요 시 마스킹/해시 정책 합의 필요.
- Kibana 스페이스/권한: 전사 공개 vs 도메인 팀 전용 중 선택 필요.
- 접근 로그 필드 명세: 현 스키마 유지 전제에서 `latency_ms` 등 새 필드의 키 이름 확정 필요.
- 퍼널 이벤트 스키마: 현 스키마 유지 원칙하에 비즈니스 이벤트 키(예: `event=cart.add` 등)를 어떤 키에 담을지 합의 필요.

## 13) 구현 계획(2주)
- W1
  - (D1) LoggingFilter 사용자 식별자 해시 적용(HMAC-SHA256), 미설정 시 폴백 처리
  - (D2) HTTP Access Log 필터/인터셉터 추가(지연/상태 기록)
  - (D3) 도메인 MDC 주입(AOP/Interceptor) — Cart/Order/Payment 핵심 경로 연결 및 finally 해제
  - (D4) Kibana 임시 대시보드 초안 생성(3개)
- W2
  - (D5) 현 스키마와 충돌 여부 점검(샘플 로그 캡처), 필드명 미세조정
  - (D6) Prod 배포 및 스모크 체크(색인/대시보드 확인)
  - (D7) 문서화/운영 핸드오프(알림 연계 로드맵 포함)

## 14) 롤아웃/운영(계획)
- 전략: Prod 즉시 적용(모니터링 전용 변경, 위험도 낮음). 필요 시 기능 플래그로 Access Log 토글 가능.
- 책임: 공동 운영(플랫폼/인프라 + 각 도메인 팀) 제안 — 최종 확정 필요.
- 샘플링: 100% 유지.

## 15) 부록(참고)
- 현 구성 파일 예시
  - Logback(JSON): `src/main/resources/logback-spring.xml`
  - 파이프라인: `infra/elk/filebeat-docker.yml`, `infra/elk/logstash.conf`
- 기존 도메인 컨텍스트 유틸(현 사용 확대 예정)
  - Cart: `CartLogContext.setCartContext(cartId?, productId, quantity)`
  - Order: `OrderLogContext.setOrderContext(orderId, status, totalAmount)`
  - Payment: `PaymentLogContext.setPaymentContext(paymentId, method, status)`

