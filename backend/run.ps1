# uso: run.cmd | run.cmd test | run.cmd package
# busca un JDK 17+ y si no hay mvn en el PATH descarga maven en .maven
param([string]$Goal = "run")

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Get-JavaInfo([string]$javaExe) {
    # java escribe en stderr; con Stop, PowerShell 5.1 lo tomaria como error
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = & $javaExe -XshowSettings:properties -version 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = $prev

    $info = @{ Major = 0; Home = $null }
    foreach ($line in $out) {
        if ($line -match 'java\.specification\.version = (\S+)') {
            $v = $Matches[1]
            if ($v.StartsWith("1.")) { $v = $v.Substring(2) }
            $info.Major = [int]($v.Split(".")[0])
        }
        if ($line -match '^\s*java\.home = (.+)$') { $info.Home = $Matches[1].Trim() }
    }
    return $info
}

function Find-Jdk {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) { $candidates.Add((Join-Path $env:JAVA_HOME "bin\java.exe")) }
    $onPath = Get-Command java -ErrorAction SilentlyContinue
    if ($onPath) { $candidates.Add($onPath.Source) }

    $roots = @(
        "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\Zulu", "$env:ProgramFiles\Amazon Corretto", "$env:ProgramFiles\BellSoft",
        "$env:ProgramFiles\JetBrains", "$env:LOCALAPPDATA\Programs", "$env:USERPROFILE\.jdks"
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        foreach ($dir in Get-ChildItem $root -Directory -ErrorAction SilentlyContinue) {
            foreach ($rel in @("bin\java.exe", "jbr\bin\java.exe")) {
                $exe = Join-Path $dir.FullName $rel
                if (Test-Path $exe) { $candidates.Add($exe) }
            }
        }
    }

    foreach ($exe in ($candidates | Select-Object -Unique)) {
        if (-not (Test-Path $exe)) { continue }
        $info = Get-JavaInfo $exe
        if ($info.Major -ge 17 -and $info.Home) { return $info }
    }
    return $null
}

$jdk = Find-Jdk
if (-not $jdk) {
    Write-Host "No encontre Java 17 o superior." -ForegroundColor Red
    Write-Host "Instala un JDK (por ejemplo Temurin 21 desde https://adoptium.net) y abre una terminal nueva."
    exit 1
}
$env:JAVA_HOME = $jdk.Home
$env:Path = "$($jdk.Home)\bin;$env:Path"
Write-Host "Java $($jdk.Major): $($jdk.Home)"

$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $version = "3.9.9"
    $mavenDir = Join-Path $PSScriptRoot ".maven"
    $mvn = Join-Path $mavenDir "apache-maven-$version\bin\mvn.cmd"
    if (-not (Test-Path $mvn)) {
        Write-Host "Descargando Maven $version..."
        $zip = Join-Path $env:TEMP "apache-maven-$version-bin.zip"
        $url = "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
        # Expand-Archive falla en algunos Windows PowerShell 5.1, tar viene incluido desde Windows 10
        New-Item -ItemType Directory -Force -Path $mavenDir | Out-Null
        & tar.exe -xf $zip -C $mavenDir
        if ($LASTEXITCODE -ne 0) {
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            [System.IO.Compression.ZipFile]::ExtractToDirectory($zip, $mavenDir)
        }
        Remove-Item $zip
    }
}

switch ($Goal) {
    "test"    { & $mvn -B test }
    "package" { & $mvn -B package }
    default   { & $mvn -B spring-boot:run }
}
exit $LASTEXITCODE
