#!/bin/bash
# Test Redis Cache Script

echo "=== Testing Redis Cache Integration ==="
echo ""

# Test 1: Check Redis connection
echo "1. Testing Redis connection..."
curl -X GET "http://localhost:8080/api/cache/health" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\n" \
  -s

echo ""
echo "2. Testing cache statistics..."
curl -X GET "http://localhost:8080/api/cache/statistics" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\n" \
  -s

echo ""
echo "=== Test completed ==="
echo ""
echo "To test with authentication:"
echo "1. Login to get JWT token"
echo "2. Use the token in Authorization header"
echo "3. Add product to cart to test cache"
echo "4. Check cache status with: curl -X GET \"http://localhost:8080/api/cache/status\" -H \"Authorization: Bearer YOUR_TOKEN\""
