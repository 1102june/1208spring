# Gmail SMTP 환경 변수 설정 및 확인 스크립트

Write-Host "=== Gmail SMTP 환경 변수 설정 ===" -ForegroundColor Green
Write-Host ""

# 현재 환경 변수 확인
Write-Host "현재 설정된 환경 변수:" -ForegroundColor Yellow
Write-Host "EMAIL_USERNAME: $env:EMAIL_USERNAME"
Write-Host "EMAIL_PASSWORD: $($env:EMAIL_PASSWORD -replace '.', '*')"  # 비밀번호는 마스킹
Write-Host ""

# 환경 변수 설정
$emailUsername = Read-Host "Gmail 주소를 입력하세요 (예: your-email@gmail.com)"
$emailPassword = Read-Host "앱 비밀번호를 입력하세요 (16자리, 공백 제거)" -AsSecureString
$emailPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($emailPassword))

# 현재 세션에 설정
$env:EMAIL_USERNAME = $emailUsername
$env:EMAIL_PASSWORD = $emailPasswordPlain

Write-Host ""
Write-Host "✅ 환경 변수가 현재 세션에 설정되었습니다!" -ForegroundColor Green
Write-Host ""
Write-Host "설정된 값:" -ForegroundColor Yellow
Write-Host "EMAIL_USERNAME: $env:EMAIL_USERNAME"
Write-Host "EMAIL_PASSWORD: $($env:EMAIL_PASSWORD -replace '.', '*')"
Write-Host ""

# 영구 설정 여부 확인
$permanent = Read-Host "영구적으로 설정하시겠습니까? (Y/N)"
if ($permanent -eq "Y" -or $permanent -eq "y") {
    [System.Environment]::SetEnvironmentVariable("EMAIL_USERNAME", $emailUsername, "User")
    [System.Environment]::SetEnvironmentVariable("EMAIL_PASSWORD", $emailPasswordPlain, "User")
    Write-Host "✅ 환경 변수가 영구적으로 설정되었습니다!" -ForegroundColor Green
    Write-Host "   (새 터미널 창을 열면 자동으로 적용됩니다)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 다음 단계 ===" -ForegroundColor Green
Write-Host "1. 이 터미널에서 Spring Boot 서버를 실행하세요"
Write-Host "2. 서버 시작 시 '✅ Gmail SMTP 설정 완료' 메시지가 보여야 합니다"
Write-Host ""

