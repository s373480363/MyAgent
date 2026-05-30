param(
    [string]$BaseUrl = $env:MYAGENT_OPENAPI_BASE_URL,
    [string]$OutputPath = "openapi/myagent-openapi.json"
)

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "http://localhost:8080"
}

$outputDir = Split-Path -Parent $OutputPath
if (-not [string]::IsNullOrWhiteSpace($outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

Invoke-WebRequest -Uri "$BaseUrl/v3/api-docs" -OutFile $OutputPath
