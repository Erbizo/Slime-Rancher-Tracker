// Logros de la partida por categoría: marcar/desmarcar con el check en forma de plort y editar la nota.

import { api } from '../api.js';
import { iniciarCabecera } from '../cabecera.js';
import { alerta, conBoton, crear, estado, limpiarErrores, mensajeDe, mostrarErrores, plort } from '../ui.js';
import { dia } from '../fechas.js';

const CATEGORIAS = [
  ['EXPLORACION', 'Exploración'], ['SLIMES', 'Slimes'], ['RANCHO', 'Rancho'], ['SECRETOS', 'Secretos'],
];

const $ = (selector) => document.querySelector(selector);
const aviso = $('[data-aviso]');
const grupos = $('[data-grupos]');
let partidaId;

function check(logro) {
  return crear('button', {
    clase: 'check-plort',
    atributos: {
      type: 'button',
      role: 'checkbox',
      'aria-checked': String(logro.conseguido),
      'aria-label': `${logro.conseguido ? 'Desmarcar' : 'Marcar'} ${logro.nombre}`,
      'data-check': logro.id,
    },
  }, [plort(logro.conseguido, 'grande')]);
}

function editorNota(logro, contenedor) {
  const area = crear('textarea', {
    clase: 'campo campo--area',
    atributos: { name: 'nota', maxlength: '200', rows: '3', 'aria-label': `Nota de ${logro.nombre}` },
  });
  area.value = logro.nota ?? '';
  const guardar = crear('button', { clase: 'btn btn--oscuro btn--pequeno', texto: 'Guardar nota', atributos: { type: 'submit' } });
  const cancelar = crear('button', { clase: 'btn btn--secundario btn--pequeno', texto: 'Cancelar', atributos: { type: 'button' } });
  const formulario = crear('form', { clase: 'editor-nota', atributos: { novalidate: true } }, [
    area, crear('div', { clase: 'editor-nota__botones' }, [guardar, cancelar]),
  ]);
  const cerrar = () => {
    contenedor.replaceWith(pieNota(logro));
    document.querySelector(`[data-editar="${logro.id}"]`)?.focus();
  };
  cancelar.addEventListener('click', cerrar);
  formulario.addEventListener('keydown', (evento) => { if (evento.key === 'Escape') cerrar(); });
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario);
    await conBoton(guardar, async () => {
      try {
        await api(`/partidas/${partidaId}/logros/${logro.id}/nota`, { metodo: 'PUT', cuerpo: { nota: area.value } });
        await cargar(`[data-editar="${logro.id}"]`);
      } catch (error) {
        if (mostrarErrores(formulario, error.campos).length > 0 || !error.campos?.nota) alerta(aviso, mensajeDe(error));
      }
    });
  });
  contenedor.replaceChildren(formulario);
  area.focus();
}

/** Nota (si hay) y el botón para añadirla o editarla. Solo en logros conseguidos. */
function pieNota(logro) {
  const pie = crear('div', { atributos: { 'data-pie': logro.id } });
  if (logro.nota) pie.append(crear('p', { clase: 'nota', texto: logro.nota }));
  if (logro.conseguido) {
    const editar = crear('button', {
      clase: 'enlace-accion',
      texto: logro.nota ? 'Editar nota' : 'Añadir nota',
      atributos: { type: 'button', 'data-editar': logro.id },
    });
    editar.addEventListener('click', () => editorNota(logro, pie));
    pie.append(editar);
  }
  return pie;
}

function filaLogro(logro) {
  return crear('li', { clase: 'logro' }, [
    check(logro),
    crear('div', { clase: 'logro__texto' }, [
      crear('div', { clase: 'logro__cabeza' }, [
        crear('span', { texto: logro.nombre }),
        crear('span', { clase: 'meta', texto: logro.conseguido ? dia(logro.conseguidoEn) : '' }),
      ]),
      crear('p', { clase: 'subtitulo logro__descripcion', texto: logro.descripcion }),
      pieNota(logro),
    ]),
  ]);
}

function grupo([clave, titulo], logros) {
  const delGrupo = logros.filter((logro) => logro.categoria === clave);
  const conseguidos = delGrupo.filter((logro) => logro.conseguido).length;
  const id = `titulo-${clave.toLowerCase()}`;
  return crear('section', { clase: 'grupo', atributos: { 'aria-labelledby': id } }, [
    crear('div', { clase: 'grupo__cabeza' }, [
      crear('h2', { clase: 'titulo-seccion', texto: titulo, atributos: { id } }),
      crear('span', { texto: `${conseguidos} de ${delGrupo.length}` }),
    ]),
    crear('ul', { clase: 'tarjeta grupo-logros' }, delGrupo.map(filaLogro)),
  ]);
}

async function cargar(enfocar) {
  const logros = await api(`/partidas/${partidaId}/logros`);
  const conseguidos = logros.filter((logro) => logro.conseguido).length;
  $('[data-subtitulo]').textContent = `${conseguidos} de ${logros.length} logros conseguidos`;
  grupos.replaceChildren(...CATEGORIAS.map((categoria) => grupo(categoria, logros)));
  if (enfocar) grupos.querySelector(enfocar)?.focus();
}

grupos.addEventListener('click', async (evento) => {
  const pulsado = evento.target.closest('[data-check]');
  if (!pulsado) return;
  const id = pulsado.dataset.check;
  const conseguido = pulsado.getAttribute('aria-checked') === 'true';
  await conBoton(pulsado, async () => {
    try {
      await api(`/partidas/${partidaId}/logros/${id}`, { metodo: conseguido ? 'DELETE' : 'POST' });
      aviso.replaceChildren();
      await cargar(`[data-check="${id}"]`);
    } catch (error) {
      alerta(aviso, mensajeDe(error));
    }
  });
});

async function iniciar() {
  try {
    partidaId = (await iniciarCabecera()).id;
    await cargar();
  } catch (error) {
    alerta(aviso, mensajeDe(error));
    $('[data-subtitulo]').textContent = '';
    estado(grupos, 'No se han podido cargar los logros.');
  }
}

iniciar();
