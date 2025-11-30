# bookmark와 calendar_event 테이블의 모든 데이터 삭제 스크립트

Write-Host "북마크 및 캘린더 이벤트 삭제 시작..." -ForegroundColor Yellow

# 북마크 삭제
Write-Host "`n[1/2] 모든 북마크 삭제 중..." -ForegroundColor Cyan
$bookmarkResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/bookmarks/admin/all" -Method Delete -ErrorAction Stop
Write-Host "✅ 북마크 삭제 완료: $($bookmarkResponse.message)" -ForegroundColor Green

# 캘린더 이벤트 삭제
Write-Host "`n[2/2] 모든 캘린더 이벤트 삭제 중..." -ForegroundColor Cyan
$calendarResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/calendar/admin/all" -Method Delete -ErrorAction Stop
Write-Host "✅ 캘린더 이벤트 삭제 완료: $($calendarResponse.message)" -ForegroundColor Green

Write-Host "`n모든 데이터 삭제 완료!" -ForegroundColor Green
