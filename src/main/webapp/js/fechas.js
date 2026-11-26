// Fechas en español relativas a hoy: "hoy", "ayer", "3 nov".

const diaMes = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });
const horaMinuto = new Intl.DateTimeFormat('es-ES', { hour: '2-digit', minute: '2-digit' });

function diasDesde(fecha) {
  const hoy = new Date();
  const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  const inicioFecha = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  return Math.round((inicioHoy - inicioFecha) / 86400000);
}

function sinPunto(texto) {
  return texto.replace('.', '');
}

/** "hoy", "ayer" o "3 nov". */
export function dia(iso) {
  const fecha = new Date(iso);
  const dias = diasDesde(fecha);
  if (dias === 0) return 'hoy';
  if (dias === 1) return 'ayer';
  return sinPunto(diaMes.format(fecha));
}

/** Para "Desbloqueada el 3 nov" / "Desbloqueada hoy". */
export function conArticulo(iso) {
  const texto = dia(iso);
  return texto === 'hoy' || texto === 'ayer' ? texto : 'el ' + texto;
}

/** "hoy 18:42", "ayer 22:10", "3 nov". */
export function diaYHora(iso) {
  const fecha = new Date(iso);
  const texto = dia(iso);
  return texto === 'hoy' || texto === 'ayer' ? `${texto} ${horaMinuto.format(fecha)}` : texto;
}

/** "hoy a las 18:42", "ayer a las 22:10", "el 15 nov". */
export function actualizada(iso) {
  const fecha = new Date(iso);
  const texto = dia(iso);
  return texto === 'hoy' || texto === 'ayer' ? `${texto} a las ${horaMinuto.format(fecha)}` : 'el ' + texto;
}
