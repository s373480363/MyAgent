$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")

Set-Location (Join-Path $repoRoot "11_code\frontend")
npm run dev -- --host 127.0.0.1 --port 5173
