# 프로젝트에서의 역할(로컬 개발 아키텍처)
```
[App 컨테이너]  ──(cou-commerce-net)──>  [mysql:8.4 LTS]
   │
   └────────(cou-commerce-net)──>  [redis:7.4]
   ```

##  의도
- DB와 캐시를 각각 별도 Compose 스택으로 독립 실행하면서, 공통 외부 네트워크(cou-commerce-net)로 애플리케이션이 접근하도록 설계

## 이점
- 서비스별 lifecycle 분리(DB만 재시작해도 App 영향 최소화)
- 스택 교체/확장(예: Redis→클러스터) 용이
- 팀별 책임 경계 명확

## 핵심 규칙
- 외부 네트워크는 미리 생성해두고(docker network create cucommerce-net), 각 Compose에서 networks: { cucommerce-net: { external: true } }로 공유합니다.
---
# 실행 방법
## 공통 준비(한번만 실행)
```
docker network create cou-commerce-net
```
- App의 연결 호스트명은 각 서비스명(예: mysql, redis)을 그대로 사용합니다. Compose 네트워크에서 서비스명이 곧 DNS 이름

---
# 인프라 스택
## A. MySQL 스택 (권장: 8.4 LTS)
- 이미지: mysql:8.4 ← 커뮤니티 LTS 릴리스(장기 지원) 라인
- 포트(기본): 3306 
- 지속 데이터: mysql-data 볼륨(/var/lib/mysql)

### 권장 옵션
- 문자셋/콜레이션: utf8mb4 / utf8mb4_0900_ai_ci 
- 타임존: Asia/Seoul 
- 적정 max_connections(개발 200 정도)
- 예시 연결 문자열 (Spring)
    ```
    jdbc:mysql://mysql:3306/coucommercedb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username=app
    password=app-secret
    ```
- 헬스체크: mysqladmin ping 활용해 의존 서비스가 준비된 뒤 App 기동

## B. Redis 스택 (권장: 7.4 계열)

- 이미지: redis:7.4 (공식 Docker Hub)
- 포트(기본): 6379
- 지속 데이터: redis-data 볼륨(/data), AOF 모드(--appendonly yes)
- 예시 연결 URI: redis://redis:6379
- 헬스체크: redis-cli ping

### 특징
```
두 스택은 서로 별도 docker-compose.yml에 존재하며, 둘 다 cou-commerce-net에 참여하므로 App은 mysql, redis라는 호스트명으로 접근합니다. (다중 Compose 프로젝트 간 통신의 정석 패턴)
``` 