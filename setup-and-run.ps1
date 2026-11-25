# ==============================================================================
# UniEvents — Zero-Prerequisite Windows PowerShell Setup & Launcher
# FAST-NUCES | Software Design & Analysis | Section E (23I-0716 · 25I-1022)
#
# Works on a completely fresh Windows 10 / Windows 11 PC (PowerShell 5.1 or 7+):
#   1. Installs Java JDK 21 LTS (via winget or portable Eclipse Temurin fallback)
#   2. Installs Apache Maven 3.9.9 (portable, no Admin rights required)
#   3. Installs Git (optional via winget) or fetches the UniEvents repo directly
#   4. Configures your Supabase PostgreSQL password in .env
#   5. Downloads Maven/JavaFX dependencies and launches the UniEvents Desktop App
# ==============================================================================

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$RepoUrl    = 'https://github.com/ammarasad2005/unievents-javafx-supabase.git'
$RepoZipUrl = 'https://github.com/ammarasad2005/unievents-javafx-supabase/archive/refs/heads/main.zip'
$ToolsRoot  = Join-Path $env:LOCALAPPDATA 'UniEventsTools'
$MavenVer   = '3.9.9'
$DefaultDbUrl  = 'jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0'
$DefaultDbUser = 'postgres.reoscvsfwwefkeoclkvz'

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host "    [OK] $Message" -ForegroundColor Green
}

function Write-Warn([string]$Message) {
    Write-Host "    [!]  $Message" -ForegroundColor Yellow
}

function Refresh-SessionPath {
    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $userPath    = [Environment]::GetEnvironmentVariable('Path', 'User')
    $combined    = @($userPath, $machinePath) -join ';'
    $env:Path    = $combined
}

function Add-ToUserPath([string]$DirToAdd) {
    if (-not (Test-Path $DirToAdd)) { return }
    $currentUserPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if ([string]::IsNullOrWhiteSpace($currentUserPath)) {
        $currentUserPath = ''
    }
    $parts = $currentUserPath -split ';' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($parts -notcontains $DirToAdd) {
        $newUserPath = (@($DirToAdd) + $parts) -join ';'
        [Environment]::SetEnvironmentVariable('Path', $newUserPath, 'User')
    }
    if (($env:Path -split ';') -notcontains $DirToAdd) {
        $env:Path = "$DirToAdd;$env:Path"
    }
}

function Get-JavaMajorVersion {
    try {
        $cmd = Get-Command java -ErrorAction SilentlyContinue
        if (-not $cmd) { return 0 }
        $verOutput = & java -version 2>&1 | Out-String
        if ($verOutput -match 'version "(\d+)(?:\.(\d+))?') {
            $major = [int]$Matches[1]
            if ($major -eq 1 -and $Matches[2]) {
                return [int]$Matches[2]
            }
            return $major
        }
    } catch {
        return 0
    }
    return 0
}

Clear-Host
Write-Host '====================================================================' -ForegroundColor Blue
Write-Host '   UniEvents — Automated Prerequisites Installer & App Launcher     ' -ForegroundColor White
Write-Host '   FAST-NUCES SDA Section E | JavaFX 21 + Supabase PostgreSQL JDBC  ' -ForegroundColor DarkGray
Write-Host '====================================================================' -ForegroundColor Blue

if (-not (Test-Path $ToolsRoot)) {
    New-Item -ItemType Directory -Path $ToolsRoot -Force | Out-Null
}

Refresh-SessionPath

# ------------------------------------------------------------------------------
# STEP 1: Ensure Java JDK 17+ (Installs JDK 21 LTS if missing or older than 17)
# ------------------------------------------------------------------------------
Write-Step 'Checking Java JDK (requires Java 17+)...'

# Check if we previously installed portable JDK in ToolsRoot
$existingPortableJdk = Get-ChildItem -Path $ToolsRoot -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if ($existingPortableJdk -and (Test-Path (Join-Path $existingPortableJdk.FullName 'bin\java.exe'))) {
    $env:JAVA_HOME = $existingPortableJdk.FullName
    Add-ToUserPath (Join-Path $existingPortableJdk.FullName 'bin')
}

$javaMajor = Get-JavaMajorVersion
if ($javaMajor -ge 17) {
    Write-Ok "Java $javaMajor is already available."
} else {
    Write-Warn "Java 17+ not detected (found: $javaMajor). Installing Eclipse Temurin JDK 21 LTS..."

    $installedViaWinget = $false
    if (Get-Command winget -ErrorAction SilentlyContinue) {
        try {
            Write-Host '    Attempting silent install via Windows Package Manager (winget)...' -ForegroundColor Gray
            & winget install --id EclipseAdoptium.Temurin.21.JDK --exact --silent --accept-package-agreements --accept-source-agreements
            Refresh-SessionPath

            # Detect standard Adoptium installation path for JAVA_HOME
            $adoptiumDir = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending | Select-Object -First 1
            if ($adoptiumDir) {
                $env:JAVA_HOME = $adoptiumDir.FullName
                [Environment]::SetEnvironmentVariable('JAVA_HOME', $adoptiumDir.FullName, 'User')
                Add-ToUserPath (Join-Path $adoptiumDir.FullName 'bin')
            }
            if ((Get-JavaMajorVersion) -ge 17) {
                $installedViaWinget = $true
                Write-Ok 'Installed Temurin JDK 21 via winget.'
            }
        } catch {
            Write-Warn 'winget install did not complete; switching to direct portable JDK 21 download...'
        }
    }

    if (-not $installedViaWinget) {
        $arch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' }
        $jdkApiUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/$arch/jdk/hotspot/normal/eclipse?project=jdk"
        $jdkZip    = Join-Path $ToolsRoot 'temurin-jdk21.zip'

        Write-Host "    Downloading portable Eclipse Temurin JDK 21 ($arch) to $ToolsRoot ..." -ForegroundColor Gray
        $prevProg = $ProgressPreference
        try {
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $jdkApiUrl -OutFile $jdkZip -UseBasicParsing
            Write-Host '    Extracting JDK 21 archive...' -ForegroundColor Gray
            Expand-Archive -Path $jdkZip -DestinationPath $ToolsRoot -Force
        } finally {
            $ProgressPreference = $prevProg
            Remove-Item $jdkZip -Force -ErrorAction SilentlyContinue
        }

        $portableJdk = Get-ChildItem -Path $ToolsRoot -Directory -Filter 'jdk-*' |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if (-not $portableJdk) {
            throw 'Failed to extract portable JDK 21.'
        }

        $env:JAVA_HOME = $portableJdk.FullName
        [Environment]::SetEnvironmentVariable('JAVA_HOME', $portableJdk.FullName, 'User')
        Add-ToUserPath (Join-Path $portableJdk.FullName 'bin')
        Write-Ok "Portable JDK 21 installed at $($portableJdk.FullName)"
    }
}

# Ensure JAVA_HOME is set if java came from pre-existing install
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaExe = (Get-Command java).Source
    $binDir  = Split-Path -Parent $javaExe
    if ((Split-Path -Leaf $binDir) -ieq 'bin') {
        $candidateHome = Split-Path -Parent $binDir
        if (Test-Path (Join-Path $candidateHome 'bin\javac.exe')) {
            $env:JAVA_HOME = $candidateHome
            [Environment]::SetEnvironmentVariable('JAVA_HOME', $candidateHome, 'User')
        }
    }
}
Write-Ok "JAVA_HOME = $env:JAVA_HOME"

# ------------------------------------------------------------------------------
# STEP 2: Ensure Apache Maven 3.9.9
# ------------------------------------------------------------------------------
Write-Step 'Checking Apache Maven...'

$portableMavenBin = Join-Path $ToolsRoot "apache-maven-$MavenVer\bin"
if (Test-Path (Join-Path $portableMavenBin 'mvn.cmd')) {
    Add-ToUserPath $portableMavenBin
}

if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) {
    Write-Ok "Maven is ready ($((Get-Command mvn.cmd).Source))."
} else {
    $mavenUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVer/apache-maven-$MavenVer-bin.zip"
    $mavenZip = Join-Path $ToolsRoot "apache-maven-$MavenVer-bin.zip"

    Write-Host "    Downloading Apache Maven $MavenVer (~9 MB)..." -ForegroundColor Gray
    $prevProg = $ProgressPreference
    try {
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
        Expand-Archive -Path $mavenZip -DestinationPath $ToolsRoot -Force
    } finally {
        $ProgressPreference = $prevProg
        Remove-Item $mavenZip -Force -ErrorAction SilentlyContinue
    }

    Add-ToUserPath $portableMavenBin
    Write-Ok "Apache Maven $MavenVer installed at $portableMavenBin"
}

# ------------------------------------------------------------------------------
# STEP 3: Locate or Download the UniEvents Project Repository
# ------------------------------------------------------------------------------
Write-Step 'Checking UniEvents project workspace...'

$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$ProjectDir = $null

if (Test-Path (Join-Path $ScriptDir 'pom.xml')) {
    $ProjectDir = $ScriptDir
    Write-Ok "Found project in current folder: $ProjectDir"
} elseif (Test-Path (Join-Path (Get-Location).Path 'pom.xml')) {
    $ProjectDir = (Get-Location).Path
    Write-Ok "Found project in working directory: $ProjectDir"
} elseif (Test-Path (Join-Path $ScriptDir 'unievents-javafx-supabase\pom.xml')) {
    $ProjectDir = Join-Path $ScriptDir 'unievents-javafx-supabase'
    Write-Ok "Found existing project folder: $ProjectDir"
} else {
    $TargetCloneDir = Join-Path $ScriptDir 'unievents-javafx-supabase'
    if (Get-Command git -ErrorAction SilentlyContinue) {
        Write-Host "    Cloning $RepoUrl ..." -ForegroundColor Gray
        & git clone $RepoUrl $TargetCloneDir
        $ProjectDir = $TargetCloneDir
    } else {
        Write-Host '    Git not installed — downloading repository ZIP directly from GitHub...' -ForegroundColor Gray
        $repoZip = Join-Path $ToolsRoot 'unievents-repo.zip'
        $extractTmp = Join-Path $ToolsRoot 'repo-extract'
        $prevProg = $ProgressPreference
        try {
            $ProgressPreference = 'SilentlyContinue'
            if (Test-Path $extractTmp) { Remove-Item $extractTmp -Recurse -Force }
            Invoke-WebRequest -Uri $RepoZipUrl -OutFile $repoZip -UseBasicParsing
            Expand-Archive -Path $repoZip -DestinationPath $extractTmp -Force
            $extractedInner = Join-Path $extractTmp 'unievents-javafx-supabase-main'
            Move-Item -Path $extractedInner -Destination $TargetCloneDir -Force
        } finally {
            $ProgressPreference = $prevProg
            Remove-Item $repoZip -Force -ErrorAction SilentlyContinue
            Remove-Item $extractTmp -Recurse -Force -ErrorAction SilentlyContinue
        }
        $ProjectDir = $TargetCloneDir
    }
    Write-Ok "Project ready at: $ProjectDir"
}

Set-Location $ProjectDir

# ------------------------------------------------------------------------------
# STEP 4: Configure Supabase PostgreSQL Password (.env)
# ------------------------------------------------------------------------------
Write-Step 'Checking Supabase PostgreSQL JDBC configuration...'

$EnvFile = Join-Path $ProjectDir '.env'
$ConfiguredPassword = $null

if (Test-Path $EnvFile) {
    foreach ($line in (Get-Content $EnvFile)) {
        if ($line -match '^\s*UNIEVENTS_DB_PASSWORD\s*=\s*(.+)\s*$') {
            $val = $Matches[1].Trim().Trim('"').Trim("'")
            if ($val -and $val -ne '<YOUR_SUPABASE_DB_PASSWORD>' -and $val -ne '<DATABASE_PASSWORD>') {
                $ConfiguredPassword = $val
            }
        }
    }
}

if ([string]::IsNullOrWhiteSpace($ConfiguredPassword)) {
    Write-Host ''
    Write-Host '    Supabase Project : unievents (reoscvsfwwefkeoclkvz)' -ForegroundColor Yellow
    Write-Host "    Session Pooler   : $DefaultDbUrl" -ForegroundColor DarkGray
    Write-Host "    Database User    : $DefaultDbUser" -ForegroundColor DarkGray
    Write-Host ''
    $inputPass = Read-Host '    Paste your Supabase Database Password and press Enter'
    $ConfiguredPassword = $inputPass.Trim()

    if ([string]::IsNullOrWhiteSpace($ConfiguredPassword)) {
        Write-Warn 'No password entered — set UNIEVENTS_DB_PASSWORD in .env or db.password in src/main/resources/db.properties before signing in.'
    } else {
        $envContent = @(
            "# Local Supabase JDBC Credentials (git-ignored)",
            "UNIEVENTS_DB_URL=$DefaultDbUrl",
            "UNIEVENTS_DB_USER=$DefaultDbUser",
            "UNIEVENTS_DB_PASSWORD=""$ConfiguredPassword"""
        ) -join "`r`n"
        Set-Content -Path $EnvFile -Value $envContent -Encoding UTF8
        Write-Ok "Saved Supabase password to $EnvFile (git-ignored)."
    }
} else {
    Write-Ok "Loaded existing Supabase password from $EnvFile."
}

if (-not [string]::IsNullOrWhiteSpace($ConfiguredPassword)) {
    $env:UNIEVENTS_DB_URL      = $DefaultDbUrl
    $env:UNIEVENTS_DB_USER     = $DefaultDbUser
    $env:UNIEVENTS_DB_PASSWORD = $ConfiguredPassword
}

# ------------------------------------------------------------------------------
# STEP 5: Build & Launch UniEvents JavaFX Desktop Application
# ------------------------------------------------------------------------------
Write-Step 'Compiling & launching UniEvents Desktop Application (first run downloads JavaFX & JDBC JARs)...'
Write-Host '    Demo Login Accounts (password for all accounts: demo123):' -ForegroundColor Gray
Write-Host '      - Student   : ali.raza@nuces.edu  |  fatima.noor@nuces.edu' -ForegroundColor Gray
Write-Host '      - Organizer : hamza.tariq@nuces.edu (ACM)  |  sara.malik@nuces.edu (Sports)' -ForegroundColor Gray
Write-Host '      - Admin     : ayesha.khan@unievents.edu' -ForegroundColor Gray
Write-Host ''

$MvnCmd = if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) { (Get-Command mvn.cmd).Source } else { 'mvn' }
& $MvnCmd javafx:run
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] Maven exited with code $LASTEXITCODE. Review the output above." -ForegroundColor Red
    Read-Host 'Press Enter to close'
    exit $LASTEXITCODE
}
