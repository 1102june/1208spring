# 로컬 환경 변수 수동 설정 (PowerShell)
# 이 스크립트를 실행한 후 별도 터미널에서 스프링 부트를 실행하세요

Write-Host "환경 변수 설정 중..." -ForegroundColor Yellow

# 작은따옴표 사용 (특수문자 처리)
$env:ENCRYPTION_KEY = 'qm9MfSTvjn8IvepsCn0cAqKXLxm+Nf6da40n0kMN2e4='
$env:GEMINI_API_KEY = 'iFfWIWG6SuICN30uyNLwZsh26/+DD9k7/tbBtCNSDycIve0IyNB2DrXSZTLWCC4B'

Write-Host "✓ 환경 변수 설정 완료" -ForegroundColor Green
Write-Host ""
Write-Host "환경 변수 확인:" -ForegroundColor Cyan
Write-Host "ENCRYPTION_KEY: $env:ENCRYPTION_KEY" -ForegroundColor Gray
Write-Host "GEMINI_API_KEY: $env:GEMINI_API_KEY" -ForegroundColor Gray
Write-Host ""
Write-Host "이제 새 터미널에서 다음 명령어를 실행하세요:" -ForegroundColor Yellow
Write-Host "  .\gradlew bootRun" -ForegroundColor White
Write-Host ""
Write-Host "또는 현재 터미널에서:" -ForegroundColor Yellow
Write-Host "  .\gradlew bootRun" -ForegroundColor White
Write-Host ""

