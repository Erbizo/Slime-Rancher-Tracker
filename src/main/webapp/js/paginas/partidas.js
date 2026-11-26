// Mis partidas: crear, renombrar, borrar (con confirmación en línea) y elegir con cuál trabajar.

import { api } from '../api.js';
import { conectarSalir } from '../cabecera.js';
import { leerPartidaId, olvidarPartida } from '../estado.js';
import { alerta, conBoton, crear, estado, limpiarErrores, mensajeDe, mostrarErrores } from '../ui.js';
import { actualizada } from '../fechas.js';

const $ = (selector) => document.querySelector(selector);
const aviso = $('[data-aviso]');
const lista = $('[data-lista]');
const nueva = $('[data-nueva]');

/** Errores 400/409 de un formulario de nombre: debajo del campo; el resto, como alerta. */
function mostrarErrorNombre(formulario, error) {
  const campos = error.status === 409 ? { nombre: error.mensaje } : error.campos;
  if (mostrarErrores(formulario, campos).length > 0 || Object.keys(campos ?? {}).length === 0) {
    alerta(aviso, mensajeDe(error));
  }
}

function botonPequeno(clase, texto, tipo = 'button') {
  return crear('button', { clase: `btn ${clase} btn--pequeno`, texto, atributos: { type: tipo } });
}

function filaDatos(partida, li) {
  const renombrar = botonPequeno('btn--secundario', 'Renombrar');
  const borrar = botonPequeno('btn--texto-peligro', 'Borrar');
  renombrar.addEventListener('click', () => editarNombre(partida, li));
  borrar.addEventListener('click', () => confirmarBorrado(partida, li, borrar));
  return crear('div', { clase: 'partida__fila' }, [
    crear('div', { clase: 'partida__texto' }, [
      crear('a', { clase: 'partida__nombre', texto: partida.nombre, atributos: { href: `index.html?partida=${partida.id}` } }),
      crear('p', { clase: 'meta partida__fecha', texto: `Actualizada ${actualizada(partida.actualizadaEn)}` }),
    ]),
    crear('span', { clase: 'partida__porcentaje', texto: `${partida.porcentaje} %` }),
    crear('div', { clase: 'partida__acciones' }, [renombrar, borrar]),
  ]);
}

function editarNombre(partida, li) {
  const campo = crear('input', {
    clase: 'campo', atributos: { type: 'text', name: 'nombre', maxlength: '40', autocomplete: 'off', value: partida.nombre },
  });
  const guardar = botonPequeno('btn--primario', 'Guardar', 'submit');
  const cancelar = botonPequeno('btn--secundario', 'Cancelar');
  const formulario = crear('form', { clase: 'nueva-partida', atributos: { novalidate: true } }, [
    crear('div', { clase: 'nueva-partida__fila', atributos: { 'data-campo': true } }, [
      crear('label', { clase: 'nueva-partida__etiqueta' }, [crear('span', { clase: 'solo-lector', texto: 'Nuevo nombre de la partida' }), campo]),
      guardar, cancelar,
    ]),
  ]);
  const volver = () => {
    li.replaceChildren(filaDatos(partida, li));
    li.querySelector('.btn--secundario')?.focus();
  };
  cancelar.addEventListener('click', volver);
  formulario.addEventListener('keydown', (evento) => { if (evento.key === 'Escape') volver(); });
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario);
    await conBoton(guardar, async () => {
      try {
        await api(`/partidas/${partida.id}`, { metodo: 'PUT', cuerpo: { nombre: campo.value } });
        aviso.replaceChildren();
        await cargar();
      } catch (error) {
        mostrarErrorNombre(formulario, error);
      }
    });
  });
  li.replaceChildren(formulario);
  campo.focus();
  campo.select();
}

function confirmarBorrado(partida, li, botonBorrar) {
  if (li.querySelector('.confirmacion')) return;
  const cancelar = botonPequeno('btn--secundario', 'Cancelar');
  const confirmar = botonPequeno('btn--peligro', 'Borrar partida');
  const dialogo = crear('div', { clase: 'confirmacion', atributos: { role: 'alertdialog', 'aria-label': `Confirmar borrado de ${partida.nombre}` } }, [
    crear('span', { texto: 'Se borrarán sus zonas, slimes y logros. No se puede deshacer.' }),
    crear('span', { clase: 'confirmacion__botones' }, [cancelar, confirmar]),
  ]);
  const cerrar = () => {
    dialogo.remove();
    botonBorrar.focus();
  };
  cancelar.addEventListener('click', cerrar);
  dialogo.addEventListener('keydown', (evento) => { if (evento.key === 'Escape') cerrar(); });
  confirmar.addEventListener('click', () => conBoton(confirmar, async () => {
    try {
      await api(`/partidas/${partida.id}`, { metodo: 'DELETE' });
      if (leerPartidaId() === partida.id) olvidarPartida();
      aviso.replaceChildren();
      await cargar();
      $('[name="nombre"]').focus();
    } catch (error) {
      alerta(aviso, mensajeDe(error));
    }
  }));
  li.append(dialogo);
  cancelar.focus();
}

async function cargar() {
  const partidas = await api('/partidas');
  if (partidas.length === 0) {
    estado(lista, 'Todavía no tienes partidas. Crea la primera con el campo de arriba.', 'li');
    return;
  }
  lista.replaceChildren(...partidas.map((partida) => {
    const li = crear('li');
    li.append(filaDatos(partida, li));
    return li;
  }));
}

nueva.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  limpiarErrores(nueva);
  const campo = nueva.querySelector('[name="nombre"]');
  await conBoton(nueva.querySelector('[type="submit"]'), async () => {
    try {
      await api('/partidas', { metodo: 'POST', cuerpo: { nombre: campo.value } });
      campo.value = '';
      aviso.replaceChildren();
      await cargar();
    } catch (error) {
      mostrarErrorNombre(nueva, error);
    }
  });
});

async function iniciar() {
  conectarSalir();
  try {
    const usuario = await api('/auth/yo');
    $('[data-usuario]').textContent = usuario.nombreUsuario;
    await cargar();
  } catch (error) {
    alerta(aviso, mensajeDe(error));
    estado(lista, 'No se han podido cargar las partidas.', 'li');
  }
}

iniciar();
