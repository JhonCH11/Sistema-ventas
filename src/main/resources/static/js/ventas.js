let ocupado = false;
const money = value => 'S/ ' + value.toFixed(2);
function filtrar() {
 const buscar = document.querySelector('#buscar').value.toLocaleLowerCase();
 const categoria = document.querySelector('#categoria').value;
 let visibles = 0;
 document.querySelectorAll('.producto-venta').forEach(p => {
  p.hidden = !p.dataset.nombre.toLocaleLowerCase().includes(buscar) || (categoria && p.dataset.categoria !== categoria);
  if (!p.hidden) visibles++;
 });
 document.querySelector('#sin-resultados').hidden = visibles > 0;
}
function pago() {
 const efectivo = document.querySelector('#medioPago').value === 'EFECTIVO';
 document.querySelector('#pago-efectivo').hidden = !efectivo;
 document.querySelector('#pago-digital').hidden = efectivo;
 const input = document.querySelector('#importeRecibido');
 input.required = efectivo;
 const verificado = document.querySelector('[name="abonoVerificado"]');
 verificado.required = !efectivo;
 const total = Number(document.querySelector('[data-total]').dataset.total);
 const recibido = Number(input.value);
 document.querySelector('#vuelto').textContent = input.value ? (recibido >= total ? 'Vuelto: ' + money(recibido - total) : 'Falta: ' + money(total - recibido)) : '';
 input.min = total;
}
document.addEventListener('input', e => {
 if (e.target.id === 'buscar') filtrar();
 if (e.target.id === 'importeRecibido') pago();
 const producto = e.target.closest('.producto-venta');
 if (producto && e.target.name === 'cantidad') producto.querySelector('output').textContent = 'Subtotal: ' + money(Math.round((Number(producto.dataset.precio) * Number(e.target.value) + Number.EPSILON) * 100) / 100);
});
document.addEventListener('change', e => {
 if(e.target.id === 'categoria') filtrar();
 if(e.target.id === 'medioPago') pago();
});
document.addEventListener('click', e => {
 if(e.target.id === 'abrir-cobro') { document.querySelector('#cobro').showModal(); pago(); }
 if(e.target.id === 'volver-venta') document.querySelector('#cobro').close();
});
document.addEventListener('submit', async e => {
 const form = e.target;
 if(!form.matches('[data-ajax], #form-cobro')) return;
 e.preventDefault();
 if(ocupado) return;
 if(form.hasAttribute('data-cancelar') && !confirm('¿Cancelar todos los productos de esta venta?')) return;
 const buscar = document.querySelector('#buscar').value;
 const categoria = document.querySelector('#categoria').value;
 ocupado = true;
 const button = e.submitter;
 if(button) button.disabled = true;
 try {
  const response = await fetch(form.action, {method:'POST', body:new FormData(form)});
  if(!response.ok) throw new Error('No se pudo guardar. Revisa la conexión local y vuelve a intentarlo.');
  if(new URL(response.url).pathname !== '/ventas') { location.href = response.url; return; }
  const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
  const main = doc.querySelector('#punto-venta');
  if(!main) { location.href = response.url; return; }
  document.querySelector('#punto-venta').replaceWith(main);
  document.querySelector('#buscar').value = buscar;
  document.querySelector('#categoria').value = categoria;
  filtrar();
 } catch(error) { alert(error.message); if(button) button.disabled=false; }
 finally { ocupado=false; }
});
document.querySelectorAll('#categoria option').forEach((o,i,all)=>{if(i && [...all].slice(0,i).some(p=>p.value===o.value))o.remove();});

