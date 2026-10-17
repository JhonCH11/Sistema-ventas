(() => {
    const form = document.querySelector('#form-compra');
    if (!form) return;
    const lista = document.querySelector('#lineas-compra');
    const agregar = document.querySelector('#agregar-linea');
    const confirmar = document.querySelector('#confirmar-compra');
    const moneda = v => `S/ ${v / 100n}.${String(v % 100n).padStart(2, '0')}`;
    const centavos = valor => {
        if (!/^\d{1,10}(\.\d{1,2})?$/.test(valor)) return 0n;
        const [entero, decimal = ''] = valor.split('.');
        return BigInt(entero) * 100n + BigInt(decimal.padEnd(2, '0'));
    };
    function actualizar() {
        let total = 0n;
        const filas = [...lista.children];
        filas.forEach((fila, i) => {
            const producto = fila.querySelector('select');
            const cantidad = fila.querySelector('.cantidad-compra');
            const costo = fila.querySelector('.costo-compra');
            const tipo = producto.selectedOptions[0]?.dataset.tipo;
            fila.querySelector('.unidad-cantidad').textContent = tipo === 'PESO' ? 'Cantidad recibida (kg)' : tipo === 'UNIDAD' ? 'Cantidad recibida (unidades)' : 'Cantidad recibida';
            fila.querySelector('.unidad-costo').textContent = tipo === 'PESO' ? 'Costo por kilo (S/)' : tipo === 'UNIDAD' ? 'Costo por unidad (S/)' : 'Costo por kg/unidad (S/)';
            cantidad.step = tipo === 'UNIDAD' ? '1' : '0.01';
            cantidad.min = tipo === 'UNIDAD' ? '1' : '0.01';
            [cantidad, costo].forEach(campo => campo.setCustomValidity(campo.value && !/^\d{1,10}(\.\d{1,2})?$/.test(campo.value)
                ? 'Ingrese un numero con hasta dos decimales, sin exponentes.' : ''));
            [producto, cantidad, costo].forEach(campo => {
                campo.name = campo.name.replace(/lineas\[\d+\]/, `lineas[${i}]`);
                campo.id = `${campo.className}-${i}`;
            });
            const subtotal = (centavos(cantidad.value) * centavos(costo.value) + 50n) / 100n;
            fila.querySelector('output').textContent = moneda(subtotal);
            fila.querySelector('.quitar-linea').disabled = filas.length === 1;
            total += subtotal;
        });
        document.querySelector('#total-compra').textContent = moneda(total);
        agregar.disabled = filas.length >= 50;
    }
    agregar.addEventListener('click', () => {
        if (lista.children.length >= 50) return;
        const nueva = lista.firstElementChild.cloneNode(true);
        nueva.querySelectorAll('input, select').forEach(campo => { campo.value = ''; });
        lista.append(nueva); actualizar(); nueva.querySelector('select').focus();
    });
    lista.addEventListener('click', e => {
        if (e.target.closest('.quitar-linea') && lista.children.length > 1) { e.target.closest('.linea-compra').remove(); actualizar(); }
    });
    lista.addEventListener('input', actualizar);
    lista.addEventListener('change', actualizar);
    form.addEventListener('submit', e => {
        if (!window.confirm(`Registrar compra por ${document.querySelector('#total-compra').textContent} e ingresar esta mercaderia al stock?`)) { e.preventDefault(); return; }
        confirmar.disabled = true;
    });
    window.addEventListener('pageshow', () => { confirmar.disabled = false; });
    actualizar();
})();
