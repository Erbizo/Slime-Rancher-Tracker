// Panel de progreso de la partida seleccionada.

import { api } from '../api.js';
import { iniciarCabecera } from '../cabecera.js';
import { alerta, crear, estado, mensajeDe, plort, slime } from '../ui.js';
import { actualizada, diaYHora } from '../fechas.js';

const CAMBIOS_VISIBLES = 5;
const TEXTO_ACTIVIDAD = { ZONA: 'Zona desbloqueada: ', SLIME: 'Slime descubierto: ', LOGRO: 'Logro: ' };

const $ = (selector) => document.querySelector(selector);

function pintarResumen(clave, conteo) {
  const pips = [];
  for (let i = 0; i < conteo.total; i++) {
    pips.push(plort(i < conteo.conseguidos));
  }
  $(`[data-pips="${clave}"]`).replaceChildren(...pips);
  $(`[data-cuenta="${clave}"]`).textContent = `${conteo.conseguidos} de ${conteo.total}`;
}

function filaZona(zona) {
  const nombre = crear('span', {
    clase: zona.desbloqueada ? 'fila-zona__nombre' : 'fila-zona__nombre fila-zona__nombre--bloqueada',
    texto: zona.zonaNombre,
  });
  const slimes = crear('span', { clase: 'fila-zona__slimes' });
  for (const mini of zona.slimes) {
    const dibujo = slime({ color: mini.color, descubierto: mini.descubierto, tamano: 'mini', cara: false, interrogacion: false });
    if (zona.desbloqueada) dibujo.title = mini.descubierto ? mini.nombre : 'Sin descubrir';
    slimes.append(dibujo);
  }
  if (!zona.desbloqueada) {
    slimes.append(crear('span', { clase: 'meta fila-zona__aviso', texto: 'zona bloqueada' }));
  }
  return crear('div', { clase: 'fila-zona' }, [nombre, slimes, crear('span', { texto: `${zona.descubiertos}/${zona.total}` })]);
}

function pasos(siguientes) {
  const lista = [
    ...siguientes.zonas.map((paso) => ({
      titulo: `Desbloquear ${paso.zonaNombre}`,
      detalle: paso.requisitoNombre ? `${paso.requisitoNombre} ya está abierta` : 'No necesita ninguna zona antes',
      enlace: 'zonas.html',
    })),
    ...siguientes.slimes.map((paso) => ({
      titulo: `Buscar al ${paso.slimeNombre}`,
      detalle: paso.zonaNombre,
      enlace: `slimepedia.html?slime=${paso.slimeId}`,
    })),
  ];
  if (lista.length === 0) {
    return [crear('p', { clase: 'estado', texto: 'No hay nada pendiente: todo lo que tienes a mano ya está registrado.' })];
  }
  return lista.map((paso, indice) => {
    const principal = indice === 0;
    return crear('a', { clase: principal ? 'accion accion--principal' : 'accion', atributos: { href: paso.enlace } }, [
      crear('span', { clase: 'accion__titulo', texto: paso.titulo }),
      crear('span', { clase: principal ? 'accion__detalle' : 'meta', texto: paso.detalle }),
    ]);
  });
}

function cambio(actividad) {
  return crear('li', { clase: 'cambio' }, [
    plort(true, 'mini'),
    crear('span', { texto: TEXTO_ACTIVIDAD[actividad.tipo] + actividad.nombre }),
    crear('span', { clase: 'meta', texto: diaYHora(actividad.fecha) }),
  ]);
}

function pintar(progreso) {
  $('[data-actualizada]').textContent = `${progreso.partidaNombre}, actualizada ${actualizada(progreso.actualizadaEn)}`;
  $('[data-cifra]').textContent = `${progreso.porcentajeGlobal} %`;
  pintarResumen('zonas', progreso.zonas);
  pintarResumen('slimes', progreso.slimes);
  pintarResumen('logros', progreso.logros);
  $('[data-por-zona]').replaceChildren(...progreso.slimesPorZona.map(filaZona));
  $('[data-pasos]').replaceChildren(...pasos(progreso.siguientesPasos));
  const cambios = progreso.ultimasActividades.slice(0, CAMBIOS_VISIBLES).map(cambio);
  if (cambios.length === 0) {
    estado($('[data-cambios]'), 'Todavía no hay cambios.', 'li');
  } else {
    $('[data-cambios]').replaceChildren(...cambios);
  }
}

async function iniciar() {
  try {
    const partida = await iniciarCabecera();
    pintar(await api(`/partidas/${partida.id}/progreso`));
  } catch (error) {
    alerta($('[data-aviso]'), mensajeDe(error));
    $('[data-actualizada]').textContent = 'No se ha podido cargar la partida.';
    estado($('[data-por-zona]'), 'Sin datos.');
  }
}

iniciar();
