# WiseYoung 특정 테이블 동기화 스크립트 (로컬 → KT Cloud 서버)
# housing_complex, housing_notice 테이블만 동기화

param(
    [string]$ServerIP = "210.104.76.139",
    [string]$ServerUser = "root",
    [string]$LocalDBUser = "root",
    [string]$LocalDBPassword = "",
    [string]$ServerDBUser = "root",
    [string]$ServerDBPassword = "",
    [string]$DatabaseName = "wise_young",
    [string[]]$Tables = @("housing_complex", "housing_notice")
)

Write-Host "=== WiseYoung 테이블 동기화 시작 ===" -ForegroundColor Cyan
Write-Host "동기화할 테이블: $($Tables -join ', ')" -ForegroundColor Yellow
Write-Host "로컬 → KT Cloud 서버 ($ServerIP)" -ForegroundColor Yellow

# 1. 로컬에서 테이블 덤프
Write-Host "`n[1/4] 로컬 테이블 덤프 중..." -ForegroundColor Yellow

$dumpFile = "housing_tables_dump_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
$dumpPath = Join-Path $PSScriptRoot $dumpFile

# mysqldump 명령어 구성 (경로 자동 탐지)
$mysqldumpCmd = $null
$possiblePaths = @(
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    "C:\Program Files\MySQL\MySQL Server 8.1\bin\mysqldump.exe",
    "C:\Program Files\MariaDB 10.11\bin\mysqldump.exe",
    "C:\Program Files\MariaDB 10.10\bin\mysqldump.exe",
    "C:\xampp\mysql\bin\mysqldump.exe",
    "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
)

foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $mysqldumpCmd = $path
        Write-Host "   mysqldump 경로: $path" -ForegroundColor Gray
        break
    }
}

if ($null -eq $mysqldumpCmd) {
    # PATH에서 찾기 시도
    $mysqldumpCmd = Get-Command mysqldump -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
    if ($null -eq $mysqldumpCmd) {
        Write-Host "❌ mysqldump를 찾을 수 없습니다." -ForegroundColor Red
        Write-Host "   MySQL/MariaDB가 설치되어 있는지 확인하거나," -ForegroundColor Yellow
        Write-Host "   mysqldump.exe의 전체 경로를 입력해주세요:" -ForegroundColor Yellow
        $mysqldumpCmd = Read-Host "mysqldump 경로"
        if (-not (Test-Path $mysqldumpCmd)) {
            Write-Host "❌ 지정한 경로에 mysqldump가 없습니다." -ForegroundColor Red
            exit 1
        }
    }
}
$dumpArgs = @(
    "-u", $LocalDBUser,
    $DatabaseName
)

# 비밀번호가 있으면 추가
if ($LocalDBPassword -ne "") {
    $dumpArgs += "--password=$LocalDBPassword"
}

# 테이블 목록 추가
$dumpArgs += $Tables

# 덤프 옵션 추가
$dumpArgs += @(
    "--single-transaction",
    "--add-drop-table",
    "--default-character-set=utf8mb4",
    "--complete-insert",
    "--extended-insert"
)

# 덤프 실행
try {
    Write-Host "   테이블: $($Tables -join ', ')" -ForegroundColor Gray
    & $mysqldumpCmd $dumpArgs | Out-File -FilePath $dumpPath -Encoding UTF8
    
    if (Test-Path $dumpPath) {
        $fileSize = (Get-Item $dumpPath).Length
        if ($fileSize -gt 0) {
            Write-Host "✅ 덤프 완료: $dumpFile" -ForegroundColor Green
            Write-Host "   크기: $([math]::Round($fileSize / 1MB, 2)) MB" -ForegroundColor Gray
        } else {
            Write-Host "❌ 덤프 파일이 비어있습니다." -ForegroundColor Red
            Remove-Item $dumpPath -ErrorAction SilentlyContinue
            exit 1
        }
    } else {
        Write-Host "❌ 덤프 파일이 생성되지 않았습니다." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ 덤프 실패: $_" -ForegroundColor Red
    Remove-Item $dumpPath -ErrorAction SilentlyContinue
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
    Write-Host "   수동으로 전송하려면:" -ForegroundColor Yellow
    Write-Host "   scp $dumpPath ${ServerUser}@${ServerIP}:${serverDumpPath}" -ForegroundColor Gray
    Remove-Item $dumpPath -ErrorAction SilentlyContinue
    exit 1
}

# 3. 서버에서 테이블 복원
Write-Host "`n[3/4] 서버에서 테이블 복원 중..." -ForegroundColor Yellow

$restoreScript = @"
#!/bin/bash
# 테이블 복원 스크립트

DUMP_FILE="$serverDumpPath"
DB_NAME="$DatabaseName"
DB_USER="$ServerDBUser"
DB_PASSWORD="$ServerDBPassword"
TABLES="$($Tables -join ' ')"

echo "Starting table restore..."
echo "Database: \$DB_NAME"
echo "Tables: \$TABLES"

# 기존 테이블 백업 (선택사항)
BACKUP_DIR="/tmp/db_backup"
if [ ! -d "\$BACKUP_DIR" ]; then
    mkdir -p \$BACKUP_DIR
fi

for TABLE in \$TABLES; do
    BACKUP_FILE="\$BACKUP_DIR/\${TABLE}_backup_\$(date +%Y%m%d_%H%M%S).sql"
    echo "Backing up existing table: \$TABLE"
    if [ -z "\$DB_PASSWORD" ]; then
        mysqldump -u \$DB_USER \$DB_NAME \$TABLE > \$BACKUP_FILE 2>/dev/null || echo "Backup failed (table may not exist): \$TABLE"
    else
        mysqldump -u \$DB_USER -p\$DB_PASSWORD \$DB_NAME \$TABLE > \$BACKUP_FILE 2>/dev/null || echo "Backup failed (table may not exist): \$TABLE"
    fi
done

# 데이터베이스 복원
echo "Restoring tables..."
if [ -z "\$DB_PASSWORD" ]; then
    mysql -u \$DB_USER \$DB_NAME < \$DUMP_FILE
else
    mysql -u \$DB_USER -p\$DB_PASSWORD \$DB_NAME < \$DUMP_FILE
fi

if [ \$? -eq 0 ]; then
    echo "✅ 테이블 복원 완료"
    
    # 복원된 테이블 데이터 개수 확인
    echo ""
    echo "Restored data count:"
    for TABLE in \$TABLES; do
        if [ -z "\$DB_PASSWORD" ]; then
            COUNT=\$(mysql -u \$DB_USER \$DB_NAME -se "SELECT COUNT(*) FROM \$TABLE" 2>/dev/null)
        else
            COUNT=\$(mysql -u \$DB_USER -p\$DB_PASSWORD \$DB_NAME -se "SELECT COUNT(*) FROM \$TABLE" 2>/dev/null)
        fi
        echo "  \${TABLE}: \${COUNT} rows"
    done
    
    # 덤프 파일 삭제
    rm -f \$DUMP_FILE
    echo ""
    echo "Temporary dump file deleted"
else
    echo "❌ Table restore failed"
    exit 1
fi
"@

$restoreScriptPath = Join-Path $env:TEMP "restore_tables.sh"
$restoreScript | Out-File -FilePath $restoreScriptPath -Encoding UTF8 -NoNewline

try {
    # 스크립트를 서버로 전송
    scp $restoreScriptPath "${ServerUser}@${ServerIP}:/tmp/restore_tables.sh"
    
    # 서버에서 스크립트 실행
    ssh "${ServerUser}@${ServerIP}" "chmod +x /tmp/restore_tables.sh && /tmp/restore_tables.sh"
    
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

Write-Host "`n=== 테이블 동기화 완료! ===" -ForegroundColor Green
Write-Host "`n서버에서 확인:" -ForegroundColor Cyan
Write-Host "  ssh $ServerUser@$ServerIP" -ForegroundColor White
Write-Host "  mysql -u $ServerDBUser -p $DatabaseName -e 'SELECT COUNT(*) FROM housing_complex;'" -ForegroundColor White
Write-Host "  mysql -u $ServerDBUser -p $DatabaseName -e 'SELECT COUNT(*) FROM housing_notice;'" -ForegroundColor White

