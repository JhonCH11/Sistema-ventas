(() => {
    const cierre = document.getElementById('cierre-caja');
    const formulario = document.getElementById('formulario-cierre');
    if (!cierre || !formulario) return;

    const cancelar = document.getElementById('cancelar-cierre');
    cancelar.hidden = false;
    cancelar.addEventListener('click', () => {
        cierre.open = false;
        cierre.querySelector('summary').focus();
    });

    formulario.addEventListener('submit', evento => {
        const contado = document.getElementById('efectivoContado').value;
        if (!window.confirm(`Efectivo contado: S/ ${contado}.\n\nLas ventas quedaran bloqueadas hasta una reapertura autorizada por el administrador.\n\nDesea confirmar el cierre?`)) {
            evento.preventDefault();
        }
    });
})();
