// Piezas de interfaz comunes. Todo el texto que llega de la API se pone con textContent (nunca innerHTML).

/** Crea un elemento: crear('p', { clase: 'meta', texto: 'Hola', atributos: { id: 'x' } }, hijos). */
export function crear(etiqueta, { clase, texto, atributos } = {}, hijos = []) {
  const elemento = document.createElement(etiqueta);
  if (clase) elemento.className = clase;
  if (texto !== undefined && texto !== null) elemento.textContent = texto;
  for (const [nombre, valor] of Object.entries(atributos ?? {})) {
    if (valor !== undefined && valor !== null && valor !== false) elemento.setAttribute(nombre, valor === true ? '' : valor);
  }
  for (const hijo of hijos) {
    if (hijo) elemento.append(hijo);
  }
  return elemento;
}

/** Slime dibujado con CSS: en su color (con cara) si está descubierto; si no, silueta. */
export function slime({ color, descubierto, tamano = '', cara = true, interrogacion = true }) {
  const clase = ['slime', tamano && `slime--${tamano}`, !descubierto && 'slime--oculto'].filter(Boolean).join(' ');
  const elemento = crear('span', { clase, atributos: { 'aria-hidden': 'true' } });
  if (descubierto) {
    elemento.style.setProperty('--color', color);
    if (cara) {
      elemento.append(crear('i', { clase: 'slime__ojo slime__ojo--i' }), crear('i', { clase: 'slime__ojo slime__ojo--d' }),
        crear('i', { clase: 'slime__boca' }));
    }
  } else if (interrogacion) {
    elemento.append(crear('span', { clase: 'slime__interrogacion', texto: '?' }));
  }
  return elemento;
}

/** Marca con forma de plort (la firma del diseño). */
export function plort(hecho, tamano = '') {
  const clase = ['plort', tamano && `plort--${tamano}`, hecho && 'plort--hecho'].filter(Boolean).join(' ');
  return crear('i', { clase, atributos: { 'aria-hidden': 'true' } });
}

/** Sustituye el contenido de un contenedor por un texto de estado (cargando, vacío, error). */
export function estado(contenedor, texto, etiqueta = 'p') {
  contenedor.replaceChildren(crear(etiqueta, { clase: 'estado', texto }));
}

/** Mensaje para el usuario de cualquier error (los de la API traen el del servidor). */
export function mensajeDe(error) {
  return error?.mensaje ?? 'Ha ocurrido un error inesperado. Recarga la página.';
}

/** Alerta de error (rosa) en una zona aria-live. */
export function alerta(contenedor, mensaje) {
  contenedor.replaceChildren(crear('p', { clase: 'alerta', texto: mensaje, atributos: { role: 'alert' } }));
}

/** Desactiva el botón mientras dura la petición. */
export async function conBoton(boton, accion) {
  boton.disabled = true;
  boton.setAttribute('aria-busy', 'true');
  try {
    return await accion();
  } finally {
    boton.disabled = false;
    boton.removeAttribute('aria-busy');
  }
}

/** Quita los errores de campo de un formulario. */
export function limpiarErrores(raiz) {
  raiz.querySelectorAll('.error-campo[data-error]').forEach((error) => error.remove());
  raiz.querySelectorAll('[aria-invalid]').forEach((campo) => {
    campo.removeAttribute('aria-invalid');
    campo.removeAttribute('aria-describedby');
  });
}

/**
 * Pone cada error debajo de su campo (los campos se buscan por name).
 * Devuelve los mensajes que no tienen campo, para mostrarlos como alerta.
 */
export function mostrarErrores(raiz, campos) {
  const sueltos = [];
  for (const [nombre, mensaje] of Object.entries(campos ?? {})) {
    const campo = raiz.querySelector(`[name="${CSS.escape(nombre)}"]`);
    if (!campo) {
      sueltos.push(mensaje);
      continue;
    }
    const id = `error-${nombre}`;
    const error = crear('span', { clase: 'error-campo', texto: mensaje, atributos: { id, 'data-error': true } });
    (campo.closest('[data-campo]') ?? campo).after(error);
    campo.setAttribute('aria-invalid', 'true');
    campo.setAttribute('aria-describedby', id);
  }
  const primero = raiz.querySelector('[aria-invalid="true"]');
  if (primero) primero.focus();
  return sueltos;
}

/** Icono de cerrar (trazo, 16 px), para el aviso de las zonas. */
export function iconoCerrar() {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('aria-hidden', 'true');
  svg.setAttribute('width', '16');
  svg.setAttribute('height', '16');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '1.75');
  svg.setAttribute('stroke-linecap', 'round');
  const trazo = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  trazo.setAttribute('d', 'M18 6 6 18M6 6l12 12');
  svg.append(trazo);
  return svg;
}

/** Textos de la interfaz para los valores de la API. */
export const NOMBRE_DIETA = { CARNE: 'Carne', FRUTA: 'Fruta', VERDURA: 'Verdura', OMNIVORO: 'Omnívoro', ESPECIAL: 'Especial' };
export const NOMBRE_RAREZA = { COMUN: 'Común', RARO: 'Raro', ESPECIAL: 'Especial' };
