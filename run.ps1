$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Test-Jdk21Plus {
    param([string]$JavaExe)
    if (-not $JavaExe -or -not (Test-Path -LiteralPath $JavaExe)) {
        return $false
    }
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $version = & $JavaExe -version 2>&1 | Out-String
    } finally {
        $ErrorActionPreference = $previous
    }
    return $version -match 'version "(2[1-9]|[3-9]\d)'
}

function Get-JdkHomeFromJavaExe {
    param([string]$JavaExe)
    return (Split-Path (Split-Path -Path $JavaExe -Parent) -Parent)
}

function Find-JdkHome {
    if ($env:JAVA_HOME) {
        $javaHomeExe = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Jdk21Plus $javaHomeExe) {
            return $env:JAVA_HOME
        }
    }

    $javaCommands = @(Get-Command java -All -ErrorAction SilentlyContinue)
    foreach ($cmd in $javaCommands) {
        if (Test-Jdk21Plus $cmd.Source) {
            return (Get-JdkHomeFromJavaExe $cmd.Source)
        }
    }

    $localRoots = @(
        "C:\Software\java",
        "C:\software\java",
        (Join-Path $env:USERPROFILE ".jdks")
    )
    foreach ($root in $localRoots) {
        if (-not $root -or -not (Test-Path -LiteralPath $root)) { continue }
        $candidates = Get-ChildItem -Path $root -Recurse -Filter java.exe -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '\\bin\\java.exe$' }
        foreach ($candidate in $candidates) {
            if (Test-Jdk21Plus $candidate.FullName) {
                return (Get-JdkHomeFromJavaExe $candidate.FullName)
            }
        }
    }

    return $null
}

$jdk = Find-JdkHome
if (-not $jdk) {
    Write-Error "JDK 21 or newer was not found. Install one, set JAVA_HOME to it, or put its bin directory on PATH. The Gradle toolchain in build.gradle is Java 27."
}

$env:JAVA_HOME = $jdk
$env:Path = "$(Join-Path $jdk 'bin');$env:Path"
Write-Host "JAVA_HOME=$env:JAVA_HOME"
& java -version

try {
    $tags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5
    $names = @($tags.models | ForEach-Object { $_.name }) -join ", "
    if (-not $names) { $names = "(none)" }
    Write-Host "Ollama is up. Models: $names"
} catch {
    Write-Warning "Ollama is not responding at http://localhost:11434. Start Ollama and pull the model before sending a chat request."
}

& .\gradlew.bat bootRun
