$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Set-Location -LiteralPath $PSScriptRoot
$headers = @{ 'User-Agent' = 'LPChat-TestServer/1.2.0 (https://github.com/Tobias30yt/LPChat)' }

foreach ($artifact in (Get-Content -LiteralPath 'downloads.json' -Raw | ConvertFrom-Json)) {
    $destination = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $artifact.path))
    if (-not $destination.StartsWith($PSScriptRoot.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Downloadziel liegt ausserhalb des Testservers: $destination"
    }
    New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($destination)) | Out-Null
    if ((Test-Path -LiteralPath $destination) -and ((Get-FileHash -LiteralPath $destination -Algorithm $artifact.algorithm).Hash -ieq $artifact.hash)) {
        Write-Host "Bereits vorhanden und geprueft: $($artifact.path)"
        continue
    }
    $temporary = $destination + '.download'
    try {
        Write-Host "Lade $($artifact.path) ..."
        Invoke-WebRequest -UseBasicParsing -Headers $headers -Uri $artifact.url -OutFile $temporary
        if ((Get-FileHash -LiteralPath $temporary -Algorithm $artifact.algorithm).Hash -ine $artifact.hash) {
            throw "Pruefsumme stimmt nicht: $($artifact.path)"
        }
        Move-Item -LiteralPath $temporary -Destination $destination -Force
    } finally {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
    }
}

# Explicit setup installs this development version. launch.bat never copies an old build over an auto-update.
$localPlugin = Join-Path $PSScriptRoot '..\target\LPChat-1.2.0.jar'
if (-not (Test-Path -LiteralPath $localPlugin)) { throw 'LPChat-1.2.0.jar fehlt. Im Projekt zuerst mvn verify ausfuehren.' }
Copy-Item -LiteralPath $localPlugin -Destination (Join-Path $PSScriptRoot 'plugins\LPChat.jar') -Force
if (-not (Test-Path -LiteralPath 'server.properties')) { Copy-Item -LiteralPath 'server.properties.example' -Destination 'server.properties' }
Write-Host 'Testserver fertig. Mit launch.bat starten; die EULA wird beim ersten Start abgefragt.'
