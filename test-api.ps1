# API 테스트 스크립트 (PowerShell)
# 사용법: .\test-api.ps1

$baseUrl = "http://localhost:8080"
$userId = "test-user"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Youth API 테스트 시작" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 서버 상태 확인
Write-Host "[1] 서버 상태 확인 (주택 동기화 상태)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/admin/housing/stats" -Method Get
    Write-Host "✅ 서버 연결 성공" -ForegroundColor Green
    Write-Host "   - 공고문 수: $($response.data.housingNoticeCount)" -ForegroundColor Gray
    Write-Host "   - 단지정보 수: $($response.data.housingComplexCount)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ 서버 연결 실패: $_" -ForegroundColor Red
    Write-Host "   서버가 실행 중인지 확인하세요." -ForegroundColor Red
    exit 1
}

# 2. 활성 주택 목록 조회
Write-Host "[2] 활성 주택 목록 조회..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/housing/active?userId=$userId" -Method Get -Headers @{"X-User-Id"=$userId}
    $count = $response.data.Count
    Write-Host "✅ 주택 목록 조회 성공: $count 건" -ForegroundColor Green
    if ($count -gt 0) {
        Write-Host "   첫 번째 주택: $($response.data[0].name)" -ForegroundColor Gray
    }
    Write-Host ""
} catch {
    Write-Host "❌ 주택 목록 조회 실패: $_" -ForegroundColor Red
    Write-Host ""
}

# 3. 활성 정책 목록 조회
Write-Host "[3] 활성 정책 목록 조회..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/policy/active?userId=$userId" -Method Get
    $count = $response.data.Count
    Write-Host "✅ 정책 목록 조회 성공: $count 건" -ForegroundColor Green
    if ($count -gt 0) {
        Write-Host "   첫 번째 정책: $($response.data[0].title)" -ForegroundColor Gray
    }
    Write-Host ""
} catch {
    Write-Host "❌ 정책 목록 조회 실패: $_" -ForegroundColor Red
    Write-Host ""
}

# 4. 메인 페이지 데이터 조회
Write-Host "[4] 메인 페이지 데이터 조회..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/main" -Method Get -Headers @{"X-User-Id"=$userId}
    Write-Host "✅ 메인 페이지 데이터 조회 성공" -ForegroundColor Green
    Write-Host "   - 읽지 않은 알림: $($response.data.unreadNotificationCount) 개" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ 메인 페이지 데이터 조회 실패: $_" -ForegroundColor Red
    Write-Host ""
}

# 5. OTP 이메일 중복 확인
Write-Host "[5] OTP 이메일 중복 확인..." -ForegroundColor Yellow
try {
    $testEmail = "test@example.com"
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/otp/email/check?email=$testEmail" -Method Get
    $isDuplicate = $response.data
    if ($isDuplicate) {
        Write-Host "✅ 이메일 중복 확인 성공: 이미 사용 중" -ForegroundColor Green
    } else {
        Write-Host "✅ 이메일 중복 확인 성공: 사용 가능" -ForegroundColor Green
    }
    Write-Host ""
} catch {
    Write-Host "❌ 이메일 중복 확인 실패: $_" -ForegroundColor Red
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "테스트 완료!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

