# 로컬 MariaDB 데이터 삭제 스크립트
# 다음 테이블의 데이터를 모두 삭제합니다:
# - user
# - user_profile
# - interest_category
# - bookmark
# - calendar_event
# - chat_history

param(
    [Parameter(Mandatory=$false)]
    [string]$DbHost = "127.0.0.1",
    
    [Parameter(Mandatory=$false)]
    [string]$DbPort = "3306",
    
    [Parameter(Mandatory=$false)]
    [string]$DbName = "wise_young",
    
    [Parameter(Mandatory=$false)]
    [string]$DbUser = "root",
    
    [Parameter(Mandatory=$false)]
    [string]$DbPassword = "1234"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "로컬 DB 데이터 삭제" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 삭제할 테이블 목록 (외래키 순서 고려)
$tables = @(
    "bookmark",
    "calendar_event",
    "interest_category",
    "user_profile",
    "chat_history",
    "user"
)

Write-Host "삭제할 테이블:" -ForegroundColor Yellow
foreach ($table in $tables) {
    Write-Host "  - $table" -ForegroundColor Gray
}
Write-Host ""

# 확인 메시지
$confirm = Read-Host "정말로 위 테이블의 모든 데이터를 삭제하시겠습니까? (yes/no)"
if ($confirm -ne "yes") {
    Write-Host "취소되었습니다." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "데이터 삭제 시작..." -ForegroundColor Yellow
Write-Host ""

# MySQL/MariaDB 클라이언트 경로 찾기
$mysqlPath = $null
$possiblePaths = @(
    "C:\Program Files\MariaDB 12.0\bin\mysql.exe",
    "C:\Program Files\MariaDB 11.0\bin\mysql.exe",
    "C:\Program Files\MariaDB 10.11\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    "mysql.exe"
)

foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $mysqlPath = $path
        break
    }
    # PATH에서 찾기
    $found = Get-Command $path -ErrorAction SilentlyContinue
    if ($found) {
        $mysqlPath = $found.Source
        break
    }
}

if (-not $mysqlPath) {
    Write-Host "❌ MySQL/MariaDB 클라이언트를 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "MariaDB 설치 경로를 확인하거나 PATH에 추가하세요." -ForegroundColor Yellow
    exit 1
}

Write-Host "MySQL 클라이언트: $mysqlPath" -ForegroundColor Gray
Write-Host ""

# SQL 명령어 생성
$sqlCommands = @()

# 외래키 체크 비활성화
$sqlCommands += "SET FOREIGN_KEY_CHECKS = 0;"

# 각 테이블 데이터 삭제
foreach ($table in $tables) {
    $sqlCommands += "DELETE FROM `$table`;"
    $sqlCommands += "ALTER TABLE `$table` AUTO_INCREMENT = 1;"
}

# 외래키 체크 활성화
$sqlCommands += "SET FOREIGN_KEY_CHECKS = 1;"

# SQL 파일로 저장
$sqlFile = "$env:TEMP\delete_local_db_data.sql"
$sqlCommands | Out-File -FilePath $sqlFile -Encoding UTF8

Write-Host "SQL 명령어 생성 완료: $sqlFile" -ForegroundColor Gray
Write-Host ""

# MySQL 실행
try {
    Write-Host "데이터베이스에 연결 중..." -ForegroundColor Yellow
    
    # SQL 내용을 직접 실행
    $sqlContent = Get-Content $sqlFile -Raw -Encoding UTF8
    $sqlContent | & $mysqlPath -h $DbHost -P $DbPort -u $DbUser -p$DbPassword $DbName 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ 데이터 삭제 완료!" -ForegroundColor Green
        Write-Host ""
        
        # 삭제된 행 수 확인
        Write-Host "삭제 결과 확인:" -ForegroundColor Cyan
        foreach ($table in $tables) {
            $countSql = "SELECT COUNT(*) FROM `$table`;"
            $result = $countSql | & $mysqlPath -h $DbHost -P $DbPort -u $DbUser -p$DbPassword $DbName -N -s 2>$null
            if ($result -match '^\d+$') {
                Write-Host "  - $table : $result 행" -ForegroundColor Gray
            } else {
                Write-Host "  - $table : 확인 실패" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "❌ 데이터 삭제 실패! (Exit Code: $LASTEXITCODE)" -ForegroundColor Red
        Write-Host "SQL 파일 위치: $sqlFile" -ForegroundColor Gray
        Write-Host "수동으로 실행하려면: mysql -u $DbUser -p$DbPassword $DbName < $sqlFile" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ 오류 발생: $_" -ForegroundColor Red
    Write-Host "SQL 파일 위치: $sqlFile" -ForegroundColor Gray
    Write-Host "수동으로 실행하려면: mysql -u $DbUser -p$DbPassword $DbName < $sqlFile" -ForegroundColor Yellow
    exit 1
} finally {
    # 임시 파일 삭제 (선택사항 - 디버깅용으로 남겨둘 수도 있음)
    # if (Test-Path $sqlFile) {
    #     Remove-Item $sqlFile -Force
    # }
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "작업 완료!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan

