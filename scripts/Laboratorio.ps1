param(
    [Parameter(Mandatory=$true)][ValidateSet('Build','Start','Check')][string]$Action,
    [ValidateSet('DEV','QA','PDN')][string]$Environment = 'DEV'
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $repoRoot
$artifactRoot = Join-Path $repoRoot '.artifacts'
$pointer = Join-Path $artifactRoot 'current.json'

function Assert-NativeSuccess([string]$operation) {
    if ($LASTEXITCODE -ne 0) { throw "Fallo: $operation (codigo $LASTEXITCODE)." }
}

if ($Action -eq 'Build') {
    $commit = (& git rev-parse HEAD | Out-String).Trim()
    Assert-NativeSuccess 'leer commit'
    $pending = & git status --porcelain
    Assert-NativeSuccess 'consultar Git'
    if ($pending) { throw 'Guarda los cambios en un commit antes de construir para mantener la trazabilidad.' }
    $version = 'local-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $commit.Substring(0,8)
    $backend = Join-Path $repoRoot 'backend_proyecto'
    & docker run --rm --mount "type=bind,source=$backend,target=/workspace" --mount 'type=volume,source=futprediction_lab_maven_cache,target=/root/.m2' --workdir /workspace maven:3.9.9-eclipse-temurin-17 mvn -B -ntp "-Dlab.build.version=$version" "-Dlab.build.commit=$commit" clean verify
    Assert-NativeSuccess 'pruebas y build Java'
    Push-Location (Join-Path $repoRoot 'frontend')
    try {
        & npm.cmd ci
        Assert-NativeSuccess 'instalacion frontend'
        & npm.cmd test -- --watch=false
        Assert-NativeSuccess 'pruebas frontend'
        & npm.cmd run build -- --configuration docker
        Assert-NativeSuccess 'build frontend'
    } finally { Pop-Location }

    $releaseDir = Join-Path $artifactRoot $version
    New-Item -ItemType Directory -Path $releaseDir -ErrorAction Stop | Out-Null
    $jarPath = Join-Path $releaseDir 'app.jar'
    Copy-Item -LiteralPath (Join-Path $backend 'target/backend-proyecto-0.0.1-SNAPSHOT.jar') -Destination $jarPath
    $hash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $release = [ordered]@{ version=$version; commit=$commit; sha256=$hash; jarPath=$jarPath; directory=$releaseDir; builtAt=(Get-Date).ToUniversalTime().ToString('o') }
    $json = $release | ConvertTo-Json
    $json | Set-Content -LiteralPath (Join-Path $releaseDir 'release.json') -Encoding UTF8
    $json | Set-Content -LiteralPath $pointer -Encoding UTF8
    Copy-Item -LiteralPath (Join-Path $backend 'target/surefire-reports') -Destination (Join-Path $releaseDir 'java-tests') -Recurse
    Write-Host 'CI LOCAL COMPLETADA. Este JAR se reutilizara sin recompilar.'
    $json | Write-Output
    exit 0
}

if (-not (Test-Path -LiteralPath $pointer)) { throw 'Ejecuta primero -Action Build.' }
$release = Get-Content -LiteralPath $pointer -Raw | ConvertFrom-Json
$actualHash = (Get-FileHash -LiteralPath $release.jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $release.sha256) { throw 'El JAR cambio despues del build. Se detiene la promocion.' }
$env:LAB_JAR_PATH = $release.jarPath.Replace('\','/')
$service = $Environment.ToLowerInvariant()
$composeArgs = @('compose','--env-file','backend_proyecto/.env','-f','compose.lab.yml')

if ($Action -eq 'Start') {
    $previous = switch ($Environment) { 'QA' { 'DEV' }; 'PDN' { 'QA' }; default { $null } }
    if ($previous) {
        $evidenceFile = Join-Path $release.directory "$previous.json"
        if (-not (Test-Path -LiteralPath $evidenceFile)) { throw "Primero valida $previous con -Action Check." }
        $evidence = Get-Content -LiteralPath $evidenceFile -Raw | ConvertFrom-Json
        if ($evidence.sha256 -ne $release.sha256 -or $evidence.commit -ne $release.commit) { throw 'La evidencia previa corresponde a otro artefacto.' }
    }
    if ($Environment -eq 'PDN') {
        Write-Host "Aprobacion LOCAL para $($release.version), SHA256 $($release.sha256)"
        $answer = Read-Host 'Escribe APROBAR-PDN para desplegar esta version'
        if ($answer -cne 'APROBAR-PDN') { throw 'Despliegue no aprobado.' }
        [ordered]@{scope='LOCAL'; approvedBy=[Environment]::UserName; approvedAt=(Get-Date).ToUniversalTime().ToString('o'); commit=$release.commit; sha256=$release.sha256} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $release.directory 'approval-local.json') -Encoding UTF8
    }
    Write-Host "Iniciando $Environment en primer plano. En otra terminal ejecuta -Action Check -Environment $Environment."
    Write-Host 'Ctrl+C detiene esta etapa; los volumenes y las evidencias se conservan.'
    & docker @composeArgs up $service
    Assert-NativeSuccess 'ejecutar etapa'
    exit 0
}

$port = switch ($Environment) { 'DEV' {8082}; 'QA' {8083}; 'PDN' {8084} }
$url = "http://localhost:$port/api/v1/hello"
$response = Invoke-RestMethod -Uri $url -TimeoutSec 15
if ($response.environment -ne $Environment -or $response.version -ne $release.version -or $response.commit -ne $release.commit) { throw 'El ambiente no responde con la version y el commit esperados.' }
$containerHashLine = & docker @composeArgs exec -T $service sha256sum /app/app.jar
Assert-NativeSuccess 'verificar JAR dentro del contenedor'
$containerHash = (($containerHashLine | Out-String).Trim() -split '\s+')[0]
if ($containerHash -ne $release.sha256) { throw 'El hash dentro del contenedor no coincide con el artefacto de CI.' }
$evidence = [ordered]@{scope='LOCAL'; environment=$Environment; version=$response.version; commit=$response.commit; sha256=$containerHash; url=$url; verifiedAt=(Get-Date).ToUniversalTime().ToString('o')}
$evidence | ConvertTo-Json | Tee-Object -Variable json | Write-Output
$json | Set-Content -LiteralPath (Join-Path $release.directory "$Environment.json") -Encoding UTF8
Write-Host 'VALIDACION CORRECTA. Evidencia guardada.'
