(() => {
    const producto = document.getElementById('productoId');
    if (!producto) return;

    const cantidad = document.getElementById('cantidad');
    const ayudaCantidad = document.getElementById('ayuda-cantidad');
    const ayudaCosto = document.getElementById('ayuda-costo');

    // La ayuda se inserta debajo de su etiqueta para no tapar otros campos.
    document.querySelectorAll('.ayuda-stock').forEach(contenedor => {
        const boton = contenedor.querySelector('button');
        const texto = contenedor.querySelector('[role="tooltip"]');
        const etiqueta = contenedor.closest('.etiqueta-con-ayuda');
        etiqueta.after(texto);
        let fijada = false;
        const mostrar = valor => {
            texto.hidden = !valor;
            boton.setAttribute('aria-expanded', String(valor));
        };
        // El texto personalizado reemplaza el tooltip nativo cuando hay JavaScript.
        boton.removeAttribute('title');
        boton.addEventListener('pointerenter', evento => {
            if (evento.pointerType !== 'touch') mostrar(true);
        });
        boton.addEventListener('pointerleave', () => { if (!fijada) mostrar(false); });
        boton.addEventListener('focus', () => mostrar(true));
        boton.addEventListener('blur', () => { fijada = false; mostrar(false); });
        boton.addEventListener('click', () => { fijada = !fijada; mostrar(fijada); });
        document.addEventListener('keydown', evento => {
            if (evento.key === 'Escape') { fijada = false; mostrar(false); }
        });
    });

    function actualizarAyuda() {
        const tipo = producto.selectedOptions[0]?.dataset.tipo;
        const peso = tipo === 'PESO';
        const unidad = tipo === 'UNIDAD';
        document.getElementById('etiqueta-cantidad').textContent = peso
            ? 'Cantidad ingresada (kg)' : unidad ? 'Cantidad ingresada (unidades)' : 'Cantidad ingresada';
        document.getElementById('etiqueta-costo').textContent = peso
            ? 'Costo de compra por kilo (S/)' : unidad ? 'Costo de compra por unidad (S/)' : 'Costo de compra por unidad o kilo (S/)';
        cantidad.min = unidad ? '1' : '0.01';
        cantidad.step = unidad ? '1' : '0.01';
        cantidad.placeholder = peso ? 'Ejemplo: 0.50' : unidad ? 'Ejemplo: 6' : '';
        ayudaCantidad.textContent = peso
            ? 'Ingrese solo los kilos que estan entrando. Ejemplos: 0.50 para medio kilo, 1.25 para un kilo y cuarto o 10.00 para diez kilos. Maximo dos decimales. No ingrese el stock total.'
            : unidad
                ? 'Ingrese solo las unidades que estan entrando. Ejemplo: 6 para seis unidades. Use numeros enteros, sin decimales. No ingrese el stock total.'
                : 'Seleccione un producto. Ingrese solo lo que esta entrando: kilos (0.50 para medio kilo) o unidades enteras (6 para seis unidades). No ingrese el stock total.';
        ayudaCosto.textContent = unidad
            ? 'Ingrese lo que pago por una unidad, no el precio de venta. Ejemplo: si 6 unidades costaron S/ 18.00, ingrese 3.00.'
            : peso
                ? 'Ingrese lo que pago por un kilo, no el precio de venta. Ejemplo: si 5 kg costaron S/ 20.00, ingrese 4.00.'
                : 'Seleccione un producto e ingrese lo que pago por un kilo o una unidad, no el precio de venta ni el costo de todo el ingreso.';
    }

    producto.addEventListener('change', actualizarAyuda);
    actualizarAyuda();
})();
