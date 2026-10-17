# Sistema de ventas para minimarket

## Requisitos

- Java JDK 21 o superior. Verificar con `java -version`.
- MySQL Server 8 instalado y en funcionamiento.
- Navegador web.
- Maven incluido mediante `mvnw` y `mvnw.cmd`, o Maven 3.9 instalado.

La primera compilacion requiere internet para descargar Maven y las dependencias. Con las dependencias disponibles, el programa funciona localmente sin internet. No incluye una base de datos con ventas, usuarios ni existencias.

## Crear la base de datos

En una instalacion nueva, abrir MySQL Workbench o el cliente de MySQL con una cuenta administradora y ejecutar:

```sql
CREATE DATABASE minimarket_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER 'minimarket_app'@'localhost'
    IDENTIFIED BY 'CAMBIA_ESTA_CONTRASENA';

GRANT ALL PRIVILEGES ON minimarket_db.*
    TO 'minimarket_app'@'localhost';
```

Reemplazar `CAMBIA_ESTA_CONTRASENA` por una contrasena propia antes de ejecutar. No conceder permisos sobre otras bases. Si la base o el usuario ya existen, no eliminarlos: revisar la configuracion existente.

No crear tablas manualmente ni ejecutar los archivos de migracion por separado. Flyway aplica las migraciones de `src/main/resources/db/migration` al iniciar el programa y conserva su historial. No editar migraciones ya aplicadas.

## Configurar la conexion

Desde la raiz del proyecto, en PowerShell:

```powershell
Copy-Item src/main/resources/application.properties.example src/main/resources/application.properties
```

Abrir `src/main/resources/application.properties` y completar la conexion usando la contrasena elegida al crear el usuario:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/minimarket_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Lima
spring.datasource.username=minimarket_app
spring.datasource.password=CAMBIA_ESTA_CONTRASENA
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

Conservar las otras opciones del archivo de ejemplo. La configuracion local esta excluida de Git. Nunca subirla con credenciales reales. Si se cambia el nombre de la base, actualizar tanto su creacion como la URL de conexion y los permisos.

## Iniciar el programa

Desde una sola terminal en la raiz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

Con Maven instalado tambien se puede usar:

```text
mvn spring-boot:run
```

En Linux o macOS, usar `sh mvnw spring-boot:run` y adaptar las rutas locales. Los scripts `.ps1` de verificacion requieren Windows.

Esperar el mensaje `Started MinimarketApplication` y abrir:

```text
http://localhost:8080
```

En una base nueva se solicita crear el primer administrador. Los productos y las existencias se registran en la aplicacion. La cuenta administradora del programa es independiente del usuario de MySQL.

El servidor queda limitado a `127.0.0.1` en el archivo de ejemplo. `localhost` corresponde a la computadora donde se ejecuta el programa; no permite acceder desde otro equipo o celular.

Para detenerlo, presionar `Ctrl + C` en la terminal. Si Windows pregunta si desea terminar el trabajo por lotes, responder `S` y Enter.

## Generar un ejecutable Java

```powershell
.\mvnw.cmd -DskipTests package
java -jar target/minimarket-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:./src/main/resources/application.properties
```

Estos comandos se ejecutan desde la raiz del proyecto, despues de configurar la conexion. No distribuir un JAR compilado con credenciales locales en sus recursos. Para una distribucion sin credenciales, compilar sin `src/main/resources/application.properties` y usar un archivo externo al ejecutar.

## Impresion

Instalar la impresora en el sistema operativo. En un ticket elegir 80 mm o A4 y pulsar Imprimir o reimprimir. Seleccionar impresora y papel en el dialogo del navegador, desactivar sus encabezados y pies de pagina y revisar la vista previa. Guardar como PDF permite comprobar el formato sin impresora.

## Respaldos

Los respaldos requieren `mysqldump`, incluido con MySQL Server. Ajustar esta opcion a la ruta de instalacion si no se encuentra en PATH:

```properties
minimarket.respaldos.mysqldump=C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe
```

En Configuracion, elegir una carpeta local con permisos de escritura. Se genera un respaldo al cerrar caja y se puede reintentar con Crear respaldo ahora. Una falla del respaldo no vuelve a abrir la caja ni requiere repetir el cierre.

Los respaldos contienen datos privados y deben permanecer fuera del repositorio. Una copia en el mismo disco no protege contra la averia de ese disco. Antes de actualizar una instalacion con datos reales, crear y verificar un respaldo.

Para verificar una copia de confianza en una instancia temporal separada:

```powershell
.\scripts\Verificar-Respaldo.ps1 -Archivo 'C:\RespaldosMinimarket\NOMBRE_DEL_RESPALDO.sql'
```

El script no restaura sobre la base del negocio. Conserva resultados privados dentro de `target`, que no debe compartirse. Adaptar `-MySqlBin` si las herramientas de MySQL estan en otra carpeta.

## Pruebas

Pruebas unitarias sin conexion a la base del negocio:

```powershell
.\mvnw.cmd test
```

Pruebas de integracion con MySQL temporal independiente, en Windows y despues de descargar las dependencias:

```powershell
.\scripts\Probar-Liquidaciones.ps1 -Pruebas 'LiquidacionMySqlIT,MovimientoCajaMySqlIT,CompraMySqlIT'
```

El script requiere MySQL Server 8 y Maven disponible en PATH o descargado por el wrapper. Crea datos ficticios dentro de `target`, usa otro puerto y detiene su propia instancia al finalizar. No necesita una base real ni credenciales del negocio.

## Problemas de inicio

- Puerto 8080 ocupado: detener la instancia anterior antes de iniciar otra. No ejecutar el programa simultaneamente desde IntelliJ y otra terminal.
- Acceso denegado a MySQL: comprobar usuario, contrasena, host y permisos de la base configurada.
- No se puede conectar con MySQL: comprobar que el servicio este iniciado y que la URL use su puerto correcto.
- Base desconocida: crear primero la base vacia indicada en la URL.
- Error de migracion: conservar los datos, revisar el mensaje concreto y no borrar tablas ni el historial para ocultar el error.
