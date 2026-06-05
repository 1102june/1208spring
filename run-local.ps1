# 로컬 개발 서버 실행 스크립트
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "WiseYoung 로컬 서버 시작" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 환경 변수 (Gemini 챗봇용 — 로그인 API만 쓸 때는 없어도 서버는 기동됨)
$env:ENCRYPTION_KEY = 'TksJPfalBU2+pIU/A87y1nlswEHECggq5hS6JOOGZ2M='
$env:GEMINI_API_KEY = 'DDc7Csizrex/VD+7vXQUHQf1LHT4Zlyjm6OskFedbaMfFGyEKtHQmdN/iMpsCL4I'

# DB (기본값: application.yml — root / 1234 / wise_young)
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "1234" }

Write-Host "[확인] ENCRYPTION_KEY: 설정됨" -ForegroundColor Green
Write-Host "[확인] GEMINI_API_KEY: 설정됨" -ForegroundColor Green
Write-Host "[확인] DB: $($env:DB_USERNAME)@127.0.0.1:3306/wise_young" -ForegroundColor Green
Write-Host ""

# MariaDB 포트 확인
$maria = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
if (-not $maria) {
    Write-Host "경고: MariaDB(3306)가 listening 상태가 아닙니다. 서버 기동이 실패할 수 있습니다." -ForegroundColor Yellow
}

# 8080 이미 사용 중이면 안내
$port8080 = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($port8080) {
    Write-Host "경고: 8080 포트가 이미 사용 중입니다 (PID $($port8080.OwningProcess))." -ForegroundColor Yellow
    Write-Host "       기존 서버를 종료하거나 다른 포트를 사용하세요." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "서버 시작: http://127.0.0.1:8080/api/health" -ForegroundColor Cyan
Write-Host ""

.\gradlew.bat bootRun --no-daemon
