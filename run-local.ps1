# 로컬 개발 서버 실행 스크립트
# 환경 변수를 설정하고 스프링 부트를 실행합니다.

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "로컬 개발 서버 시작" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 환경 변수 설정
Write-Host "[1/2] 환경 변수 설정 중..." -ForegroundColor Yellow

# ⚠️ 보안 주의: API 키는 환경 변수로만 설정하세요.
# 실제 운영 환경에서는 .env 파일이나 시스템 환경 변수를 사용하세요.
# 특수문자가 포함된 경우 작은따옴표 사용
$env:ENCRYPTION_KEY = 'TksJPfalBU2+pIU/A87y1nlswEHECggq5hS6JOOGZ2M='
$env:GEMINI_API_KEY = '9HZFLOrxj42v41LUObdcdbe9tN7NkhSZYESgpXDyCyfLDft85vfS7+flo7ylq3KC'

Write-Host "✓ ENCRYPTION_KEY: 설정됨" -ForegroundColor Green
Write-Host "✓ GEMINI_API_KEY: 설정됨" -ForegroundColor Green
Write-Host ""

# 환경 변수 확인
if ([string]::IsNullOrEmpty($env:ENCRYPTION_KEY)) {
    Write-Host "❌ ENCRYPTION_KEY가 설정되지 않았습니다!" -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrEmpty($env:GEMINI_API_KEY)) {
    Write-Host "❌ GEMINI_API_KEY가 설정되지 않았습니다!" -ForegroundColor Red
    exit 1
}

# 스프링 부트 실행
Write-Host "[2/2] 스프링 부트 서버 시작 중..." -ForegroundColor Yellow
Write-Host ""

.\gradlew bootRun

