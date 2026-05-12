# forceteller-targeting

Targeting rule engine — a domain-agnostic library for evaluating boolean rule trees against attribute-based candidates.

## 좌표

- Group: `co.un7qi3`
- Artifact: `forceteller-targeting`
- 배포: GitHub Packages

## 사용

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

## 구조

```
co.un7qi3.targeting/
├── core/
│   ├── rule/        Rule, RuleNode, Operator
│   ├── candidate/   TargetingCandidate, AttributeBag, MapCandidate
│   ├── attribute/   AttributeSpec, AttributeProvider, AttributeCatalog
│   ├── evaluator/   RuleEvaluator, EvaluationResult, TraceEntry
│   └── error/
└── engine/
    ├── tree/        TreeRuleEvaluator
    ├── validate/    RuleValidator
    └── serde/       RuleJsonMapper
```

## 빌드

```bash
./gradlew build
./gradlew publishToMavenLocal      # 로컬 시험
./gradlew publish                  # GitHub Packages
```

## 설계 문서

이 라이브러리의 배경·결정 사항은 forceteller-api 레포의 `docs/targeting-refactor.md` 참고.
