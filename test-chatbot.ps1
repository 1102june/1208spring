# 챗봇 API 테스트 스크립트

Write-Host "=== 챗봇 API 테스트 ===" -ForegroundColor Green

$baseUrl = "http://localhost:8080/api/chat"
$headers = @{
    "Content-Type" = "application/json"
    "X-User-Id" = "test-user"
}

# 테스트 1: 간단한 인사
Write-Host "`n[테스트 1] 간단한 인사" -ForegroundColor Yellow
$body1 = @{
    message = "안녕하세요"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body1
    Write-Host "성공!" -ForegroundColor Green
    $response1 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "실패: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "에러 응답: $errorBody" -ForegroundColor Red
    }
}

# 테스트 2: 청년 정책 질문
Write-Host "`n[테스트 2] 청년 정책 질문" -ForegroundColor Yellow
$body2 = @{
    message = "청년 주거 지원 정책이 뭐가 있나요?"
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method POST -Headers $headers -Body $body2
    Write-Host "성공!" -ForegroundColor Green
    $response2 | ConvertTo-Json -Depth 10
} catch {
    Write-Host "실패: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "에러 응답: $errorBody" -ForegroundColor Red
    }
}

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Green

