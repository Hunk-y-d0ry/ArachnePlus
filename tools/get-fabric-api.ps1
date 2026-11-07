param([string]$ModsDir)
$ErrorActionPreference = 'Stop'
try {
  $versions = Invoke-RestMethod -Uri 'https://api.modrinth.com/v2/project/fabric-api/version' `
    -Body @{ game_versions = '["26.2"]'; loaders = '["fabric"]' } `
    -Headers @{ 'User-Agent' = 'ArachnePlus-installer' }
  $file = $versions[0].files | Where-Object { $_.primary } | Select-Object -First 1
  if (-not $file) { $file = $versions[0].files[0] }
  Invoke-WebRequest -Uri $file.url -OutFile (Join-Path $ModsDir $file.filename)
  Write-Host "Downloaded $($file.filename)"
} catch {
  Write-Host "Could not download Fabric API automatically: $_"
  exit 1
}
