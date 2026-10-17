(() => {
    const boton = document.querySelector('#imprimir');
    const formato = document.querySelector('#formato-impresion');
    const ticket = document.querySelector('.ticket-contenido');
    const aviso = document.querySelector('#aviso-impresion');
    if (!boton || !formato || !ticket || !aviso) return;
    const aplicar = () => {
        ticket.classList.toggle('formato-ticket', formato.value === 'ticket');
        ticket.classList.toggle('formato-a4', formato.value === 'a4');
    };
    formato.addEventListener('change', aplicar);
    window.addEventListener('beforeprint', aplicar);
    boton.addEventListener('click', () => {
        aplicar(); aviso.hidden = false;
        aviso.textContent = 'Seleccione la impresora y el papel en el dialogo. Cancelar o reimprimir no modifica la venta.';
        try { window.print(); }
        catch (_) { aviso.textContent = 'No se pudo abrir la impresion. La venta sigue guardada; puede volver a intentarlo.'; }
    });
})();
