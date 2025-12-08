# 테스트 데이터 삭제 PowerShell 스크립트
# housing_complex, housing_notice, policy 테이블은 제외하고 나머지 데이터 삭제

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "테스트 데이터 삭제 스크립트" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  주의: housing_complex, housing_notice, policy 테이블은 유지됩니다." -ForegroundColor Yellow
Write-Host ""

# application.yml에서 데이터베이스 정보 읽기
$applicationYmlPath = Join-Path $PSScriptRoot "src\main\resources\application.yml"
if (-not (Test-Path $applicationYmlPath)) {
    Write-Host "❌ application.yml 파일을 찾을 수 없습니다: $applicationYmlPath" -ForegroundColor Red
    exit 1
}

$applicationYml = Get-Content $applicationYmlPath -Raw

# 데이터베이스 연결 정보 추출
$dbUrl = if ($applicationYml -match 'url:\s*jdbc:(?:mariadb|mysql)://([^?]+)') { 
    $matches[1] 
} else { 
    "127.0.0.1:3306/wise_young" 
}

$dbUser = if ($applicationYml -match 'username:\s*\$\{DB_USERNAME:([^}]+)\}') { 
    $matches[1] 
} elseif ($applicationYml -match 'username:\s*([^\s#]+)') { 
    $matches[1] 
} else { 
    "root" 
}

$dbPassword = if ($applicationYml -match 'password:\s*\$\{DB_PASSWORD:([^}]+)\}') { 
    $matches[1] 
} elseif ($applicationYml -match 'password:\s*([^\s#]+)') { 
    $matches[1] 
} else { 
    "1234" 
}

# URL 파싱
if ($dbUrl -match '([^:]+):(\d+)/(.+)') {
    $dbHost = $matches[1]
    $dbPort = $matches[2]
    $dbName = $matches[3]
    
    Write-Host "데이터베이스 연결 정보:" -ForegroundColor Cyan
    Write-Host "  호스트: $dbHost" -ForegroundColor White
    Write-Host "  포트: $dbPort" -ForegroundColor White
    Write-Host "  데이터베이스: $dbName" -ForegroundColor White
    Write-Host "  사용자: $dbUser" -ForegroundColor White
    Write-Host ""
    
    # SQL 파일 경로
    $sqlFile = Join-Path $PSScriptRoot "clear_test_data.sql"
    
    if (Test-Path $sqlFile) {
        Write-Host "SQL 스크립트 실행 중..." -ForegroundColor Green
        
        # MariaDB/MySQL 명령어 실행
        $mysqlPath = "mysql"
        
        # MySQL 클라이언트 경로 확인
        $mysqlPaths = @(
            "mysql",
            "C:\Program Files\MariaDB\bin\mysql.exe",
            "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
            "C:\xampp\mysql\bin\mysql.exe"
        )
        
        $foundMysql = $false
        foreach ($path in $mysqlPaths) {
            if (Get-Command $path -ErrorAction SilentlyContinue) {
                $mysqlPath = $path
                $foundMysql = $true
                break
            }
        }
        
        if (-not $foundMysql) {
            Write-Host "⚠️  MySQL/MariaDB 클라이언트를 찾을 수 없습니다." -ForegroundColor Yellow
            Write-Host ""
            Write-Host "수동 실행 방법:" -ForegroundColor Yellow
            Write-Host "1. MySQL/MariaDB 클라이언트를 사용하여 데이터베이스에 연결" -ForegroundColor White
            Write-Host "2. 다음 명령어 실행:" -ForegroundColor White
            Write-Host "   mysql -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName < clear_test_data.sql" -ForegroundColor Gray
            Write-Host ""
            Write-Host "또는 MySQL Workbench, HeidiSQL 등의 도구를 사용하여" -ForegroundColor White
            Write-Host "clear_test_data.sql 파일의 내용을 실행하세요." -ForegroundColor White
            exit 1
        }
        
        # SQL 파일 실행
        $env:MYSQL_PWD = $dbPassword
        $arguments = @(
            "-h", $dbHost,
            "-P", $dbPort,
            "-u", $dbUser,
            $dbName
        )
        
        try {
            Get-Content $sqlFile | & $mysqlPath $arguments
            Write-Host ""
            Write-Host "✅ 데이터 삭제가 완료되었습니다!" -ForegroundColor Green
        } catch {
            Write-Host ""
            Write-Host "❌ 오류 발생: $_" -ForegroundColor Red
            Write-Host ""
            Write-Host "수동 실행 방법:" -ForegroundColor Yellow
            Write-Host "mysql -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName < clear_test_data.sql" -ForegroundColor Gray
        } finally {
            Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "❌ SQL 파일을 찾을 수 없습니다: $sqlFile" -ForegroundColor Red
    }
} else {
    Write-Host "❌ 데이터베이스 URL 형식을 파싱할 수 없습니다: $dbUrl" -ForegroundColor Red
    Write-Host ""
    Write-Host "수동 실행 방법:" -ForegroundColor Yellow
    Write-Host "1. MySQL/MariaDB 클라이언트를 사용하여 데이터베이스에 연결" -ForegroundColor White
    Write-Host "2. clear_test_data.sql 파일의 내용을 실행" -ForegroundColor White
}

