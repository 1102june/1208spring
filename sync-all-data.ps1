# MariaDB 동기화 스크립트
# 사용법: .\sync-all-data.ps1

$baseUrl = "http://localhost:8080"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "MariaDB 데이터 동기화 시작" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Policy 동기화
Write-Host "[1/3] Policy 데이터 동기화..." -ForegroundColor Yellow
try {
    $response1 = Invoke-WebRequest -Uri "$baseUrl/api/policy/sync" -Method POST -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
    Write-Host "✓ 성공 (상태 코드: $($response1.StatusCode))" -ForegroundColor Green
    Write-Host $response1.Content
} catch {
    Write-Host "✗ 실패: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 2. Housing Notice 동기화
Write-Host "[2/3] Housing Notice 데이터 동기화..." -ForegroundColor Yellow
try {
    $response2 = Invoke-WebRequest -Uri "$baseUrl/api/housing/notice/sync" -Method POST -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
    Write-Host "✓ 성공 (상태 코드: $($response2.StatusCode))" -ForegroundColor Green
    Write-Host $response2.Content
} catch {
    Write-Host "✗ 실패: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 3. Housing Complex 동기화
Write-Host "[3/3] Housing Complex 데이터 동기화..." -ForegroundColor Yellow
try {
    $response3 = Invoke-WebRequest -Uri "$baseUrl/api/housing/complex/sync" -Method POST -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
    Write-Host "✓ 성공 (상태 코드: $($response3.StatusCode))" -ForegroundColor Green
    Write-Host $response3.Content
} catch {
    Write-Host "✗ 실패: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "동기화 요청 완료" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "참고: 동기화는 백그라운드에서 실행됩니다." -ForegroundColor Gray
Write-Host "서버 로그를 확인하여 진행 상황을 확인하세요." -ForegroundColor Gray







