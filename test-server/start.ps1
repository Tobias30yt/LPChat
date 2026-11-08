param([switch]$ValidateOnly)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

function Find-TestJava {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if ($env:LPCHAT_JAVA) { $candidates.Add($env:LPCHAT_JAVA) }
    if ($env:JAVA_HOME) { $candidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe')) }
    foreach ($javaRoot in @((Join-Path $env:ProgramFiles 'Java'), (Join-Path $env:ProgramFiles 'Eclipse Adoptium'))) {
        if (Test-Path -LiteralPath $javaRoot) {
            Get-ChildItem -LiteralPath $javaRoot -Directory | Sort-Object Name -Descending | ForEach-Object {
                $candidates.Add((Join-Path $_.FullName 'bin\java.exe'))
            }
        }
    }
    $pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($pathJava) { $candidates.Add($pathJava.Source) }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) { continue }
        $process = [System.Diagnostics.Process]::new()
        $process.StartInfo.FileName = $candidate
        $process.StartInfo.Arguments = '-version'
        $process.StartInfo.UseShellExecute = $false
        $process.StartInfo.CreateNoWindow = $true
        $process.StartInfo.RedirectStandardOutput = $true
        $process.StartInfo.RedirectStandardError = $true
        try {
            [void]$process.Start()
            if (-not $process.WaitForExit(10000)) { $process.Kill(); continue }
            $versionOutput = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
            if ($process.ExitCode -eq 0 -and $versionOutput -match 'version\s+"(?<major>\d+)') {
                if ([int]$Matches.major -ge 25) { return $candidate }
            }
        } catch {
            Write-Verbose "Java nicht verwendbar: $candidate"
        } finally { $process.Dispose() }
    }
    throw 'Java 25 oder neuer fehlt. JDK installieren oder LPCHAT_JAVA auf den Pfad zu java.exe setzen.'
}

try {
    $javaPath = Find-TestJava
    $memory = if ($env:LPCHAT_MEMORY) { $env:LPCHAT_MEMORY } else { '2G' }
    if ($memory -notmatch '^[1-9][0-9]*[MG]$') { throw 'LPCHAT_MEMORY muss z.B. 2G oder 4096M sein.' }
    foreach ($required in @('paper-26.2-135.jar', 'plugins\LPChat.jar', 'plugins\LuckPerms.jar', 'plugins\PlaceholderAPI.jar', 'plugins\UltimateTeams.jar')) {
        if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot $required) -PathType Leaf)) {
            throw "Datei fehlt: $required. Zuerst setup.ps1 ausfuehren."
        }
    }
    if (-not (Test-Path -LiteralPath 'server.properties')) {
        Copy-Item -LiteralPath 'server.properties.example' -Destination 'server.properties'
    }
    Write-Host "Java: $javaPath"
    Write-Host "Paper 26.2 Build 135 | Maximaler RAM: $memory"
    Write-Host 'Standard-Verbindung: 127.0.0.1:25566 (nur dieser Computer)'
    if ($ValidateOnly) {
        Write-Host 'Startdateien und Java geprueft. Kein Server gestartet, keine EULA akzeptiert.'
        exit 0
    }

    $eulaAccepted = (Test-Path -LiteralPath 'eula.txt') -and
        (Select-String -LiteralPath 'eula.txt' -Pattern '^\s*eula\s*=\s*true\s*$' -Quiet)
    if (-not $eulaAccepted) {
        Write-Host 'Zum Start muss die Minecraft-EULA akzeptiert werden:'
        Write-Host 'https://www.minecraft.net/eula'
        $answer = Read-Host 'Hast du sie gelesen und akzeptierst sie? Tippe JA, sonst wird abgebrochen'
        if ($answer -cne 'JA') { Write-Host 'Abgebrochen. Die EULA wurde nicht akzeptiert.'; exit 0 }
        Set-Content -LiteralPath 'eula.txt' -Value 'eula=true' -Encoding ASCII
    }
    Write-Host 'Startet. Zum Beenden in der Serverkonsole stop eingeben.'
    Write-Host 'Beim ersten Start laedt Paper weitere Bibliotheken herunter.'
    # Run in this console so the user can enter server commands and stop cleanly.
    $ErrorActionPreference = 'Continue'
    & $javaPath '-Xms512M' "-Xmx$memory" '-jar' (Join-Path $PSScriptRoot 'paper-26.2-135.jar') 'nogui'
    exit $LASTEXITCODE
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
