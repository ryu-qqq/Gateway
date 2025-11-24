# Persistence Layer — **MySQL (JPA/QueryDSL)**

> 이 문서는 `persistence-layer` 중 **MySQL(JPA/QueryDSL)** 파트에 대한 **요약 가이드**
> 
> **핵심 원칙**, **패키징 구조**, 그리고 각 디렉터리별 **상세 가이드 링크**를 제공합니다.

---

## 1) 핵심 원칙 (한눈에)

* **어댑터=비즈니스 로직 금지**: 포트가 준 명령대로 **저장/조회만** 수행한다. 가공·검증·정책 판단은 애플리케이션/도메인에서.
* **CQRS 분리 고정**: **Command=JPA 레포**, **Query=QueryDSL 레포**. 구현/패키지 **절대 섞지 않는다**.
* **트랜잭션은 애플리케이션 레이어 전용**: `@Transactional`은 유스케이스에만. 리포지토리/어댑터/엔티티 레벨 적용 금지.
* **엔티티 연관관계 금지**: N+1 회피를 위해 엔티티에는 FK 컬럼만. 필요한 그래프는 애플리케이션에서 **재조립**.
* **Persist 포트 통일**: 생성/수정/소프트삭제 반영은 `Persist*Port.persist(aggregate)`로 저장. 소프트 딜리트는 **도메인 행위**.
* **매퍼는 순수 Java**: MapStruct 미사용, 명시적 변환. **세터 금지**, 생성자/정적 팩토리로 상태 확정.
* **Open‑in‑View 비활성화**: 읽기는 Projection/QueryDSL로 해결. 엔티티를 API로 직접 노출하지 않는다.


### 금지사항

* Lombok, MapStruct, 엔티티 세터, 엔티티 연관관계(`@OneToMany/@ManyToOne` 등)
* JPA 레포와 QueryDSL 레포 **혼합 구현/공유**
* 어댑터/리포지토리/엔티티에서의 `@Transactional`

---

## 2) 패키징 구조 (CQRS 분리, BC 예)

```
persistence-mysql/
├─ config/
│  ├─ FlywayConfig.java
│  └─ JpaConfig.java
│
├─ common/
│  └─ entity/
│     ├─ BaseAuditEntity.java
│     └─ SoftDeletableEntity.java
│
└─ order/                         # ← 예시 BC
   ├─ adapter/
   │  ├─ OrderCommandAdapter.java
   │  └─ OrderQueryAdapter.java
   ├─ entity/
   │  └─ OrderJpaEntity.java
   ├─ mapper/
   │  └─ OrderJpaEntityMapper.java
   └─ repository/
      ├─ OrderJpaRepository.java
      └─ OrderQueryDslRepository.java
      
```
---

## 3) 디렉터리별 상세 가이드 링크
* **config/**

* **adapter/**
	* `CommandAdapter/`: [Command Adapter 가이드](./adapter/command-adapter-guide.md) | [테스트](./adapter/command-adapter-test-guide.md) | [ArchUnit](./adapter/command-adapter-archunit.md)
	* `QueryAdapter/`: [Query Adapter 가이드](adapter/query/query-adapter-guide.md) 

* **entity/**


* **mapper/**


* **repository/**


---


