(() => {
    const panel = document.querySelector('.horario-jornada');
    if (!panel) return;
    const reloj = panel.querySelector('.reloj-jornada');
    const fecha = panel.querySelector('.fecha-jornada');
    const previsto = panel.querySelector('.horario-previsto');
    const aviso = panel.querySelector('.aviso-horario');
    const extender = panel.querySelector('.horario-extender');
    const opciones = { timeZone: 'America/Lima' };
    const horaFormato = new Intl.DateTimeFormat('es-PE', { ...opciones, hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' });
    const fechaFormato = new Intl.DateTimeFormat('es-PE', { ...opciones, weekday: 'long', day: 'numeric', month: 'long' });
    const cierreFormato = new Intl.DateTimeFormat('es-PE', { ...opciones, hour: 'numeric', minute: '2-digit', hour12: true });
    let datos, referencia, pendiente = false, fallo = false;
    function actualizar() {
        if (!datos) return;
        const ahora = datos.ahora + performance.now() - referencia;
        reloj.textContent = horaFormato.format(ahora);
        fecha.textContent = fechaFormato.format(ahora);
        previsto.textContent = 'Horario diario: 6:00 a. m. a ' + cierreFormato.format(datos.cierre);
        const mismoDia = new Intl.DateTimeFormat('en-CA', { ...opciones, year: 'numeric', month: '2-digit', day: '2-digit' }).format(ahora) === datos.fecha;
        extender.hidden = fallo || !mismoDia || !datos.administrador || datos.estado !== 'ABIERTA';
        let mensaje = '';
        if (fallo || !mismoDia) mensaje = 'No se pudo actualizar el estado de caja. Revise la conexion local o vuelva a ingresar.';
        else if (datos.estado === 'CERRADA') mensaje = 'Caja cerrada por hoy.';
        else if (ahora < datos.apertura) mensaje = 'La apertura habitual es a las 6:00 a. m.';
        else if (datos.estado === 'SIN_ABRIR') mensaje = 'La caja de hoy aun no esta abierta. Ingrese a Caja para abrirla.';
        else if (ahora >= datos.cierre) mensaje = datos.administrador
            ? 'Se alcanzo la hora de cierre. Finalice las ventas y cierre la caja, o autorice una extension.'
            : 'Se alcanzo la hora de cierre. Consulte al administrador si necesita extender el horario.';
        else if (datos.cierre - ahora <= 30 * 60000) mensaje = 'Faltan ' + Math.ceil((datos.cierre - ahora) / 60000) + ' minutos para el cierre previsto.';
        if (aviso.textContent !== mensaje) aviso.textContent = mensaje;
        aviso.hidden = !mensaje;
    }
    async function sincronizar() {
        if (pendiente) return;
        pendiente = true;
        const control = new AbortController();
        const timeout = setTimeout(() => control.abort(), 5000);
        try {
            const respuesta = await fetch(panel.dataset.horarioUrl, { credentials: 'same-origin', cache: 'no-store', signal: control.signal });
            if (!respuesta.ok) throw new Error('Horario no disponible');
            const nuevo = await respuesta.json();
            if (![nuevo.ahora, nuevo.apertura, nuevo.cierre].every(Number.isFinite)) throw new Error('Horario invalido');
            datos = nuevo;
            referencia = performance.now();
            fallo = false;
        } catch (_) {
            fallo = true;
            extender.hidden = true;
            aviso.hidden = false;
            aviso.textContent = 'No se pudo actualizar el horario. Revise la conexion local o vuelva a ingresar.';
        } finally {
            clearTimeout(timeout);
            pendiente = false;
            actualizar();
        }
    }
    sincronizar();
    setInterval(actualizar, 1000);
    setInterval(sincronizar, 60000);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) sincronizar(); });
})();
