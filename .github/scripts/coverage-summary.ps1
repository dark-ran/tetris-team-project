param(
    [string]$ReportPath = 'build/reports/jacoco/test/jacocoTestReport.xml',
    [string]$OutputPath = 'build/reports/jacoco/test/coverage-summary.md',
    [string]$TestResultsPath = 'build/test-results/test'
)

$ErrorActionPreference = 'Stop'

function Get-CoverageText($Node, [string]$Type) {
    $counter = @($Node.counter) | Where-Object { $_.type -eq $Type }
    $covered = [int]$counter.covered
    $total = $covered + [int]$counter.missed
    if ($total -eq 0) { return 'N/A' }
    return '{0:F1}% ({1}/{2})' -f (100.0 * $covered / $total), $covered, $total
}

if (Test-Path -LiteralPath $ReportPath) {
    # Read JaCoCo's DOCTYPE without fetching its external DTD.
    $readerSettings = [System.Xml.XmlReaderSettings]::new()
    $readerSettings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
    $readerSettings.XmlResolver = $null
    $reader = [System.Xml.XmlReader]::Create(
        [System.IO.Path]::GetFullPath($ReportPath), $readerSettings
    )
    try {
        $document = [System.Xml.XmlDocument]::new()
        $document.XmlResolver = $null
        $document.Load($reader)
    } finally {
        $reader.Dispose()
    }

    $rows = [System.Collections.Generic.List[string]]::new()
    $rows.Add('## JaCoCo coverage')
    $rows.Add('')
    $rows.Add('| Scope | Line coverage (covered/total) | Branch coverage (covered/total) |')
    $rows.Add('|---|---:|---:|')
    $line = Get-CoverageText $document.report 'LINE'
    $branch = Get-CoverageText $document.report 'BRANCH'
    $rows.Add("| **Total** | **$line** | **$branch** |")
    $notice = "Line coverage: $line; Branch coverage: $branch"
    $sourceFiles = foreach ($package in $document.report.package) {
        foreach ($source in $package.sourcefile) {
            [pscustomobject]@{ Path = "$($package.name)/$($source.name)"; Node = $source }
        }
    }
    foreach ($sourceFile in ($sourceFiles | Sort-Object Path)) {
        $line = Get-CoverageText $sourceFile.Node 'LINE'
        $branch = Get-CoverageText $sourceFile.Node 'BRANCH'
        $rows.Add("| $($sourceFile.Path) | $line | $branch |")
    }
    $rows.Add('')
    $rows.Add('Coverage measures executed code. N/A means there are no counters of that type.')
    $summary = ($rows -join "`n") + "`n"
} else {
    $summary = "## JaCoCo coverage`n`nReport unavailable. Check the build and test logs.`n"
    $notice = 'Report unavailable. Check the build and test logs.'
}

$testFiles = @(Get-ChildItem -LiteralPath $TestResultsPath -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue)
if ($testFiles.Count -gt 0) {
    $tests = 0
    $failures = 0
    $errors = 0
    $skipped = 0
    foreach ($testFile in $testFiles) {
        [xml]$testReport = Get-Content -LiteralPath $testFile.FullName -Raw
        $tests += [int]$testReport.testsuite.tests
        $failures += [int]$testReport.testsuite.failures
        $errors += [int]$testReport.testsuite.errors
        $skipped += [int]$testReport.testsuite.skipped
    }
    $passed = $tests - $failures - $errors - $skipped
    $summary += "`n## Tests`n`n| Passed | Failed | Errors | Skipped | Total |`n|---:|---:|---:|---:|---:|`n| $passed | $failures | $errors | $skipped | $tests |`n"
} else {
    $summary += "`n## Tests`n`nTest results unavailable. Check the build and test logs.`n"
}

$outputDirectory = Split-Path -Parent $OutputPath
if ($outputDirectory) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}
[System.IO.File]::WriteAllText(
    [System.IO.Path]::GetFullPath($OutputPath), $summary, [System.Text.UTF8Encoding]::new($false)
)
if ($env:GITHUB_STEP_SUMMARY) {
    Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value $summary -Encoding utf8
}
if ($env:GITHUB_ACTIONS -eq 'true') {
    $escapedNotice = $notice.Replace('%', '%25')
    Write-Output "::notice title=JaCoCo coverage::$escapedNotice"
}
Write-Output $summary
Write-Output "Coverage summary saved to $OutputPath"
