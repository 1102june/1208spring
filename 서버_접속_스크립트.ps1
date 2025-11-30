# KT Cloud 서버 접속 및 배포 스크립트

$ServerIP = "210.104.76.139"
$ServerUser = "root"
$ServerPassword = "Ahk3678@ as!@ #"
$JarPath = "build\libs\youth-0.0.1-SNAPSHOT.jar"
$RemotePath = "/home/root/wiseyoung-backend/app.jar"

Write-Host "=== WiseYoung 서버 배포 도구 ===" -ForegroundColor Cyan
Write-Host ""

function Show-Menu {
    Write-Host "선택하세요:" -ForegroundColor Yellow
    Write-Host "1. 서버 접속 (SSH)" -ForegroundColor White
    Write-Host "2. JAR 파일 업로드" -ForegroundColor White
    Write-Host "3. 서버 상태 확인" -ForegroundColor White
    Write-Host "4. 서비스 재시작" -ForegroundColor White
    Write-Host "5. 로그 확인" -ForegroundColor White
    Write-Host "6. 전체 배포 (빌드 + 업로드 + 재시작)" -ForegroundColor Green
    Write-Host "0. 종료" -ForegroundColor Red
    Write-Host ""
}

function Connect-Server {
    Write-Host "서버 접속 중..." -ForegroundColor Yellow
    Write-Host "서버: $ServerUser@$ServerIP" -ForegroundColor Gray
    Write-Host ""
    Write-Host "비밀번호: $ServerPassword" -ForegroundColor Gray
    Write-Host ""
    
    # SSH 접속 (Windows에서 plink 또는 ssh 사용)
    # plink가 설치되어 있다면
    if (Get-Command plink -ErrorAction SilentlyContinue) {
        echo y | plink -ssh $ServerUser@$ServerIP -pw $ServerPassword
    } else {
        Write-Host "SSH 접속을 위해 다음 명령어를 실행하세요:" -ForegroundColor Yellow
        Write-Host "ssh $ServerUser@$ServerIP" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "비밀번호: $ServerPassword" -ForegroundColor Gray
    }
}

function Upload-Jar {
    if (-not (Test-Path $JarPath)) {
        Write-Host "❌ JAR 파일을 찾을 수 없습니다: $JarPath" -ForegroundColor Red
        Write-Host "먼저 빌드를 실행하세요." -ForegroundColor Yellow
        return
    }
    
    Write-Host "JAR 파일 업로드 중..." -ForegroundColor Yellow
    Write-Host "로컬 파일: $JarPath" -ForegroundColor Gray
    Write-Host "서버 경로: $ServerUser@$ServerIP`:$RemotePath" -ForegroundColor Gray
    Write-Host ""
    
    # SCP로 파일 업로드
    try {
        # PSCP 사용 (PuTTY가 설치되어 있다면)
        if (Get-Command pscp -ErrorAction SilentlyContinue) {
            echo y | pscp -pw $ServerPassword $JarPath "$ServerUser@$ServerIP`:$RemotePath"
        } else {
            # OpenSSH 사용
            Write-Host "SCP를 사용하여 업로드합니다..." -ForegroundColor Yellow
            scp $JarPath "$ServerUser@$ServerIP`:$RemotePath"
        }
        Write-Host "✅ 업로드 완료!" -ForegroundColor Green
    } catch {
        Write-Host "❌ 업로드 실패: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "수동 업로드 방법:" -ForegroundColor Yellow
        Write-Host "scp $JarPath $ServerUser@$ServerIP`:$RemotePath" -ForegroundColor Cyan
    }
}

function Check-ServerStatus {
    Write-Host "서버 상태 확인 중..." -ForegroundColor Yellow
    
    # SSH로 명령 실행
    Write-Host ""
    Write-Host "다음 명령어를 서버에서 실행하세요:" -ForegroundColor Yellow
    Write-Host "sudo systemctl status wiseyoung-backend" -ForegroundColor Cyan
    Write-Host "sudo journalctl -u wiseyoung-backend -n 20" -ForegroundColor Cyan
    Write-Host ""
    
    # 간단한 연결 테스트
    try {
        $response = Invoke-WebRequest -Uri "http://$ServerIP:8080/api/health" -TimeoutSec 5 -ErrorAction Stop
        Write-Host "✅ 서버 응답 성공 (상태 코드: $($response.StatusCode))" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ 서버에 연결할 수 없습니다. 서비스가 실행 중인지 확인하세요." -ForegroundColor Yellow
    }
}

function Restart-Service {
    Write-Host "서비스 재시작 중..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "서버에서 다음 명령어를 실행하세요:" -ForegroundColor Yellow
    Write-Host "sudo systemctl restart wiseyoung-backend" -ForegroundColor Cyan
    Write-Host "sudo systemctl status wiseyoung-backend" -ForegroundColor Cyan
}

function Show-Logs {
    Write-Host "서버 로그 확인" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "서버에서 다음 명령어를 실행하세요:" -ForegroundColor Yellow
    Write-Host "sudo journalctl -u wiseyoung-backend -f" -ForegroundColor Cyan
    Write-Host "또는" -ForegroundColor Gray
    Write-Host "tail -f /home/root/wiseyoung-backend/logs/app.log" -ForegroundColor Cyan
}

function Deploy-All {
    Write-Host "=== 전체 배포 프로세스 시작 ===" -ForegroundColor Cyan
    Write-Host ""
    
    # 1. 빌드
    Write-Host "[1/3] JAR 파일 빌드 중..." -ForegroundColor Yellow
    & .\gradlew.bat clean build -x test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 빌드 실패!" -ForegroundColor Red
        return
    }
    Write-Host "✅ 빌드 완료" -ForegroundColor Green
    Write-Host ""
    
    # 2. 업로드
    Write-Host "[2/3] JAR 파일 업로드 중..." -ForegroundColor Yellow
    Upload-Jar
    Write-Host ""
    
    # 3. 재시작 안내
    Write-Host "[3/3] 서비스 재시작" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "서버에서 다음 명령어를 실행하여 서비스를 재시작하세요:" -ForegroundColor Yellow
    Write-Host "sudo systemctl restart wiseyoung-backend" -ForegroundColor Cyan
    Write-Host "sudo systemctl status wiseyoung-backend" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "=== 배포 완료! ===" -ForegroundColor Green
}

# 메인 루프
while ($true) {
    Show-Menu
    $choice = Read-Host "번호를 입력하세요"
    
    switch ($choice) {
        "1" { Connect-Server }
        "2" { Upload-Jar }
        "3" { Check-ServerStatus }
        "4" { Restart-Service }
        "5" { Show-Logs }
        "6" { Deploy-All }
        "0" { 
            Write-Host "종료합니다." -ForegroundColor Yellow
            break 
        }
        default { 
            Write-Host "잘못된 선택입니다." -ForegroundColor Red
        }
    }
    
    if ($choice -eq "0") { break }
    
    Write-Host ""
    Write-Host "계속하려면 아무 키나 누르세요..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    Write-Host ""
}

