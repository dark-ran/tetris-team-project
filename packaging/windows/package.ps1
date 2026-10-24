param(
    [Parameter(Mandatory = $true, Position = 0)][string]$Jdk,
    [Parameter(Mandatory = $true, Position = 1)][string]$InputDir,
    [Parameter(Mandatory = $true, Position = 2)][string]$JarName,
    [Parameter(Mandatory = $true, Position = 3)][string]$OutputDir
)

$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT' -or $env:PROCESSOR_ARCHITECTURE -ne 'AMD64') {
    throw 'Run packageWindows on Windows x64 with an x64 JDK 21.'
}
$java = Join-Path $Jdk 'bin\java.exe'
$jpackage = Join-Path $Jdk 'bin\jpackage.exe'
foreach ($tool in @($java, $jpackage)) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "Missing Java 21 tool: $tool"
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $InputDir $JarName) -PathType Leaf)) {
    throw "Missing application JAR: $JarName"
}

# Match the macOS packaging approach: assemble outside build/, then copy and archive.
$work = Join-Path ([System.IO.Path]::GetTempPath()) ('tetris-windows-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null
try {
    $icon = Join-Path $work 'Tetris.ico'
    & $java '-Djava.awt.headless=true' (Join-Path $PSScriptRoot 'CreateIcon.java') $icon
    if ($LASTEXITCODE -ne 0) { throw "Icon generator failed (exit $LASTEXITCODE)" }

    & $jpackage --type app-image --name Tetris --app-version 1.0.0 `
        --vendor 'Tetris Team' `
        --description 'SeoulTech Software Engineering Tetris Team Project' `
        --input $InputDir --main-jar $JarName --main-class tetris.Main `
        --icon $icon --add-modules 'java.desktop,jdk.random' `
        --jlink-options '--strip-debug --no-man-pages --no-header-files' `
        --java-options '-Xmx256m' --dest $work
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed (exit $LASTEXITCODE)" }

    $builtApp = Join-Path $work 'Tetris'
    if (-not (Test-Path -LiteralPath (Join-Path $builtApp 'Tetris.exe') -PathType Leaf)) {
        throw 'jpackage did not produce Tetris.exe.'
    }

    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Copy-Item -LiteralPath $builtApp -Destination $OutputDir -Recurse -Force
    $app = Join-Path $OutputDir 'Tetris'
    if (-not (Test-Path -LiteralPath (Join-Path $app 'runtime\bin\javaw.exe') -PathType Leaf)) {
        throw 'Bundled Java runtime is missing.'
    }

    $zip = Join-Path $OutputDir 'Tetris-windows-x64.zip'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $app, $zip, [System.IO.Compression.CompressionLevel]::Optimal, $true
    )
    $archive = [System.IO.Compression.ZipFile]::OpenRead($zip)
    try {
        $foundExe = $false
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.Replace('\', '/') -eq 'Tetris/Tetris.exe') { $foundExe = $true; break }
        }
        if (-not $foundExe) { throw 'ZIP is missing Tetris/Tetris.exe.' }
    } finally {
        $archive.Dispose()
    }

    $stream = [System.IO.File]::OpenRead($zip)
    try {
        $digest = [System.Security.Cryptography.SHA256]::Create()
        try {
            $hash = [System.BitConverter]::ToString($digest.ComputeHash($stream)).Replace('-', '').ToLowerInvariant()
        } finally {
            $digest.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
    $sha = Join-Path $OutputDir 'Tetris-windows-x64.zip.sha256'
    [System.IO.File]::WriteAllText($sha, "$hash  Tetris-windows-x64.zip`n",
        [System.Text.Encoding]::ASCII)
} finally {
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}
