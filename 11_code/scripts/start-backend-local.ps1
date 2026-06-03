param(
    [string]$DatasourceUrl = $env:MYAGENT_DATASOURCE_URL,
    [string]$DatasourceUsername = $env:MYAGENT_DATASOURCE_USERNAME,
    [string]$DatasourcePassword = $env:MYAGENT_DATASOURCE_PASSWORD,
    [string]$ServerPort = $env:MYAGENT_SERVER_PORT
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$env:JAVA_HOME = Join-Path $repoRoot "9_dependency\tools\jdk21\jdk-21.0.11+10"
$mavenBin = Join-Path $repoRoot "9_dependency\tools\maven-3.9.11\apache-maven-3.9.11\bin"
$env:Path = "$env:JAVA_HOME\bin;$mavenBin;$env:Path"

if (-not [string]::IsNullOrWhiteSpace($DatasourceUrl)) {
    $env:MYAGENT_DATASOURCE_URL = $DatasourceUrl
}
if (-not [string]::IsNullOrWhiteSpace($DatasourceUsername)) {
    $env:MYAGENT_DATASOURCE_USERNAME = $DatasourceUsername
}
if (-not [string]::IsNullOrWhiteSpace($DatasourcePassword)) {
    $env:MYAGENT_DATASOURCE_PASSWORD = $DatasourcePassword
}
if (-not [string]::IsNullOrWhiteSpace($ServerPort)) {
    $env:MYAGENT_SERVER_PORT = $ServerPort
}

Set-Location (Join-Path $repoRoot "11_code\backend")
& mvn "spring-boot:run" "-Dspring-boot.run.profiles=local"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
