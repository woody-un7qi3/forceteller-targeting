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

라이브러리는 Provider들을 모아 `AttributeCatalog`를 구성하고, 평가 시 필요한 Provider만 호출.

### `Targeting` (Facade)

라이브러리 진입점을 한 클래스에 모음. host는 IDE 자동완성으로 "필요한 부품"을 한눈에 발견:

```java
Targeting.defaultEvaluator()                       // RuleEvaluator
Targeting.jsonMapper()                             // RuleJsonMapper
Targeting.jacksonModule()                          // Jackson Module
Targeting.catalog(providers)                       // AttributeCatalog
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
2. **빈 등록 6개** (Spring 호스트 예시):

```java
@Configuration
public class TargetingConfig {
    @Bean public RuleEvaluator evaluator()        { return Targeting.defaultEvaluator(); }
    @Bean public RuleJsonMapper jsonMapper()      { return Targeting.jsonMapper(); }
    @Bean public Module jacksonModule()           { return Targeting.jacksonModule(); }

    @Bean public AttributeCatalog catalog(List<AttributeProvider<User>> ps) {
        return Targeting.catalog(ps);
    }
    @Bean public InputAssembler<User> assembler(List<AttributeProvider<User>> ps) {
        return Targeting.assembler(ps, u -> String.valueOf(u.getId()));
    }
    @Bean public TargetingResolver<User> resolver(InputAssembler<User> a, RuleEvaluator e) {
        return Targeting.resolver(a, e);
    }
}
```

평가 호출:
```java
EvaluationResult result = targetingResolver.evaluate(rule, user);
if (result.matched()) { ... }
```

---

## JSON 룰 예시

```json
{
  "id": 42,
  "version": 1,
  "root": {
    "type": "and",
    "children": [
      { "type": "compare", "attribute": "user.gender",     "operator": "EQ",      "value": "F" },
      { "type": "compare", "attribute": "user.age",        "operator": "BETWEEN", "value": [20, 39] },
      { "type": "compare", "attribute": "device.platform", "operator": "EQ",      "value": "android" },
      { "type": "compare", "attribute": "now.kstHour",     "operator": "GTE",     "value": 18 }
    ]
  }
}
```

`RuleJsonMapper.fromJson(json)` 으로 `Rule` 인스턴스 생성, 그대로 `evaluate()`에 전달.

---

## 패키지 구조

```
co.un7qi3.targeting/
├── core/
│   ├── attribute/    AttributeSpec, Type, Status, Provider, Catalog, Bag
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
