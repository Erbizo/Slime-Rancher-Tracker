// Slimepedia: cuadrícula de fichas con filtros y panel de detalle del slime seleccionado.

import { api } from '../api.js';
import { iniciarCabecera } from '../cabecera.js';
import {
  NOMBRE_DIETA, NOMBRE_RAREZA, alerta, conBoton, crear, estado, limpiarErrores, mensajeDe, mostrarErrores, slime,
} from '../ui.js';
import { conArticulo } from '../fechas.js';

const $ = (selector) => document.querySelector(selector);
const cuadricula = $('[data-cuadricula]');
const detalle = $('[data-detalle]');
const selectorZona = $('[data-filtro-zona]');

let partidaId;
let slimes = [];
let filtroEstado = 'todos';
let seleccionado = null;

function visibles() {
  const zona = selectorZona.value;
  return slimes.filter((s) => (!zona || String(s.zonaId) === zona)
    && (filtroEstado === 'todos' || (filtroEstado === 'descubiertos') === s.descubierto));
}

function ficha(s) {
  const yaPuedes = !s.descubierto && s.zonaDesbloqueada;
  const boton = crear('button', {
    clase: s.zonaDesbloqueada ? 'ficha' : 'ficha ficha--lejana',
    atributos: { type: 'button', 'aria-pressed': String(s.id === seleccionado), 'data-slime': s.id },
  }, [
    slime({ color: s.color, descubierto: s.descubierto }),
    crear('span', { clase: 'ficha__nombre', texto: s.descubierto ? s.nombre : '???' }),
    crear('span', { clase: 'meta', texto: yaPuedes ? `${s.zonaNombre}, ya puedes` : s.zonaNombre }),
  ]);
  return boton;
}

function pintarCuadricula() {
  const lista = visibles();
  if (lista.length === 0) {
    estado(cuadricula, 'No hay slimes con estos filtros.');
    return;
  }
  cuadricula.replaceChildren(...lista.map(ficha));
}

function accionesDescubierto(s) {
  const formulario = crear('form', { clase: 'detalle__acciones', atributos: { novalidate: true } });
  const nota = crear('textarea', { clase: 'campo campo--area', atributos: { name: 'nota', maxlength: '200', rows: '3' } });
  nota.value = s.nota ?? '';
  const guardar = crear('button', { clase: 'btn btn--oscuro', texto: 'Guardar nota', atributos: { type: 'submit' } });
  const quitar = crear('button', { clase: 'btn btn--texto-peligro', texto: 'Quitar de la Slimepedia', atributos: { type: 'button' } });
  formulario.append(crear('label', { clase: 'etiqueta' }, [crear('span', { texto: 'Nota' }), nota]), guardar, quitar);

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario);
    await conBoton(guardar, () => ejecutar(formulario,
      () => api(`/partidas/${partidaId}/slimes/${s.id}/nota`, { metodo: 'PUT', cuerpo: { nota: nota.value } })));
  });
  quitar.addEventListener('click', () => conBoton(quitar, () => ejecutar(formulario,
    () => api(`/partidas/${partidaId}/slimes/${s.id}`, { metodo: 'DELETE' }))));
  return formulario;
}

function accionDescubrir(s) {
  const boton = crear('button', { clase: 'btn btn--primario btn--alto', texto: 'Marcar como descubierto', atributos: { type: 'button' } });
  boton.addEventListener('click', () => conBoton(boton, () => ejecutar(detalle,
    () => api(`/partidas/${partidaId}/slimes/${s.id}`, { metodo: 'POST' }))));
  return boton;
}

function avisoZonaBloqueada(s) {
  return crear('p', {}, [
    document.createTextNode(`Primero tienes que desbloquear ${s.zonaNombre}. `),
    crear('a', { clase: 'enlace-fuerte', texto: 'Ir a Zonas', atributos: { href: 'zonas.html' } }),
  ]);
}

function pintarDetalle(enfocar = false) {
  const s = slimes.find((x) => x.id === seleccionado);
  if (!s) {
    detalle.hidden = true;
    return;
  }
  const titulo = crear('h2', { clase: 'detalle__nombre', texto: s.descubierto ? s.nombre : '???', atributos: { tabindex: '-1' } });
  let accion;
  if (s.descubierto) accion = accionesDescubierto(s);
  else if (s.zonaDesbloqueada) accion = accionDescubrir(s);
  else accion = avisoZonaBloqueada(s);

  detalle.replaceChildren(
    crear('div', { clase: 'detalle__cabeza' }, [
      slime({ color: s.color, descubierto: s.descubierto, tamano: 'grande' }),
      crear('div', { clase: 'detalle__titulo' }, [
        titulo,
        crear('p', { clase: 'subtitulo', texto: s.descubierto ? `Descubierto ${conArticulo(s.descubiertoEn)}` : 'Sin descubrir' }),
      ]),
    ]),
    crear('dl', { clase: 'datos' }, [
      crear('dt', { texto: 'Zona' }), crear('dd', { texto: s.zonaNombre }),
      crear('dt', { texto: 'Dieta' }), crear('dd', { texto: s.descubierto ? NOMBRE_DIETA[s.dieta] : '?' }),
      crear('dt', { texto: 'Rareza' }), crear('dd', { texto: s.descubierto ? NOMBRE_RAREZA[s.rareza] : '?' }),
    ]),
    crear('div', { clase: 'avisos', atributos: { 'aria-live': 'polite', 'data-aviso-detalle': true } }),
    accion,
  );
  detalle.hidden = false;
  if (enfocar) titulo.focus();
}

/** Ejecuta una acción del detalle, recarga y muestra el error junto a su campo o como alerta. */
async function ejecutar(raiz, peticion) {
  try {
    await peticion();
    await cargar();
    pintarDetalle(true);
  } catch (error) {
    const sueltos = mostrarErrores(raiz, error.campos);
    if (sueltos.length > 0 || Object.keys(error.campos ?? {}).length === 0) {
      alerta(detalle.querySelector('[data-aviso-detalle]'), mensajeDe(error));
    }
  }
}

async function cargar() {
  slimes = await api(`/partidas/${partidaId}/slimes`);
  const descubiertos = slimes.filter((s) => s.descubierto).length;
  $('[data-subtitulo]').textContent = `${descubiertos} de ${slimes.length} slimes descubiertos`;
  pintarCuadricula();
}

function seleccionar(id) {
  seleccionado = id;
  cuadricula.querySelectorAll('.ficha').forEach((f) => f.setAttribute('aria-pressed', String(Number(f.dataset.slime) === id)));
  pintarDetalle();
  if (detalle.getBoundingClientRect().top > window.innerHeight) {
    detalle.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
}

cuadricula.addEventListener('click', (evento) => {
  const pulsada = evento.target.closest('.ficha');
  if (pulsada) seleccionar(Number(pulsada.dataset.slime));
});

selectorZona.addEventListener('change', pintarCuadricula);

document.querySelectorAll('[data-estado]').forEach((boton) => boton.addEventListener('click', () => {
  filtroEstado = boton.dataset.estado;
  document.querySelectorAll('[data-estado]').forEach((b) => b.setAttribute('aria-pressed', String(b === boton)));
  pintarCuadricula();
}));

async function iniciar() {
  try {
    partidaId = (await iniciarCabecera()).id;
    const zonas = await api(`/partidas/${partidaId}/zonas`);
    selectorZona.append(...zonas.filter((z) => z.slimesTotal > 0)
      .map((z) => crear('option', { texto: z.nombre, atributos: { value: z.id } })));
    await cargar();
    const pedido = Number(new URLSearchParams(window.location.search).get('slime'));
    const inicial = slimes.find((s) => s.id === pedido)
      ?? slimes.find((s) => !s.descubierto && s.zonaDesbloqueada)
      ?? slimes[0];
    if (inicial) seleccionar(inicial.id);
  } catch (error) {
    alerta($('[data-aviso]'), mensajeDe(error));
    $('[data-subtitulo]').textContent = '';
    estado(cuadricula, 'No se han podido cargar los slimes.');
  }
}

iniciar();
