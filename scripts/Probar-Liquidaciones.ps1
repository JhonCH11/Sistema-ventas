param(
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin',
    [ValidateSet('LiquidacionMySqlIT','MovimientoCajaMySqlIT','CompraMySqlIT','LiquidacionMySqlIT,MovimientoCajaMySqlIT','LiquidacionMySqlIT,MovimientoCajaMySqlIT,CompraMySqlIT')]
    [string]$Pruebas = 'LiquidacionMySqlIT'
)
$ErrorActionPreference = 'Stop'
$raiz = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$datos = Join-Path $raiz ('target\prueba-liquidaciones\' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $datos -Force | Out-Null
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$puerto = $listener.LocalEndpoint.Port
$listener.Stop()
if ($puerto -eq 3306) { throw 'No se puede usar el puerto del negocio.' }
$conexion = @('--no-defaults', '--protocol=TCP', '--host=127.0.0.1', "--port=$puerto", '--user=root', '--skip-password')
$servidor = $null
try {
    $init = Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList @('--no-defaults', '--initialize-insecure', "--datadir=`"$datos`"", "--basedir=`"$(Split-Path $MySqlBin -Parent)`"") -WindowStyle Hidden -PassThru
    if (-not $init.WaitForExit(120000)) { $init.Kill(); $init.WaitForExit(); throw 'Inicializacion agotada.' }
    if ($init.ExitCode -ne 0) { throw 'Fallo al inicializar MySQL temporal.' }
    $servidor = Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList @('--no-defaults', "--datadir=`"$datos`"", "--basedir=`"$(Split-Path $MySqlBin -Parent)`"", '--bind-address=127.0.0.1', "--port=$puerto", '--mysqlx=OFF', '--event-scheduler=OFF', "--log-error=`"$(Join-Path $datos 'mysql.log')`"") -WindowStyle Hidden -PassThru
    $lista = $false
    for ($i=0; $i -lt 60; $i++) {
        if ($servidor.HasExited) { throw 'MySQL temporal se detuvo.' }
        & (Join-Path $MySqlBin 'mysqladmin.exe') @conexion '--connect-timeout=2' ping 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $lista=$true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $lista) { throw 'MySQL temporal no respondio.' }
    & (Join-Path $MySqlBin 'mysql.exe') @conexion '--execute=CREATE DATABASE minimarket_liquidacion_prueba CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
    if ($LASTEXITCODE -ne 0) { throw 'Fallo al crear la base temporal.' }
    $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -eq $maven) { $maven = Get-ChildItem -Path (Join-Path $HOME '.m2\wrapper\dists\apache-maven-*\*\bin\mvn.cmd') -ErrorAction SilentlyContinue | Select-Object -First 1 }
    if ($null -eq $maven) { throw 'Instale Maven o ejecute primero .\mvnw.cmd -DskipTests package para descargarlo.' }
    $ejecutableMaven = if ($maven -is [System.Management.Automation.CommandInfo]) { $maven.Source } else { $maven.FullName }
    Push-Location $raiz
    try {
        & $ejecutableMaven -o "-Dmaven.repo.local=$HOME\.m2\repository" "-Dtest=$Pruebas" "-Dminimarket.prueba.puerto=$puerto" test
        if ($LASTEXITCODE -ne 0) { throw 'Las pruebas MySQL fallaron.' }
    } finally { Pop-Location }
    Write-Output 'Pruebas en MySQL temporal completas. Base del negocio sin modificar.'
} finally {
    if ($null -ne $servidor -and -not $servidor.HasExited) {
        try { & (Join-Path $MySqlBin 'mysqladmin.exe') @conexion '--connect-timeout=2' shutdown 2>$null | Out-Null } catch {}
        if (-not $servidor.WaitForExit(15000)) { $servidor.Kill(); $servidor.WaitForExit() }
    }
}
