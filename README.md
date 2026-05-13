# forceteller-targeting

도메인 무관, DI 프레임워크 무관, **속성 기반 룰 평가 엔진** 라이브러리.

> "이 사용자(/디바이스/계정)가 이 룰에 매칭되는가?" 라는 질문에 답하는 라이브러리.
> 광고 타게팅, 콘텐츠 노출, 푸시 자격, 이벤트 자격 등 어디든 공통으로 쓸 수 있게 설계됨.

---

## 설계 원칙

라이브러리 전반을 관통하는 4가지 원칙:

### 1. 프레임워크 중립 (Framework-neutral)

라이브러리 코드는 **Spring/Spring Boot/Quarkus/Micronaut 어느 것에도 의존하지 않음**.
호스트가 어떤 DI 도구를 쓰든 — 또는 순수 Java든 — 동일하게 동작.

- `@Component`, `@Service`, `@Autowired` 등 프레임워크 어노테이션 없음
- 라이브러리는 생성자 주입을 받는 평범한 클래스만 노출
- 호스트가 자기 DI 도구로 빈 등록 (또는 직접 `new`)

### 2. Subject 타입 자유 (Generic over `<S>`)

라이브러리는 **누가 평가 대상인지 모름**. 호스트가 자기 도메인 타입을 끼워 넣음:

```java
TargetingResolver<User>     // 사용자 단위 평가
TargetingResolver<Device>   // 디바이스 단위 평가
TargetingResolver<Account>  // 계정 단위 평가
```

`<S>`는 "평가 단위"(target). use-case 도메인(Today/Premium/Banner 등)이 아님 —
사용 도메인은 같은 resolver를 공유하며 다른 `Rule`을 넘길 뿐.

### 3. 관심사 분리 (Separation of Concerns)

| 책임 | 라이브러리 표현 |
|---|---|
| **속성 스키마 정의** (key/type/operator) | `AttributeSpec`, `AttributeProvider` |
| **평가 대상** (host 도메인 객체) | 제네릭 `<S>` |
| **평가 입력** (속성값 가방) | `EvaluationInput` |
| **룰** (조건 트리) | `Rule`, `RuleNode` |
| **평가 결과** | `EvaluationResult`, `TraceEntry` |
| **JSON 직렬화** | `RuleJsonMapper` |

각 책임은 별도 타입으로 분리되어 서로 알지 못함. 룰 엔진은 host 도메인을 모르고,
host 도메인은 룰 표현을 모름. 둘 사이를 `EvaluationInput`이 잇는다.

### 4. 비용 최소화 (Pay only for what you query)

룰이 실제로 참조하는 키만 채우도록 두 단계 스킵:

1. **namespace 단위 스킵**: 룰이 `user.*`만 본다면 `device.*`, `now.*` Provider는 아예 호출 안 함
2. **키 단위 스킵**: Provider 내부에서 `if (neededKeys.contains(k)) bag.put(...)` 분기로 필요한 키만 채움

→ DB/외부 호출이 룰에 비례해 최소화됨.

---

## 핵심 추상

### `EvaluationInput` ↔ `EvaluationResult` (입출력 대칭)

라이브러리 핵심 함수는 가장 단순한 의미 쌍으로 읽힘:

```java
EvaluationResult eval(Rule rule, EvaluationInput input);
//                                ↑                  ↑
//                              입력                결과
```

`EvaluationInput`은 평가 시점의 속성값 가방. host 도메인 객체(`User` 등)가 Provider들을
거쳐 조립된 결과물.

### `Rule` (트리)

```
Rule
└── root: RuleNode
    ├── And(children: [...])
    ├── Or(children: [...])
    ├── Not(child)
    └── Compare(attribute, operator, value)
```

`RuleNode`는 **sealed interface**. And/Or/Not/Compare 네 가지로 닫힌 합타입.
JSON으로 직렬화되어 DB/파일/API로 주고받기 좋음.

### `AttributeProvider<S>` (Provider SPI)

호스트가 구현하는 속성 공급자. 한 Provider는 한 namespace 영역을 책임짐:

```java
public class UserAttributeProvider implements AttributeProvider<User> {
    public String namespace() { return "user"; }

    public List<AttributeSpec> declare() {
        return List.of(
            AttributeSpec.of("user.age",    AttributeType.INTEGER, "만 나이"),
            AttributeSpec.of("user.gender", AttributeType.STRING,  "성별")
        );
    }

    public void fill(User target, AttributeBag bag, Set<String> neededKeys) {
        if (neededKeys.contains("user.age"))    bag.put("user.age", target.getAge());
        if (neededKeys.contains("user.gender")) bag.put("user.gender", target.getGender());
    }
}
```

라이브러리는 Provider들을 모아 `AttributeRegistry`를 구성하고, 평가 시 필요한 Provider만 호출.

### `Targeting` (Facade)

라이브러리 진입점을 한 클래스에 모음. host는 IDE 자동완성으로 "필요한 부품"을 한눈에 발견:

```java
Targeting.defaultEvaluator()                       // RuleEvaluator
Targeting.jsonMapper()                             // RuleJsonMapper
Targeting.jacksonModule()                          // Jackson Module
Targeting.registry(providers)                      // AttributeRegistry
Targeting.assembler(providers, idExtractor)        // InputAssembler<S>
Targeting.resolver(assembler, evaluator)           // TargetingResolver<S>
```

---

## 데이터 흐름

```
[host]                  [라이브러리]
                        ┌─────────────────────────────────────┐
User target  ──────────▶│ TargetingResolver<User>.evaluate    │
Rule rule    ──────────▶│   ↓                                 │
                        │   keys = usedAttributes(rule.root)  │
                        │   ↓                                 │
                        │ InputAssembler<User>.assembleFor    │
                        │   ↓                                 │
                        │   for each provider:                │
                        │     if namespace needed:            │
                        │       provider.fill(target, bag, …) │◀─── host의 Provider 구현
                        │   ↓                                 │
                        │ EvaluationInput input               │
                        │   ↓                                 │
                        │ RuleEvaluator.eval(rule, input)     │
                        │   ↓                                 │
                        └───── EvaluationResult ──────────────▶
                              (matched, trace, evaluatedAt)
```

---

## 호스트가 작성하는 것

라이브러리 도입 시 호스트가 작성해야 하는 코드는 **단 두 가지**:

1. **Provider 구현체들** — 자기 도메인의 속성을 정의/채움
2. **빈 등록** — Spring Boot 호스트는 starter 한 줄로 대부분 자동, 도메인 타입 빈 2개만 직접

---

### Spring Boot 호스트 (권장)

`forceteller-targeting-spring-boot-starter` 의존성 한 줄 추가:

```kotlin
// build.gradle.kts
implementation("co.un7qi3:forceteller-targeting-spring-boot-starter:0.2.0-SNAPSHOT")
```

starter가 다음 4개 빈을 자동 등록한다 (모두 `@ConditionalOnMissingBean` — 호스트가 같은 타입 빈을 선언하면 양보):

| 빈 | 비고 |
|---|---|
| `RuleEvaluator` | `TreeRuleEvaluator` 기본 구현 |
| `RuleJsonMapper` | Rule ↔ JSON 변환기 |
| `Module targetingJacksonModule` | Spring MVC 전역 ObjectMapper에 자동 등록되어 `@RequestBody Rule` 역직렬화 지원 |
| `AttributeRegistry` | 컨텍스트의 모든 `AttributeProvider` 빈을 자동 수집해 빌드 |

호스트는 **도메인 타입에 묶인 2개 빈만 직접 선언**한다. `InputAssembler<S>`와 `TargetingResolver<S>`는 호스트의 평가 대상 타입 `<S>`(예: `User`)와 `idExtractor`(로깅용 식별자 추출 함수)가 필요해서 starter가 자동 등록할 수 없다.

```java
@Configuration
public class TargetingConfig {

    @Bean
    public InputAssembler<User> inputAssembler(List<AttributeProvider<User>> providers) {
        return Targeting.assembler(providers, u -> String.valueOf(u.getId()));
    }

    @Bean
    public TargetingResolver<User> targetingResolver(
            InputAssembler<User> assembler, RuleEvaluator evaluator) {
        return Targeting.resolver(assembler, evaluator);
    }
}
```

그리고 도메인별 `AttributeProvider<User>` 구현체에 `@Component`만 붙이면 starter가 자동으로 모아 `AttributeRegistry`를 만든다.

평가 호출:
```java
@Autowired TargetingResolver<User> targetingResolver;

EvaluationResult result = targetingResolver.evaluate(rule, user);
if (result.matched()) { ... }
```

---

### 비-Spring 호스트 (또는 직접 조립)

`forceteller-targeting-core` 의존성만 추가하고 손으로 6개 부품을 조립한다:

```java
RuleEvaluator evaluator           = Targeting.defaultEvaluator();
RuleJsonMapper jsonMapper         = Targeting.jsonMapper();
List<AttributeProvider<User>> ps  = List.of(new UserAttributeProvider(), /* ... */);
AttributeRegistry registry        = Targeting.registry(ps);
InputAssembler<User> assembler    = Targeting.assembler(ps, u -> String.valueOf(u.getId()));
TargetingResolver<User> resolver  = Targeting.resolver(assembler, evaluator);
```

---

## 어드민용 내부 API

### `GET /api/internal/targeting/attributes` — 속성 레지스트리 조회

부팅 시점에 모든 Provider가 declare한 spec 목록을 조회. 어드민은 시작 시 1회 + 주기적 새로고침으로 캐시 권장.

#### 요청
```bash
curl http://localhost:8080/api/internal/targeting/attributes
```

#### 응답 (예시 일부)
```json
[
  {
    "key": "user.gender",
    "type": "STRING",
    "allowedOps": ["EQ", "NEQ", "IN", "NOT_IN"],
    "label": "성별 (M/F)",
    "description": null,
    "status": "ACTIVE"
  },
  {
    "key": "user.age",
    "type": "INTEGER",
    "allowedOps": ["EQ", "NEQ", "GT", "GTE", "LT", "LTE", "BETWEEN"],
    "label": "만 나이",
    "description": null,
    "status": "ACTIVE"
  },
  {
    "key": "device.platform",
    "type": "STRING",
    "allowedOps": ["EQ", "NEQ", "IN", "NOT_IN"],
    "label": "플랫폼",
    "description": "android / ios / web",
    "status": "ACTIVE"
  },
  {
    "key": "device.appVersionInt",
    "type": "INTEGER",
    "allowedOps": ["EQ", "NEQ", "GT", "GTE", "LT", "LTE", "BETWEEN"],
    "label": "앱 버전(비교용 정수)",
    "description": "(major<<16)+(minor<<8)+patch",
    "status": "ACTIVE"
  },
  {
    "key": "now.kstHour",
    "type": "INTEGER",
    "allowedOps": ["EQ", "NEQ", "GT", "GTE", "LT", "LTE", "BETWEEN"],
    "label": "현재 시각(KST, 시)",
    "description": "0~23",
    "status": "ACTIVE"
  }
]
```

#### 응답 필드

| 필드 | 의미 |
|---|---|
| `key` | 룰 JSON의 `attribute` 자리에 들어갈 키 |
| `type` | 값 타입 (`STRING` / `INTEGER` / `BOOLEAN` / `INSTANT` / `LIST_STRING`) |
| `allowedOps` | 이 키에 사용 가능한 연산자 목록 |
| `label` | 어드민 UI 표시용 한국어 라벨 |
| `description` | 추가 설명 (값 범위, 단위 등) |
| `status` | `ACTIVE` 또는 `DEPRECATED` (DEPRECATED는 신규 룰에 사용 자제) |

#### 어드민 활용 패턴

1. 페이지 진입 시 이 API 호출해 캐시
2. 룰 편집 UI에서:
   - "속성 선택" 드롭다운 = `key`+`label` 목록
   - 선택된 키에 따라 "연산자" 드롭다운 = `allowedOps`로 제한
   - 선택된 연산자에 따라 "value" 입력 형태 결정 (단일/배열, type별 입력기)
3. `DEPRECATED` 상태인 키는 어드민에서 회색 처리 또는 숨김

---

## JSON 룰 예시 모음

어드민에서 룰 작성 시 참고용. 모든 예시는 `RuleJsonMapper.fromJson(json)` 으로 그대로 파싱됩니다.

### 노드 종류

| 타입 | 의미 | 자식 필드 |
|---|---|---|
| `compare` | 속성 비교 (잎 노드) | `attribute`, `operator`, `value` |
| `and` | 모두 참이어야 참 | `children: [...]` |
| `or` | 하나라도 참이면 참 | `children: [...]` |
| `not` | 자식 결과 부정 | `child: {...}` |

### 연산자 표

`value` 형태:
- **단일 값**: 문자열/숫자/불리언 한 개. 예: `"F"`, `30`, `true`
- **값 목록 (배열)**: 여러 값을 `[...]`로 묶음. 예: `[20, 39]`, `["android", "ios"]`

| 연산자 | 의미 | 적용 타입 | value 형태 | 예시 |
|---|---|---|---|---|
| `EQ` | 같음 | 모든 타입 | 단일 값 | `"F"` / `30` / `true` |
| `NEQ` | 다름 | 모든 타입 | 단일 값 | `"M"` |
| `GT` / `GTE` | 초과 / 이상 | INTEGER, INSTANT | 단일 값 | `30` |
| `LT` / `LTE` | 미만 / 이하 | INTEGER, INSTANT | 단일 값 | `100` |
| `BETWEEN` | 범위 (양 끝 포함) | INTEGER, INSTANT | 2개 원소 배열 `[min, max]` | `[20, 39]` |
| `IN` | 목록에 포함 | STRING, INTEGER | 값 목록 | `["android", "ios"]` |
| `NOT_IN` | 목록에 미포함 | STRING, INTEGER | 값 목록 | `[101, 102]` |
| `CONTAINS` | 리스트 속성이 값을 포함 | LIST_STRING | 단일 값 | `"love"` |

---

### 1. 단일 조건 — 30세 이상 사용자

```json
{
  "id": 1, "version": 1,
  "root": {
    "type": "compare", "id": "c1",
    "attribute": "user.age", "operator": "GTE", "value": 30
  }
}
```

### 2. AND 조합 — 여성, 20-39세, 안드로이드, 저녁 시간

```json
{
  "id": 2, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "id": "c1", "attribute": "user.gender",     "operator": "EQ",      "value": "F" },
      { "type": "compare", "id": "c2", "attribute": "user.age",        "operator": "BETWEEN", "value": [20, 39] },
      { "type": "compare", "id": "c3", "attribute": "device.platform", "operator": "EQ",      "value": "android" },
      { "type": "compare", "id": "c4", "attribute": "now.kstHour",     "operator": "GTE",     "value": 18 }
    ]
  }
}
```

### 3. OR 조합 — 모바일 사용자 (안드로이드 OR iOS)

```json
{
  "id": 3, "version": 1,
  "root": {
    "type": "or",
    "children": [
      { "type": "compare", "id": "c1", "attribute": "device.platform", "operator": "EQ", "value": "android" },
      { "type": "compare", "id": "c2", "attribute": "device.platform", "operator": "EQ", "value": "ios" }
    ]
  }
}
```

`IN` 연산자로 단축 가능:

```json
{
  "type": "compare", "attribute": "device.platform", "operator": "IN", "value": ["android", "ios"]
}
```

### 4. NOT — 게스트 제외

```json
{
  "id": 4, "version": 1,
  "root": {
    "type": "not",
    "child": {
      "type": "compare", "attribute": "user.isGuest", "operator": "EQ", "value": true
    }
  }
}
```

### 5. 중첩 — (저녁시간 OR 주말) AND 결제 이력 있음

```json
{
  "id": 5, "version": 1,
  "root": {
    "type": "and",
    "children": [
      {
        "type": "or",
        "children": [
          { "type": "compare", "attribute": "now.kstHour",    "operator": "GTE", "value": 18 },
          { "type": "compare", "attribute": "now.dayOfWeek",  "operator": "IN",  "value": [6, 7] }
        ]
      },
      { "type": "compare", "attribute": "action.hasCharged", "operator": "EQ", "value": true }
    ]
  }
}
```

### 6. 신규 가입 사용자 — 가입 7일 이내, 미결제

```json
{
  "id": 6, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "user.daysAfterSignup", "operator": "LTE", "value": 7 },
      { "type": "compare", "attribute": "action.hasCharged",    "operator": "EQ",  "value": false }
    ]
  },
  "meta": { "label": "신규 가입 + 미결제 (첫 결제 유도)" }
}
```

### 7. 휴면 복귀 캠페인 — 30일 무로그인 + 결제 이력

```json
{
  "id": 7, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "event.lastLoginDaysAgo", "operator": "GTE", "value": 30 },
      { "type": "compare", "attribute": "action.hasCharged",      "operator": "EQ",  "value": true }
    ]
  },
  "meta": { "label": "휴면 결제 유저 복귀 캠페인" }
}
```

### 8. 어드민/게스트 제외, 비로그인 제외

```json
{
  "id": 8, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "user.isAnonymous", "operator": "EQ", "value": false },
      { "type": "compare", "attribute": "user.isAdmin",     "operator": "EQ", "value": false },
      { "type": "compare", "attribute": "user.isGuest",     "operator": "EQ", "value": false }
    ]
  }
}
```

### 9. 앱 버전 조건 — 3.5.0 이상 안드로이드

`device.appVersionInt`는 `(major<<16) + (minor<<8) + patch` 정수 표현.

- 3.5.0 = `(3<<16) + (5<<8) + 0 = 197376`

```json
{
  "id": 9, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "device.platform",      "operator": "EQ",  "value": "android" },
      { "type": "compare", "attribute": "device.appVersionInt", "operator": "GTE", "value": 197376 }
    ]
  }
}
```

### 10. 다중 국가 제외 — 한국/일본만 허용

```json
{
  "id": 10, "version": 1,
  "root": {
    "type": "compare",
    "attribute": "device.platform",
    "operator": "IN",
    "value": ["android", "ios"]
  }
}
```

`NOT_IN`로 반대 표현:

```json
{
  "type": "compare", "attribute": "device.store", "operator": "NOT_IN", "value": ["onestore", "web"]
}
```

### 11. LIST_STRING 속성 사용 — 특정 운세 태그 열람 사용자

```json
{
  "id": 11, "version": 1,
  "root": {
    "type": "compare",
    "attribute": "item.seenFortuneTags",
    "operator": "CONTAINS",
    "value": "love"
  }
}
```

### 12. 결제 패턴 — 이번 달 3회 이상 결제

```json
{
  "id": 12, "version": 1,
  "root": {
    "type": "compare",
    "attribute": "action.monthChargeCount",
    "operator": "GTE",
    "value": 3
  }
}
```

### 13. 생일 사용자 + 푸시 허용

```json
{
  "id": 13, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "post.isBirthdayToday",       "operator": "EQ", "value": true },
      { "type": "compare", "attribute": "device.appPushPermission",   "operator": "EQ", "value": true }
    ]
  },
  "meta": { "label": "생일자 푸시 캠페인" }
}
```

### 14. 프리미엄 사용자 — 운명의 책 보유 + 30일 내 프리미엄 구매

```json
{
  "id": 14, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "premium.fatebookCount",  "operator": "GTE", "value": 1 },
      { "type": "compare", "attribute": "action.hasPremiumIn30Days", "operator": "EQ", "value": true }
    ]
  }
}
```

### 15. 복합 자격 판정 — 친구 5명 이상, 미사용 선물 보유, 이번 달 미결제

```json
{
  "id": 15, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "friendship.total",      "operator": "GTE", "value": 5 },
      { "type": "compare", "attribute": "gift.hasUnusedGift",    "operator": "EQ",  "value": true },
      { "type": "compare", "attribute": "action.monthChargeCount", "operator": "EQ",  "value": 0 }
    ]
  },
  "meta": { "label": "친구활동 + 선물보유 + 미결제 (선물 사용 유도)" }
}
```

### 16. 시간 범위 — 평일 점심시간 (월~금, 11-14시)

```json
{
  "id": 16, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "now.dayOfWeek", "operator": "BETWEEN", "value": [1, 5] },
      { "type": "compare", "attribute": "now.kstHour",   "operator": "BETWEEN", "value": [11, 14] }
    ]
  }
}
```

### 17. 별자리 + 띠 매칭 — 특정 운세 콘텐츠 타겟팅

```json
{
  "id": 17, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "user.zodiac", "operator": "IN", "value": [0, 1, 2] },
      { "type": "compare", "attribute": "user.animal", "operator": "EQ", "value": 0 }
    ]
  },
  "meta": { "label": "양자리/황소자리/쌍둥이자리 + 쥐띠" }
}
```

### 18. 충전 후 결제 유도 — 보유 포스가 적은 결제 경험자

```json
{
  "id": 18, "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "force.total",         "operator": "LTE", "value": 100 },
      { "type": "compare", "attribute": "action.hasCharged",   "operator": "EQ",  "value": true },
      { "type": "compare", "attribute": "action.daysAfterLastCharge", "operator": "GTE", "value": 7 }
    ]
  }
}
```

---

### 작성 팁

1. **`id` 필드**: Rule의 고유 ID(양의 Long). 어드민에서 자동 채번 권장.
2. **`version`**: 룰 변경 시 증가. 같은 id가 진화한 흔적.
3. **노드의 `id` (Compare 안)**: 옵션이지만 trace 디버깅 시 큰 도움. `c1`, `c2`처럼 의미있는 식별자 부여.
4. **`meta`**: 평가에 영향 없는 부가 정보. 어드민 라벨/설명/작성자 등을 담기 좋음.
5. **빈 `and`/`or`**: `and: []` → 항상 true, `or: []` → 항상 false. 의도치 않은 빈 배열 주의.
6. **타입 일치**: `value`의 JSON 타입이 attribute spec과 일치해야 함. 예: INTEGER 속성에 문자열 비교는 실패.
7. **`BETWEEN`은 양 끝 포함**: `[20, 39]` → 20, 39 모두 매칭.
8. **테스트**: 어드민에 등록 전 `/api/internal/targeting/simulate` 엔드포인트로 시뮬레이션 권장.

---

## 패키지 구조

```
co.un7qi3.targeting/
├── core/
│   ├── attribute/    AttributeSpec, Type, Status, Provider, Registry, Bag
│   ├── evaluation/   EvaluationInput, MapEvaluationInput
│   ├── rule/         Rule, RuleNode(And/Or/Not/Compare), Operator, RuleNodes
│   ├── evaluator/    RuleEvaluator, EvaluationResult, TraceEntry
│   └── error/        RuleValidationException
├── engine/
│   ├── tree/         TreeRuleEvaluator (재귀 트리 평가)
│   ├── serde/        RuleJsonMapper, TargetingJacksonModule
│   └── validate/     RuleValidator
└── host/
    ├── Targeting              (Facade — 라이브러리 진입점)
    ├── InputAssembler<S>      (host 도메인 → EvaluationInput)
    └── TargetingResolver<S>   (호스트가 사용하는 평가 entry point)
```

- `core/`: 라이브러리의 타입/인터페이스 (불변)
- `engine/`: 기본 구현체 (트리 평가, JSON, 검증)
- `host/`: 호스트가 직접 다루는 facade와 wrapper

---

## 좌표

- Group: `co.un7qi3`
- Artifact: `forceteller-targeting`
- 배포: GitHub Packages

### 의존성 추가

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/un7qi3/forceteller-targeting")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("co.un7qi3:forceteller-targeting:0.1.0-SNAPSHOT")
}
```

### 빌드

```bash
./gradlew build
./gradlew publishToMavenLocal      # 로컬 시험
./gradlew publish                  # GitHub Packages
```

---

## 철학 한 줄

> **"평가 단위는 호스트가, 룰은 데이터로, 결과는 결정적으로(deterministic)."**
>
> 라이브러리는 호스트 도메인을 모르고, host 도메인은 룰 표현을 모른다.
> 둘 사이의 다리만 명확하게 정의해두면 — 콘텐츠 노출/푸시/이벤트/할인 등
> "조건부 의사결정"이 필요한 모든 곳에서 같은 인프라를 재사용할 수 있다.
