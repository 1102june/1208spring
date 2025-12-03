# WiseYoung 백엔드 배포 스크립트 (Windows PowerShell)

param(
    [string]$ServerIP = "210.104.76.139",
    [string]$ServerUser = "root",
    [string]$ServerPath = "/home/root/wiseyoung-backend",
    [string]$JarName = "app.jar"
)

Write-Host "=== WiseYoung 백엔드 배포 시작 ===" -ForegroundColor Cyan

# 프로젝트 디렉토리로 이동
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

# 1. 빌드
Write-Host "`n[1/4] JAR 파일 빌드 중..." -ForegroundColor Yellow
& .\gradlew.bat clean build -x test

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 빌드 실패!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ 빌드 완료" -ForegroundColor Green

# 2. JAR 파일 확인
$jarFile = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1

if (-not $jarFile) {
    Write-Host "❌ JAR 파일을 찾을 수 없습니다!" -ForegroundColor Red
    exit 1
}

Write-Host "`n[2/4] JAR 파일 확인: $($jarFile.Name)" -ForegroundColor Green

# 3. 서버로 업로드
Write-Host "`n[3/4] 서버로 JAR 파일 업로드 중..." -ForegroundColor Yellow
Write-Host "   서버: $ServerUser@$ServerIP" -ForegroundColor Gray
Write-Host "   경로: $ServerPath/$JarName" -ForegroundColor Gray

$scpCommand = "scp `"$($jarFile.FullName)`" ${ServerUser}@${ServerIP}:${ServerPath}/${JarName}"
Invoke-Expression $scpCommand

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 업로드 실패!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ 업로드 완료" -ForegroundColor Green

# 4. 서버에서 서비스 재시작
Write-Host "`n[4/4] 서버에서 서비스 재시작 중..." -ForegroundColor Yellow

$restartCommand = @"
ssh ${ServerUser}@${ServerIP} 'sudo systemctl stop wiseyoung-backend && sudo systemctl start wiseyoung-backend && sleep 3 && sudo systemctl status wiseyoung-backend --no-pager'
"@

Invoke-Expression $restartCommand

Write-Host "`n=== 배포 완료! ===" -ForegroundColor Green
Write-Host "`n서버 상태 확인:" -ForegroundColor Cyan
Write-Host "  ssh ${ServerUser}@${ServerIP}" -ForegroundColor White
Write-Host "  sudo systemctl status wiseyoung-backend" -ForegroundColor White
Write-Host "  sudo journalctl -u wiseyoung-backend -f" -ForegroundColor White

