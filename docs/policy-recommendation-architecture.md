# 정책 추천 아키텍처 (Before / After)

## Before — 요청마다 실시간 전량 계산

```
[클라이언트] GET /api/policy/recommended
       │
       ▼
PolicyService.getPersonalizedPolicies()
       │
       ├─ UserService → 프로필(나이·지역·관심)
       ├─ PolicyRepository → 활성 정책 전체 (~1,000+건)
       └─ 매 요청마다 for-loop 점수 계산 + 정렬 + Top N 반환
```

**문제**
- API 호출·메인 진입마다 동일 계산 반복
- 정책 sync 후에도 캐시 없음 → 데이터는 바뀌었는데 순위는 요청 시점에만 반영
- 가중치 공식과 저장 결과가 섞여 있지 않아 튜닝·디버깅이 어려움

---

## After — 공식은 코드, Top-K만 DB

```
┌─────────────────────────────────────────────────────────────┐
│  주 1회 (일 02:00) PolicySyncScheduler                       │
│    1) 온통청년 API → policy 테이블 upsert                    │
│    2) PolicyPreprocessorService → hasApplicationLink 등     │
│    3) UserPolicyRecommendationService.recomputeAllUsers()   │
│         → 사용자별 Top-35 → user_policy_recommendation      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  프로필 저장 (UserService.saveOrUpdateProfile)               │
│    → refreshForUserAsync(userId)  // 해당 user Top-K만 재계산 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  GET /api/policy/recommended (category 없음)                 │
│    → user_policy_recommendation 조회 → Top N 반환            │
│    → 캐시 없으면 1회 recomputeForUser 후 저장                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  GET /api/policy/recommended?category=...                    │
│    → PolicyScoringService 실시간 계산 (캐시 미사용)          │
└─────────────────────────────────────────────────────────────┘
```

**임대주택**: `HousingDataScheduler` — 매주 일요일 03:00 (`syncLHRentalHouseData`)

---

## 테이블 역할

| 테이블 | 역할 |
|--------|------|
| `policy` | 온통청년 원본 + 전처리(`has_application_link`, `preprocessed_at`) |
| `user_policy_recommendation` | **user별 Top-K만** (policy_id, score, rank_order, computed_at) |
| `user_profile` / `interest_category` | 점수 계산 입력 (변경 시 user 캐시만 갱신) |

---

## 가중치 공식 (단일 소스: `PolicyScoringService`)

| 항목 | 점수 |
|------|------|
| 기본 | +10 |
| 나이 범위 일치 | +30 / 불일치 -5 / 제한 없음 +15 |
| region 필드 매칭 | +20 / 전국 +10 |
| title에 지역 키워드 | +40 |
| summary에 지역 키워드 | +40 |
| 관심 카테고리 일치 | +20 |
| 마감 임박 (0~10일) | +0~+10 |
| link1·link2 모두 없음 | -10 |

---

## 왜 이렇게 하는가

1. **읽기 API가 가벼워짐** — 대부분의 `/recommended`, `/main`은 Top-K SELECT만 수행.
2. **정책 데이터와 추천 결과 분리** — sync로 policy만 갱신하고, 배치로 Top-K를 한 번에 맞춤.
3. **프로필 변경 반응** — 전체 user가 아니라 **변경한 user 1명**만 재계산 (비용 O(정책 수) × 1).
4. **가중치 튜닝 용이** — `PolicyScoringService`만 수정 후 배치/프로필 갱신으로 전파.
5. **저장 공간 절약** — user×policy 전체 점수 행렬 대신 user당 최대 35행.

---

## 수동 실행

- 정책 sync + 후처리: `POST /api/policy/sync` 또는 `POST /api/admin/policy/sync`
- 임대주택 sync: `POST /api/admin/housing/sync`
