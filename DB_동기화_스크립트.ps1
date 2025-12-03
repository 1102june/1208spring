# WiseYoung 데이터베이스 동기화 스크립트 (로컬 → KT Cloud 서버)

param(
    [string]$ServerIP = "210.104.76.139",
    [string]$ServerUser = "root",
    [string]$LocalDBUser = "root",
    [string]$LocalDBPassword = "",
    [string]$ServerDBUser = "root",
    [string]$ServerDBPassword = "",
    [string]$DatabaseName = "wise_young"
)

Write-Host "=== WiseYoung 데이터베이스 동기화 시작 ===" -ForegroundColor Cyan
Write-Host "로컬 → KT Cloud 서버 ($ServerIP)" -ForegroundColor Yellow

# 1. 로컬에서 데이터베이스 덤프
Write-Host "`n[1/4] 로컬 데이터베이스 덤프 중..." -ForegroundColor Yellow

$dumpFile = "wise_young_dump_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
$dumpPath = Join-Path $PSScriptRoot $dumpFile

# mysqldump 명령어 구성
$mysqldumpCmd = "mysqldump"
$dumpArgs = @(
    "-u", $LocalDBUser,
    "--databases", $DatabaseName,
    "--single-transaction",
    "--routines",
    "--triggers",
    "--add-drop-database",
    "--default-character-set=utf8mb4"
)

# 비밀번호가 있으면 추가
if ($LocalDBPassword -ne "") {
    $dumpArgs += "--password=$LocalDBPassword"
}

# 덤프 실행
try {
    & $mysqldumpCmd $dumpArgs | Out-File -FilePath $dumpPath -Encoding UTF8
    Write-Host "✅ 덤프 완료: $dumpFile" -ForegroundColor Green
    Write-Host "   크기: $([math]::Round((Get-Item $dumpPath).Length / 1MB, 2)) MB" -ForegroundColor Gray
} catch {
    Write-Host "❌ 덤프 실패: $_" -ForegroundColor Red
    exit 1
}

# 2. 서버로 덤프 파일 전송
Write-Host "`n[2/4] 서버로 덤프 파일 전송 중..." -ForegroundColor Yellow
Write-Host "   서버: $ServerUser@$ServerIP" -ForegroundColor Gray
Write-Host "   파일: $dumpFile" -ForegroundColor Gray

$serverDumpPath = "/tmp/$dumpFile"

try {
    scp $dumpPath "${ServerUser}@${ServerIP}:${serverDumpPath}"
    Write-Host "✅ 전송 완료" -ForegroundColor Green
} catch {
    Write-Host "❌ 전송 실패: $_" -ForegroundColor Red
    Remove-Item $dumpPath -ErrorAction SilentlyContinue
    exit 1
}

# 3. 서버에서 데이터베이스 복원
Write-Host "`n[3/4] 서버에서 데이터베이스 복원 중..." -ForegroundColor Yellow

$restoreScript = @"
#!/bin/bash
# 데이터베이스 복원 스크립트

DUMP_FILE="$serverDumpPath"
DB_NAME="$DatabaseName"
DB_USER="$ServerDBUser"
DB_PASSWORD="$ServerDBPassword"

echo "데이터베이스 복원 시작..."

# 기존 데이터베이스 백업 (선택사항)
if [ -d "/tmp/db_backup" ]; then
    echo "기존 백업 디렉토리 존재"
else
    mkdir -p /tmp/db_backup
fi

BACKUP_FILE="/tmp/db_backup/wise_young_backup_\$(date +%Y%m%d_%H%M%S).sql"
if [ -f "\$BACKUP_FILE" ]; then
    echo "백업 파일이 이미 존재합니다: \$BACKUP_FILE"
else
    echo "기존 데이터베이스 백업 중..."
    mysqldump -u \$DB_USER $([string]::IsNullOrEmpty($ServerDBPassword) ? "" : "-p\$DB_PASSWORD") --databases \$DB_NAME > \$BACKUP_FILE 2>/dev/null || echo "백업 실패 (데이터베이스가 없을 수 있음)"
fi

# 데이터베이스 복원
echo "데이터베이스 복원 중..."
if [ -z "\$DB_PASSWORD" ]; then
    mysql -u \$DB_USER < \$DUMP_FILE
else
    mysql -u \$DB_USER -p\$DB_PASSWORD < \$DUMP_FILE
fi

if [ \$? -eq 0 ]; then
    echo "✅ 데이터베이스 복원 완료"
    # 덤프 파일 삭제
    rm -f \$DUMP_FILE
    echo "임시 덤프 파일 삭제 완료"
else
    echo "❌ 데이터베이스 복원 실패"
    exit 1
fi
"@

$restoreScriptPath = Join-Path $env:TEMP "restore_db.sh"
$restoreScript | Out-File -FilePath $restoreScriptPath -Encoding UTF8 -NoNewline

try {
    # 스크립트를 서버로 전송
    scp $restoreScriptPath "${ServerUser}@${ServerIP}:/tmp/restore_db.sh"
    
    # 서버에서 스크립트 실행
    ssh "${ServerUser}@${ServerIP}" "chmod +x /tmp/restore_db.sh && /tmp/restore_db.sh"
    
    Write-Host "✅ 복원 완료" -ForegroundColor Green
} catch {
    Write-Host "❌ 복원 실패: $_" -ForegroundColor Red
    Write-Host "   수동으로 복원하려면:" -ForegroundColor Yellow
    Write-Host "   ssh $ServerUser@$ServerIP" -ForegroundColor Gray
    Write-Host "   mysql -u $ServerDBUser -p $DatabaseName < $serverDumpPath" -ForegroundColor Gray
    exit 1
} finally {
    Remove-Item $restoreScriptPath -ErrorAction SilentlyContinue
}

# 4. 로컬 덤프 파일 정리
Write-Host "`n[4/4] 임시 파일 정리 중..." -ForegroundColor Yellow

$keepDump = Read-Host "로컬 덤프 파일을 유지하시겠습니까? (Y/N, 기본값: N)"
if ($keepDump -ne "Y" -and $keepDump -ne "y") {
    Remove-Item $dumpPath -ErrorAction SilentlyContinue
    Write-Host "✅ 임시 파일 삭제 완료" -ForegroundColor Green
} else {
    Write-Host "✅ 덤프 파일 유지: $dumpPath" -ForegroundColor Green
}

Write-Host "`n=== 데이터베이스 동기화 완료! ===" -ForegroundColor Green
Write-Host "`n서버에서 확인:" -ForegroundColor Cyan
Write-Host "  ssh $ServerUser@$ServerIP" -ForegroundColor White
Write-Host "  mysql -u $ServerDBUser -p $DatabaseName -e 'SHOW TABLES;'" -ForegroundColor White

