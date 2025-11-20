# 공고문 API vs 단지정보 API 필드 매핑 분석

## 🔑 공통 필드 (두 API 모두에 있는 필드 - 매핑 키로 사용 가능)

| 필드명 | 공고문 API | 단지정보 API | 설명 |
|--------|-----------|-------------|------|
| **hsmpSn** | ✅ `hsmpSn` | ✅ `hsmpSn` | **단지 식별자** (매핑 키로 사용) |
| **hsmpNm** | ✅ `hsmpNm` | ✅ `hsmpNm` | **단지명** (매핑 키로 사용 가능) |

## 📊 Housing 엔티티 필드별 데이터 출처

| Housing 필드 | 공고문 API | 단지정보 API | 우선순위 |
|-------------|-----------|-------------|---------|
| **housingId** | `hsmpSn` | `hsmpSn` | 둘 다 동일 (단지 식별자) |
| **name** | `hsmpNm` | `hsmpNm` | 둘 다 동일 (단지명) |
| **address** | ❌ 없음 | ✅ `rnAdres` (도로명 주소) | 단지정보 API |
| **supplyArea** | ❌ 없음 | ✅ `suplyPrvuseAr` (공급 전용 면적) | 단지정보 API |
| **completeDate** | ❌ 없음 | ✅ `competDe` (준공 일자) | 단지정보 API |
| **organization** | ❌ 없음 | ✅ `insttNm` (기관명) | 단지정보 API |
| **applicationStart** | ✅ `panNtStDt` (공고게시일) | ❌ 없음 | 공고문 API |
| **applicationEnd** | ✅ `clsgDt` (공고마감일) | ❌ 없음 | 공고문 API |
| **heatingType** | ❌ 없음 | ✅ `heatMthdDetailNm` (난방 방식) | 단지정보 API |
| **elevator** | ❌ 없음 | ✅ `elvtrInstlAtNm` (승강기 설치여부) | 단지정보 API |
| **parkingSpaces** | ❌ 없음 | ✅ `parkngCo` (주차수) | 단지정보 API |
| **deposit** | ❌ 없음 | ✅ `bassRentGtn` (기본 임대보증금) | 단지정보 API |
| **monthlyRent** | ❌ 없음 | ✅ `bassMtRntchrg` (기본 월임대료) | 단지정보 API |
| **totalUnits** | ❌ 없음 | ✅ `hshldCo` (세대수) | 단지정보 API |
| **link** | ✅ `dtlUrl` (공고 상세 URL) | ❌ 없음 | 공고문 API |
| **housingType** | ✅ `aisTpCdNm` (공고유형명) | ✅ `suplyTyNm` (공급 유형명) | 둘 다 가능 (단지정보 우선) |

## 📝 결론

### 매핑 전략

1. **매핑 키**: `hsmpSn` (단지 식별자)를 사용하여 두 API의 데이터를 연결
   - 단지정보 API의 데이터를 기본으로 저장
   - 공고문 API의 데이터로 신청 기간, 링크, 주택유형 보완

2. **데이터 병합 우선순위**:
   - **단지정보 API 우선**: 주소, 면적, 준공일, 기관명, 난방, 엘리베이터, 주차, 보증금, 월세, 세대수 등
   - **공고문 API 보완**: 신청 시작일, 신청 종료일, 링크
   - **주택유형**: 단지정보 API의 `suplyTyNm` 우선, 없으면 공고문 API의 `aisTpCdNm` 사용

3. **현재 코드의 매핑 방식**:
   - `HousingSyncService.convertToHousing()`: 단지정보 API → Housing 변환
   - `HousingSyncService.updateHousingWithNotice()`: 공고문 API 데이터로 Housing 업데이트
   - 매핑 키: `hsmpSn` 또는 `hsmpNm` 사용

### 주의사항

- 공고문 API에는 `hsmpSn`이 없을 수 있음 (null 가능)
- 이 경우 `hsmpNm` (단지명)으로 매핑 시도
- 단지명이 정확히 일치하지 않을 수 있어 매핑 실패 가능성 있음

