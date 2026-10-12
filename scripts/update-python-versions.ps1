<#
.SYNOPSIS
    Queries GitHub releases for astral-sh/python-build-standalone and updates the plugin's versions.json catalog.
.EXAMPLE
    .\scripts\update-python-versions.ps1
#>

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PyScript = Join-Path $ScriptDir "update-python-versions.py"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ">>> Updating Python Version Catalog from GitHub Releases..." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Check if Python is installed locally
$HasPython = $false
try {
    $ver = & python --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        $HasPython = $true
    }
} catch {
    $HasPython = $false
}

if ($HasPython) {
    & python $PyScript
} else {
    Write-Host "Local Python not found. Running updater via Docker python:3.11-slim container..." -ForegroundColor Yellow
    $RootDir = Split-Path -Parent $ScriptDir
    $DockerPath = $RootDir.Replace("\", "/")
    docker run --rm -v "${DockerPath}:/workspace" -w /workspace python:3.11-slim python scripts/update-python-versions.py
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] Python catalog update completed!" -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] Failed to update Python version catalog." -ForegroundColor Red
}
