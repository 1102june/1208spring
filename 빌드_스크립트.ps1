# WiseYoung 백엔드 빌드 스크립트 (Windows PowerShell)

Write-Host "=== WiseYoung 백엔드 빌드 시작 ===" -ForegroundColor Cyan

# 프로젝트 디렉토리로 이동
$projectDir = "C:\Users\subpa\IdeaProjects\WiseYoung_backend"
Set-Location $projectDir

# 이전 빌드 결과물 삭제
Write-Host "`n[1/4] 이전 빌드 결과물 삭제 중..." -ForegroundColor Yellow
if (Test-Path "build") {
    Remove-Item -Recurse -Force "build"
}
Write-Host "✅ 삭제 완료" -ForegroundColor Green

# 테스트 제외하고 빌드
Write-Host "`n[2/4] JAR 파일 빌드 중..." -ForegroundColor Yellow
& .\gradlew.bat clean build -x test

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 빌드 실패!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ 빌드 완료" -ForegroundColor Green

# 빌드된 JAR 파일 확인
Write-Host "`n[3/4] 빌드 결과 확인 중..." -ForegroundColor Yellow
$jarFile = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1

if ($jarFile) {
    Write-Host "✅ JAR 파일 생성 완료: $($jarFile.Name)" -ForegroundColor Green
    Write-Host "   위치: $($jarFile.FullName)" -ForegroundColor Gray
    Write-Host "   크기: $([math]::Round($jarFile.Length / 1MB, 2)) MB" -ForegroundColor Gray
} else {
    Write-Host "❌ JAR 파일을 찾을 수 없습니다!" -ForegroundColor Red
    exit 1
}

# 빌드 결과 요약
Write-Host "`n[4/4] 빌드 결과 요약" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "JAR 파일: $($jarFile.Name)" -ForegroundColor White
Write-Host "경로: $($jarFile.FullName)" -ForegroundColor White
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

Write-Host "`n=== 빌드 완료! ===" -ForegroundColor Green
Write-Host "`n다음 단계:" -ForegroundColor Cyan
Write-Host "1. JAR 파일을 서버로 업로드" -ForegroundColor White
Write-Host "2. 서버에서 환경 변수 설정" -ForegroundColor White
Write-Host "3. 서비스 시작" -ForegroundColor White

