# API 엔드포인트 테스트 스크립트
# 사용법: .\test-api-endpoints.ps1

$baseUrl = "http://localhost:8080"
# 서버가 다른 IP에서 실행 중이면 아래 주석을 해제하고 IP를 변경하세요
# $baseUrl = "http://192.168.123.163:8080"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "API 엔드포인트 테스트 시작" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Housing Complex API 테스트
Write-Host "[1/3] Housing Complex API 테스트..." -ForegroundColor Yellow
Write-Host "GET $baseUrl/api/housing/complex/api?pageNo=1&numOfRows=10" -ForegroundColor Gray
try {
    $response1 = Invoke-WebRequest -Uri "$baseUrl/api/housing/complex/api?pageNo=1&numOfRows=10" -Method GET -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
    Write-Host "✓ 성공 (상태 코드: $($response1.StatusCode))" -ForegroundColor Green
    Write-Host "응답 길이: $($response1.Content.Length) bytes" -ForegroundColor Gray
    Write-Host "응답 미리보기 (처음 500자):" -ForegroundColor Gray
    Write-Host $response1.Content.Substring(0, [Math]::Min(500, $response1.Content.Length)) -ForegroundColor White
} catch {
    Write-Host "✗ 실패: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 2. Housing Notice API 테스트
Write-Host "[2/3] Housing Notice API 테스트..." -ForegroundColor Yellow
Write-Host "GET $baseUrl/api/housing/notice/api?pageNo=1&numOfRows=10" -ForegroundColor Gray
try {
    $response2 = Invoke-WebRequest -Uri "$baseUrl/api/housing/notice/api?pageNo=1&numOfRows=10" -Method GET -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
    Write-Host "✓ 성공 (상태 코드: $($response2.StatusCode))" -ForegroundColor Green
    Write-Host "응답 길이: $($response2.Content.Length) bytes" -ForegroundColor Gray
    Write-Host "응답 미리보기 (처음 500자):" -ForegroundColor Gray
    Write-Host $response2.Content.Substring(0, [Math]::Min(500, $response2.Content.Length)) -ForegroundColor White
} catch {
    Write-Host "✗ 실패: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 3. Policy API 테스트
Write-Host "[3/3] Policy API 테스트..." -ForegroundColor Yellow
Write-Host "GET $baseUrl/api/policy/api?pageNum=1&pageSize=10" -ForegroundColor Gray
try {
    $response3 = Invoke-WebRequest -Uri "$baseUrl/api/policy/api?pageNum=1&pageSize=10" -Method GET -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
    Write-Host "✓ 성공 (상태 코드: $($response3.StatusCode))" -ForegroundColor Green
    Write-Host "응답 길이: $($response3.Content.Length) bytes" -ForegroundColor Gray
    Write-Host "응답 미리보기 (처음 500자):" -ForegroundColor Gray
    Write-Host $response3.Content.Substring(0, [Math]::Min(500, $response3.Content.Length)) -ForegroundColor White
} catch {
    Write-Host "✗ 실패: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "테스트 완료" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan












