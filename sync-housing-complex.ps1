param(
    [string]$brtcCode = "",
    [string]$signguCode = ""
)

$baseUrl = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Housing Complex 동기화" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($brtcCode -and $signguCode) {
    Write-Host "특정 지역 동기화: brtcCode=$brtcCode, signguCode=$signguCode" -ForegroundColor Yellow
    $amp = '&'
    $url = "$baseUrl/api/housing/complex/sync?brtcCode=$brtcCode$amp" + "signguCode=$signguCode"
} else {
    Write-Host "전체 지역 동기화 (17개 시도 전체)" -ForegroundColor Yellow
    $url = "$baseUrl/api/housing/complex/sync"
}

Write-Host "요청 URL: $url" -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $url -Method POST -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
    Write-Host "성공 (상태 코드: $($response.StatusCode))" -ForegroundColor Green
    Write-Host ""
    Write-Host "응답:" -ForegroundColor Gray
    Write-Host $response.Content
} catch {
    Write-Host "실패: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "응답: $responseBody" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "완료" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
