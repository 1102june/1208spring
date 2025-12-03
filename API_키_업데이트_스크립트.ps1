# Gemini API 키 업데이트 스크립트
# 서버의 .env 파일에 새 API 키를 업데이트합니다.

param(
    [Parameter(Mandatory=$true)]
    [string]$ServerIP = "210.104.76.139",
    
    [Parameter(Mandatory=$true)]
    [string]$NewApiKey,
    
    [Parameter(Mandatory=$false)]
    [string]$ServerUser = "root"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Gemini API 키 업데이트" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 서버 .env 파일 백업
Write-Host "[1/4] 서버 .env 파일 백업 중..." -ForegroundColor Yellow
$backupCommand = "cp /home/root/wiseyoung-backend/.env /home/root/wiseyoung-backend/.env.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
ssh "${ServerUser}@${ServerIP}" $backupCommand
Write-Host "✓ 백업 완료" -ForegroundColor Green
Write-Host ""

# 2. 서버에서 현재 .env 파일 내용 확인
Write-Host "[2/4] 현재 .env 파일 내용 확인 중..." -ForegroundColor Yellow
ssh "${ServerUser}@${ServerIP}" "cat /home/root/wiseyoung-backend/.env | grep GEMINI_API_KEY"
Write-Host ""

# 3. 새 API 키로 업데이트
Write-Host "[3/4] 새 API 키로 업데이트 중..." -ForegroundColor Yellow
$updateCommand = "sed -i 's/^GEMINI_API_KEY=.*/GEMINI_API_KEY=$NewApiKey/' /home/root/wiseyoung-backend/.env"
ssh "${ServerUser}@${ServerIP}" $updateCommand
Write-Host "✓ 업데이트 완료" -ForegroundColor Green
Write-Host ""

# 4. 업데이트 확인
Write-Host "[4/4] 업데이트 확인 중..." -ForegroundColor Yellow
ssh "${ServerUser}@${ServerIP}" "cat /home/root/wiseyoung-backend/.env | grep GEMINI_API_KEY"
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "API 키 업데이트 완료!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Yellow
Write-Host "1. 서버에서 애플리케이션 재시작:" -ForegroundColor White
Write-Host "   ssh ${ServerUser}@${ServerIP}" -ForegroundColor Gray
Write-Host "   sudo systemctl restart wiseyoung-backend" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 서비스 상태 확인:" -ForegroundColor White
Write-Host "   sudo systemctl status wiseyoung-backend" -ForegroundColor Gray
Write-Host ""
Write-Host "3. 로그 확인 (API 키 로드 확인):" -ForegroundColor White
Write-Host "   sudo journalctl -u wiseyoung-backend -f | grep -i 'gemini\|api key'" -ForegroundColor Gray
Write-Host ""

