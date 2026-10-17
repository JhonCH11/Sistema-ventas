# Reglas de negocio

Este documento define las reglas funcionales confirmadas del sistema. Las reglas pendientes se completaran antes de implementar el modulo que dependa de ellas.

## Contexto

- El sistema administra ventas e inventario de un minimarket.
- Funciona localmente en una computadora, sin requerir internet para la operacion diaria.
- La base de datos es MySQL 8 local.
- La moneda usada es el sol peruano (`S/`).
- El comprobante inicial es un ticket interno y no una boleta electronica tributaria.

## Productos

- Un producto pertenece a una categoria.
- Un producto puede venderse por unidad o por peso.
- Los productos por unidad usan cantidades enteras positivas.
- Los productos por peso usan kilos y permiten como máximo dos decimales, por ejemplo `0.50 kg`, `0.25 kg` o `1.20 kg`.
- El peso se obtiene de una balanza externa y se ingresa manualmente en el sistema.
- Al registrar un producto, el stock inicial se ingresa manualmente. Para productos por peso se expresa en kilos; para productos por unidad se expresa en unidades.
- Se pueden editar nombre, categoria, forma de venta y stock minimo de un producto activo. El stock actual se modifica solo mediante movimientos de stock y el precio desde `Precios del dia` para conservar su historial.
- El sistema debe permitir registrar productos y su stock inicial desde una plantilla de Excel. La importacion valida los datos antes de guardar y reporta las filas con error.
- Los productos que ya no se comercializan se desactivan; no se eliminan si tienen ventas o movimientos asociados.

## Precios

- Cada producto tiene un precio vigente para nuevas ventas.
- Los precios pueden modificarse desde el apartado `Precios del dia`.
- Todo cambio de precio guarda fecha, producto, precio anterior y precio nuevo.
- El cambio de precio no altera ventas ya registradas.
- Si un precio cambia mientras una venta está en curso, los productos que ya fueron agregados al carrito conservan el precio mostrado al agregarlos; los agregados después usan el nuevo precio vigente.
- En una venta por peso, el subtotal se calcula como `peso en kg x precio por kg vigente`.
- Despues de multiplicar el peso por el precio por kilo, el subtotal del producto se redondea a dos decimales con redondeo comercial.
- El precio aplicado y el subtotal se guardan en el detalle de venta.

## Stock

- Toda entrada, salida o ajuste de stock debe quedar registrada con fecha, cantidad y motivo.
- El ingreso de mercaderia registra producto, cantidad, costo de compra y proveedor opcional.
- El costo de compra queda asociado al ingreso de mercaderia para permitir reportes futuros de ganancias.
- Al confirmar una venta, el sistema descuenta el stock de los productos vendidos.
- Si el stock disponible es menor que la cantidad solicitada, el sistema bloquea la venta y muestra la cantidad disponible.
- La comprobacion y el descuento de stock deben realizarse como una sola operacion para evitar sobreventas.
- Los productos por unidad manejan stock entero. Los productos por peso manejan stock decimal en kilos.
- Solo el administrador registra ajustes de stock. En conteo fisico ingresa la existencia contada; en perdida, vencimiento o dano ingresa la cantidad a retirar. Se registra la diferencia con motivo y observacion.
- Un ajuste no puede dejar stock negativo ni generar un movimiento sin diferencia. Si el stock cambio durante la edicion, se debe abrir un nuevo ajuste.
- Un producto se marca como agotado al llegar a cero y como stock bajo cuando su existencia es menor o igual al minimo configurado.
- Inicio muestra alertas para administrador y vendedor de productos activos normales: agotados a cero y stock bajo cuando la cantidad positiva es menor o igual al minimo. Se excluyen partidas de liquidacion, que no se reabastecen directamente, e inactivos. Minimo cero no genera alertas de stock bajo mientras quede producto.
- Los contadores son cantidades de productos, no sumas de kilos y unidades. Inicio muestra hasta seis alertas, priorizando agotados, y permite consultar la lista completa. Reponer abre el ingreso de stock con el producto seleccionado, sin registrar entradas automaticamente. Los avisos se recalculan al abrir o actualizar la pantalla; el minimo se modifica desde Productos.

## Ventas

- Una venta puede incluir uno o mas productos.
- Antes de confirmar, el vendedor puede corregir o retirar productos, cantidades y pesos.
- El sistema muestra el subtotal de cada producto y el total de la venta antes de confirmar.
- Una venta confirmada conserva el nombre del producto, unidad de venta, cantidad o peso, precio aplicado y subtotal, aunque el producto o su precio cambien despues.
- El sistema debe impedir que un doble clic o reintento cree una venta duplicada.
- Una venta confirmada no se elimina fisicamente.

## Anulaciones y devoluciones

- La anulacion de una venta devuelve el stock de sus productos automaticamente.
- Las devoluciones se permiten despues de confirmar una venta.
- En cada devolucion, el administrador debe indicar si el producto esta en buen estado y se reincorpora al stock vendible, o si esta danado y queda fuera del stock vendible. La decision es obligatoria y se guarda en el historial junto con su autorizacion.
- El motivo y el estado del producto se eligen por separado. Si el motivo es producto en mal estado, no se permite reincorporarlo al stock vendible.
- La anulacion completa se permite en la caja abierta original y si no hubo devoluciones. Una venta con devoluciones se corrige devolviendo lo restante.
- Las devoluciones parciales no pueden superar la cantidad pendiente. El reembolso usa el precio historico y la ultima devolucion recupera el importe restante para evitar diferencias de redondeo.
- Todo reembolso se asocia a la caja abierta actual. Los reembolsos de ventas en efectivo reducen el efectivo esperado; Yape y Plin se restituyen manualmente por el mismo medio.
- Las anulaciones y devoluciones requieren permiso de administrador y deben registrar fecha, usuario y motivo.
- Cuando un vendedor solicite una anulacion o devolucion, el administrador autoriza la operacion ingresando su clave sin necesidad de cerrar sesion.

## Liquidaciones y mermas

- Solo el administrador separa mercaderia para liquidacion y registra mermas. No hay plazos ni traslados automaticos: la decision depende del producto y su estado.
- La cantidad elegida se transfiere del producto normal a una presentacion identificada como Liquidacion L(numero), con la misma unidad y precio inferior al normal al momento de separarla. El stock total no aumenta ni se registra perdida por el traslado.
- La presentacion aparece en Nueva venta y usa las validaciones, pagos, tickets y devoluciones existentes. No recibe nuevas entradas ni se edita como producto normal; una nueva partida se separa desde el producto original.
- Al crear la partida se fija el precio reducido. Los cambios posteriores siguen centralizados en Precios del dia, sin modificar ventas pasadas; deben permanecer por debajo del precio normal de referencia guardado.
- Se registra administrador, fecha de separacion, cantidad, costo de compra y motivo. La fecha de llegada es opcional y manual; no se deduce del ultimo ingreso ni se afirma una antiguedad exacta para stock mezclado. No se implementa seguimiento FIFO de lotes originales.
- La merma puede salir del producto normal o de una liquidacion; el administrador elige cantidad y motivo (deterioro, vencimiento, dano o perdida). No venderse por si solo no convierte el producto en merma.
- Se descuenta stock una sola vez, con transaccion, bloqueo del producto y token contra reintentos. No se retira mas de lo disponible; kilos con dos decimales y unidades enteras.
- La perdida se calcula como cantidad por costo de compra, con dos decimales HALF_UP. En liquidaciones usa el costo guardado; en productos normales se sugiere el ultimo costo ingresado, pero el administrador debe confirmarlo o corregirlo para la mercaderia retirada. Si no hay costo registrado, debe ingresarlo; no se usa precio de venta ni se inventa un costo.
- El reporte consulta mermas por fecha de registro y agrupa perdidas por producto original y motivo. Conserva nombre, unidad, cantidad, costo y perdida historicos. No incluye retroactivamente ajustes antiguos ni devoluciones danadas que no se hayan registrado como merma; no es un calculo completo de utilidad ni del descuento comercial de liquidacion.

## Usuarios y permisos

- Existen dos perfiles iniciales: vendedor y administrador.
- El administrador gestiona colaboradores desde un apartado exclusivo: crea cuentas de vendedor, cambia sus contrasenas y activa o desactiva su acceso. No se eliminan usuarios ni su historial.
- Puede editar el nombre y el usuario del colaborador, sin cambiar su clave, perfil, estado ni identificador historico. El usuario debe ser unico entre cuentas activas e inactivas. Al cambiarlo, las sesiones con el usuario anterior dejan de operar en la siguiente solicitud; el colaborador ingresa con el nuevo usuario y su misma clave.
- Desactivar una cuenta impide nuevos ingresos y bloquea su siguiente solicitud si ya tenia sesion abierta. Cambiar una contrasena se aplica a futuros ingresos; no cierra sesiones existentes.
- El vendedor no puede acceder a gestion de colaboradores, precios, ajustes de stock, reportes, configuracion ni extensiones de horario. Las anulaciones y devoluciones mantienen la autorizacion administrativa existente.
- El vendedor puede registrar productos, ventas, ingresos de stock y apertura y cierre de caja.
- El administrador puede realizar las funciones del vendedor, anular ventas, aceptar devoluciones, ajustar stock, modificar precios y revisar reportes.
- Las credenciales de usuarios se almacenan de forma segura; las contrasenas no se guardan en texto visible.

## Motivos de control

- Los motivos predefinidos para ajuste de stock son: conteo fisico, producto vencido, producto danado y perdida.
- Los motivos predefinidos para anulacion de venta son: error al registrar, cliente cancelo antes de retirar productos y pago no confirmado.
- Los motivos predefinidos para devolucion son: producto en mal estado, producto equivocado y cliente cambio de opinion.
- El usuario puede agregar una observacion corta cuando el motivo necesite mayor detalle.

## Pagos

- Los medios de pago iniciales son efectivo, Yape y Plin.
- Cada venta usa un solo medio de pago en la primera version.
- Para efectivo, el vendedor ingresa el importe recibido y el sistema calcula el vuelto antes de confirmar.
- El importe recibido en efectivo no puede ser menor al total de la venta.
- Para Yape o Plin, el vendedor verifica el abono en el celular del negocio antes de confirmar la venta.
- Yape y Plin se registran manualmente; el sistema no usa pasarelas de pago ni valida transferencias de forma automatica.

## Caja

- Historial de caja es una consulta de solo lectura exclusiva del administrador. Filtra por fecha de operacion, con extremos incluidos y hasta 366 dias por consulta; inicialmente muestra el mes actual.
- El listado muestra una fila por apertura y solo los importes del cierre vigente. Una caja reabierta sin nuevo cierre se muestra sin cierre vigente; sus cierres previos quedan en el detalle. No sumar entre si cierres de la misma jornada.
- El detalle conserva apertura, todos los cierres y sus diferencias/observaciones, autorizaciones de reapertura, extensiones de horario y movimientos de efectivo con sus correcciones. Los importes de cierre se leen de sus valores guardados, sin recalcularlos con datos posteriores. Los nombres mostrados son los nombres actuales de los usuarios registrados.

- Horario habitual: todos los dias de 06:00 a 21:00, zona America/Lima.
- Inicio, Nueva venta y Caja muestran reloj y avisos; el recordatorio comienza 30 minutos antes del cierre previsto.
- Solo el administrador puede autorizar una extension para la caja abierta del dia, indicando nueva hora y motivo. Se registra administrador, horario anterior, nuevo horario y fecha de autorizacion.
- La extension termina como maximo a las 23:59 del mismo dia; no cambia el horario habitual de dias siguientes.
- Los avisos no cierran automaticamente la caja ni interrumpen ventas. El cierre sigue siendo manual.

- Cada dia debe iniciar con una apertura de caja que registre el efectivo inicial.
- Al finalizar el dia, se realiza un cierre de caja con el efectivo contado.
- Solo el administrador puede reabrir una caja cerrada del mismo dia, con motivo obligatorio. Se conserva el cierre previo y se registra quien autorizo la reapertura y cuando; no se borran ventas ni cierres.
- Se mantiene la apertura original y su efectivo inicial. El nuevo cierre calcula el efectivo acumulado de toda la jornada, sin sumar dos veces el fondo inicial. Cualquier dinero retirado debe registrarse en Movimientos de caja antes del cierre; reabrir no repone automaticamente efectivo retirado.
- La reapertura no extiende automaticamente el horario: el administrador usa luego Extender horario de hoy si corresponde. No se reabren jornadas anteriores ni se extiende mas alla de medianoche.
- El efectivo esperado es: fondo inicial + ventas confirmadas en efectivo - reembolsos en efectivo + ingresos adicionales - retiros - gastos + correcciones de movimientos. Yape y Plin no forman parte del dinero fisico de caja.
- Solo el administrador puede registrar ingresos adicionales, retiros y gastos desde Movimientos de caja, con importe positivo de hasta dos decimales, motivo, responsable y fecha/hora de Peru. Estos movimientos no modifican las ventas ni el stock.
- Se permiten movimientos y correcciones solamente en la caja abierta de hoy. Un formulario de otra apertura no es valido. Los retiros, gastos y correcciones con salida se bloquean si superan el efectivo esperado disponible.
- Los movimientos no se editan ni eliminan: una correccion registra el efecto inverso con motivo y referencia al original. Cada original puede corregirse una sola vez; no se corrige una correccion. Si corresponde, despues se registra el movimiento correcto.
- Cada operacion usa un token unico y se serializa con ventas, reembolsos, cierre y reapertura mediante el bloqueo de la apertura. El cierre conserva la foto del efectivo esperado de ese momento; los movimientos se acumulan una sola vez al volver a cerrar tras una reapertura.
- El cierre debe registrar cualquier diferencia y su observacion.

## Ticket interno

- Al confirmar una venta, el sistema genera un ticket interno.
- El ticket incluye numero de venta, fecha y hora, productos, cantidades o pesos, precios, total y medio de pago.
- Para efectivo, el ticket incluye importe recibido y vuelto.
- El ticket puede reimprimirse desde `Ventas realizadas`.
- Reimprimir un ticket no crea una venta nueva ni modifica el stock.
- El ticket se imprime localmente desde el navegador en la primera version.
- Se puede elegir contenido para ticket de 80 mm o A4. La impresora y el papel se seleccionan en el dialogo del navegador; el sistema no puede confirmar la impresion fisica.
- Cancelar el dialogo o repetir la impresion no modifica ventas, pagos ni existencias.

## Configuracion del negocio

- El nombre, direccion y telefono del minimarket se pueden configurar y se muestran en el ticket.
- El RUC no se mostrara inicialmente.

## Respaldos

- En la primera version, los respaldos de la base de datos se guardan en una carpeta local configurable.
- El respaldo se genera al cierre diario de caja y su nombre incluye la fecha.
- La restauracion de un respaldo requiere permiso de administrador.
- Una copia remota mediante internet se evaluara como mejora posterior y no sera necesaria para operar el sistema.

## Ventas realizadas, anulaciones y devoluciones

- El sistema permite consultar ventas realizadas por fecha y ver su detalle.
- Los reportes deben diferenciar ventas vigentes, anuladas y devueltas.
- La consulta muestra el total vigente neto de devoluciones de las ventas seleccionadas.
- Reportes selecciona ventas por fecha de venta con ambos dias incluidos. Muestra total original, anulaciones y devoluciones acumuladas de esas ventas, y su neto actual por medio de pago.
- Los reembolsos realizados durante el periodo se consultan por fecha de devolucion, incluidos los correspondientes a ventas anteriores. Son una medida separada del neto de las ventas seleccionadas.

## Resumen diario

- Consulta exclusiva del administrador por fecha comercial de Peru.
- Distingue cobros de ventas vigentes del dia (antes de devoluciones), neto actual de esas ventas y reembolsos efectivamente realizados ese dia, incluso de ventas anteriores. No restar reembolsos nuevamente al neto actual.
- Muestra mermas registradas ese dia valorizadas al costo de compra; no presenta estos indicadores como ganancia.
- Incluye medios de pago y caja. Si hay cierre vigente, conserva sus importes guardados; en caja abierta muestra el efectivo esperado actual. Sin apertura no inventa importes de caja.
- Ingresos, retiros y gastos incluyen el efecto inverso de sus correcciones; Yape y Plin no forman parte del efectivo fisico.

## Importacion desde Excel

- Solo el administrador puede descargar plantillas, revisar y confirmar importaciones. Operacion local con archivos `.xlsx`, una hoja, hasta 2 MB y 500 filas de datos.
- Flujo: descargar plantilla, completar, cargar con referencia unica, revisar errores y confirmar. La revision no cambia la base de datos; la confirmacion usa los datos guardados en la sesion del servidor y vence a los 30 minutos.
- Plantilla Productos: `nombre`, `categoria`, `tipo_venta`, `precio`, `stock_minimo`. Tipos `PESO` o `UNIDAD`; la categoria debe existir y estar activa. Solo crea productos nuevos con stock cero; no sobrescribe productos ni cambia precios existentes. Estos se modifican en Precios del dia.
- Plantilla Stock: `producto`, `cantidad`, `costo_unitario`, `observacion`. El producto debe existir, estar activo y no ser de liquidacion. El costo es por kilo o unidad segun el producto. Cada entrada genera su movimiento de mercaderia.
- Usar nombres completos de productos y categorias. Cantidades por unidad enteras; pesos e importes con hasta dos decimales. Se aceptan numeros o texto decimal con punto/coma sin separador de miles; no se admiten formulas, fechas ni columnas adicionales.
- No se confirma un archivo con errores. Todas sus filas se guardan en una transaccion: un fallo revierte la importacion completa. Se bloquean productos antes de actualizar existencias para no perder ventas o entradas concurrentes.
- El token evita repetir una confirmacion. Tambien se rechaza una referencia o el mismo contenido de archivo ya importado para el mismo tipo, aunque se cambie el nombre del archivo. Para otra entrega usar referencia nueva y contenido identificable, por ejemplo una observacion de entrega distinta.
- Se registra tipo, referencia, huella del archivo, responsable, fecha y numero de filas.

## Compras y proveedores

- Solo el administrador gestiona Compras y proveedores. Proveedores registra nombre, telefono opcional y estado activo; permite editar y desactivar, no eliminar. Los nombres son unicos.
- Nueva compra permite entre 1 y 50 productos distintos, proveedor opcional, documento opcional y observacion. Si se indica documento, se requiere proveedor. No se usa RUC ni se integra facturacion tributaria.
- Cada linea contiene producto existente y activo, cantidad recibida y costo por kilo o unidad. No admite productos de liquidacion. Unidades enteras, kilos y costos con hasta dos decimales, valores positivos y maximo diez cifras enteras. El total tiene el mismo limite monetario.
- El servidor calcula cada subtotal como cantidad por costo, redondeado a dos decimales HALF_UP, y suma los subtotales. No recibe un total confiable desde el navegador. El precio de venta no se modifica.
- Confirmar crea un unico ingreso de mercaderia con todos sus detalles y movimientos de stock dentro de la misma transaccion. Bloquea los productos en orden estable antes de sumar existencias; un error revierte toda la compra.
- No volver a ingresar en Stock o Excel mercaderia ya registrada como compra: esos caminos tambien suman stock y no se deduce automaticamente si representan la misma entrega.
- Un token de confirmacion evita duplicados por doble clic o reintento. Para un mismo proveedor no se puede repetir un documento informado. Sin documento no se detecta una entrega registrada manualmente como otra compra distinta; el administrador debe revisar el historial.
- Se conservan nombres de proveedor y productos, tipo de venta, cantidades y costos de la nueva compra. Cambiar nombres despues no modifica estos datos historicos. El responsable mostrado es su nombre actual.
- El historial selecciona fechas de registro de Peru, extremos incluidos y hasta 366 dias; muestra hasta 300 registros mas recientes e indica si se alcanza ese limite. Incluye ingresos simples y Excel existentes sin volver a ingresarlos; en datos antiguos sin nombre historico se muestra el nombre actual.
- Las compras confirmadas se consultan, no se editan ni eliminan. Devoluciones a proveedores y correcciones de compras quedan pendientes de definir; no se inventan salidas de stock para esos casos.
- El registro de compra no es un comprobante de pago y no descuenta dinero de caja automaticamente. Pagos a proveedores, credito, deudas y su vinculacion con Movimientos de caja quedan pendientes de confirmacion.
- No se calcula todavia utilidad ni costo promedio/FIFO de lo vendido; este historial conserva costos de entrada para una decision posterior.

## Reglas pendientes

