Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$openApiPath = "openapi/agent-studio-openapi.json"
$schemaPath = "src/api/generated/schema.ts"
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("agent-studio-openapi-check-" + [System.Guid]::NewGuid().ToString("N"))

New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
try {
    $openApiBackup = Join-Path $tempDir "agent-studio-openapi.json"
    $schemaBackup = Join-Path $tempDir "schema.ts"
    Copy-Item -Path $openApiPath -Destination $openApiBackup -Force
    Copy-Item -Path $schemaPath -Destination $schemaBackup -Force

    & npm run openapi:refresh
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $openApiSame = (Get-Content -Path $openApiBackup -Raw -Encoding UTF8) -ceq (Get-Content -Path $openApiPath -Raw -Encoding UTF8)
    $schemaSame = (Get-Content -Path $schemaBackup -Raw -Encoding UTF8) -ceq (Get-Content -Path $schemaPath -Raw -Encoding UTF8)
    if (-not ($openApiSame -and $schemaSame)) {
        Write-Error "OpenAPI generated artifacts are out of sync. Run npm run openapi:refresh and commit the generated files."
        exit 1
    }
} finally {
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
