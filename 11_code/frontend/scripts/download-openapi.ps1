param(
    [string]$BaseUrl = $env:MYAGENT_OPENAPI_BASE_URL,
    [string]$OutputPath = "openapi/myagent-openapi.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "http://127.0.0.1:8080"
}

$outputDir = Split-Path -Parent $OutputPath
if (-not [string]::IsNullOrWhiteSpace($outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

Invoke-WebRequest -Uri "$BaseUrl/v3/api-docs" -OutFile $OutputPath -UseBasicParsing -ErrorAction Stop | Out-Null

$content = Get-Content -Path $OutputPath -Raw -Encoding UTF8
if ([string]::IsNullOrWhiteSpace($content)) {
    throw "OpenAPI download failed because the output file is empty."
}

$null = $content | ConvertFrom-Json
