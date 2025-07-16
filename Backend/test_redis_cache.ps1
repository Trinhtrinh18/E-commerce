# Test script cho Redis Cache functionality - PowerShell version
Write-Host "=== Redis Cache Test Script ===" -ForegroundColor Green

# Base URL
$BaseURL = "http://localhost:8080"

Write-Host "1. Testing Redis Health Check..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "${BaseURL}/api/cache/health" -Method GET
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor White
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. Testing Cache Statistics..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "${BaseURL}/api/cache/statistics" -Method GET
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor White
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Testing Product Cache Check (example product ID)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "${BaseURL}/api/cache/product/60f8d6f8e4b0a4f8e4b0a4f8/exists" -Method GET
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor White
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. Testing Cart Items Cache (requires authentication)..." -ForegroundColor Yellow
Write-Host "Note: This endpoint requires authentication, so will return 401 without token" -ForegroundColor DarkYellow
try {
    $response = Invoke-WebRequest -Uri "${BaseURL}/api/cache/cart-items" -Method GET
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response: $($response.Content)" -ForegroundColor White
} catch {
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "This is expected - endpoint requires authentication" -ForegroundColor DarkYellow
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "To test with authentication:" -ForegroundColor Cyan
Write-Host "1. Login to get JWT token" -ForegroundColor Cyan
Write-Host "2. Use the token in Authorization header" -ForegroundColor Cyan
Write-Host "3. Add product to cart to test cache" -ForegroundColor Cyan
Write-Host "4. Check cache status with authenticated request" -ForegroundColor Cyan
