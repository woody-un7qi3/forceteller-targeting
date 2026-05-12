# Changelog

## [Unreleased]

### Breaking
- `Rule.id` 타입 변경: `String` → `Long`
  - 정수 PK 정책 채택. 호스트 DB의 `targeting_rules.id BIGINT`와 1:1 매핑
  - 슬러그 사용 안 함. 사람 친화 표시는 호스트의 `label` 컬럼이 담당

### Fixed
- `AttributeBag.snapshot()` / `MapCandidate` 생성자가 null value 거부하던 문제
  - `Map.copyOf` → `Collections.unmodifiableMap(new LinkedHashMap<>())`로 교체
  - docs상 “null value 허용”과 구현 모순 해소

### Added
- 초기 프로젝트 구조 (Gradle Kotlin DSL, Java 21)
- `core/rule/` — Rule, RuleNode (sealed), Operator (enum)
- `core/candidate/` — TargetingCandidate, AttributeBag, MapCandidate
- `core/attribute/` — AttributeType, AttributeStatus, AttributeSpec, AttributeProvider, AttributeCatalog
- `core/evaluator/` — RuleEvaluator, EvaluationResult, TraceEntry
- `core/error/` — RuleValidationException, UnknownAttributeException
- `engine/tree/TreeRuleEvaluator` — 룰 트리 재귀 평가
- `engine/validate/RuleValidator` — 룰 구조·카탈로그·깊이 검증
- `engine/serde/RuleJsonMapper` + `TargetingJacksonModule` — JSON 직렬화
- GitHub Packages publish 설정
