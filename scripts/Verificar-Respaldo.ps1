param(
    [Parameter(Mandatory = $true)][string]$Archivo,
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
)

$ErrorActionPreference = 'Stop'
$archivoSql = (Resolve-Path -LiteralPath $Archivo).Path
if ([IO.Path]::GetExtension($archivoSql) -ne '.sql') {
    throw 'Seleccione un respaldo .sql generado por el minimarket.'
}
foreach ($herramienta in @('mysqld.exe', 'mysql.exe', 'mysqladmin.exe', 'mysqlcheck.exe')) {
    if (-not (Test-Path -LiteralPath (Join-Path $MySqlBin $herramienta))) {
        throw "Falta $herramienta en la carpeta de MySQL."
    }
}
$raiz = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\target\verificacion-respaldos'))
$id = [Guid]::NewGuid().ToString('N')
$carpeta = Join-Path $raiz $id
$datos = Join-Path $carpeta 'datos'
New-Item -ItemType Directory -Path $datos -Force | Out-Null
$memoria = "minimarket_prueba_$id"
$servidor = $null
$conexion = @('--no-defaults', '--protocol=MEMORY',
    "--shared-memory-base-name=$memoria", '--user=root', '--skip-password')
$hashInicial = (Get-FileHash -LiteralPath $archivoSql -Algorithm SHA256).Hash

try {
    $inicializar = Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList @(
        '--no-defaults', '--initialize-insecure', "--datadir=`"$datos`"",
        "--basedir=`"$(Split-Path $MySqlBin -Parent)`"") -WindowStyle Hidden -PassThru
    if (-not $inicializar.WaitForExit(120000)) {
        $inicializar.Kill()
        $inicializar.WaitForExit()
        throw 'La inicializacion de MySQL temporal excedio dos minutos.'
    }
    if ($inicializar.ExitCode -ne 0) { throw 'No se pudo inicializar MySQL temporal.' }

    # Sin TCP, sin configuracion global y sin eventos: no se conecta al servidor del negocio.
    $argumentosServidor = @(
        '--no-defaults', "--datadir=`"$datos`"", "--basedir=`"$(Split-Path $MySqlBin -Parent)`"",
        '--skip-networking', '--shared-memory', "--shared-memory-base-name=$memoria",
        '--mysqlx=OFF', '--event-scheduler=OFF', "--log-error=`"$(Join-Path $carpeta 'mysql.log')`"")
    $servidor = Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList $argumentosServidor -WindowStyle Hidden -PassThru
    $lista = $false
    for ($i = 0; $i -lt 60; $i++) {
        if ($servidor.HasExited) { throw 'MySQL temporal se detuvo. Revise mysql.log en la carpeta de prueba.' }
        & (Join-Path $MySqlBin 'mysqladmin.exe') @conexion '--connect-timeout=2' ping 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $lista = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $lista) { throw 'MySQL temporal no respondio en un minuto.' }

    $parametrosImportacion = @{
        FilePath = (Join-Path $MySqlBin 'mysql.exe')
        ArgumentList = ($conexion + @('--binary-mode=1', '--local-infile=0'))
        RedirectStandardInput = $archivoSql
        RedirectStandardOutput = (Join-Path $carpeta 'importacion-salida.log')
        RedirectStandardError = (Join-Path $carpeta 'importacion-error.log')
        WindowStyle = 'Hidden'
        PassThru = $true
    }
    $importacion = Start-Process @parametrosImportacion
    if (-not $importacion.WaitForExit(120000)) {
        $importacion.Kill()
        $importacion.WaitForExit()
        throw 'La importacion excedio dos minutos.'
    }
    if ($importacion.ExitCode -ne 0) { throw 'La importacion fallo. Revise importacion-error.log en la carpeta de prueba.' }

    & (Join-Path $MySqlBin 'mysqlcheck.exe') @conexion '--check' 'minimarket_db'
    if ($LASTEXITCODE -ne 0) { throw 'La verificacion de tablas fallo.' }
    $consulta = 'SELECT COUNT(*) AS tablas FROM information_schema.tables WHERE table_schema="minimarket_db"; SELECT COUNT(*) AS productos FROM minimarket_db.productos; SELECT COUNT(*) AS ventas FROM minimarket_db.ventas; SELECT COUNT(*) AS stock_negativo FROM minimarket_db.productos WHERE stock_actual<0;'
    & (Join-Path $MySqlBin 'mysql.exe') @conexion '--batch' '--execute' $consulta
    if ($LASTEXITCODE -ne 0) { throw 'La consulta de verificacion fallo.' }
    if ((Get-FileHash -LiteralPath $archivoSql -Algorithm SHA256).Hash -ne $hashInicial) {
        throw 'El archivo de respaldo cambio durante la prueba.'
    }
    Write-Output 'RESTAURACION VERIFICADA en instancia temporal sin red. Base del negocio sin modificar.'
    Write-Output "Archivos de prueba (contienen datos privados): $carpeta"
} finally {
    if ($null -ne $servidor -and -not $servidor.HasExited) {
        try {
            & (Join-Path $MySqlBin 'mysqladmin.exe') @conexion '--connect-timeout=2' shutdown 2>$null | Out-Null
        } catch {
            Write-Warning 'El servidor temporal se detendra mediante su proceso exclusivo.'
        }
        if (-not $servidor.WaitForExit(15000)) {
            $servidor.Kill()
            $servidor.WaitForExit()
        }
    }
}
