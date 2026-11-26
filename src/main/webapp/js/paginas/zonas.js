// Zonas de la partida: desbloquear y bloquear. Los 409 se muestran en un aviso encima de la lista.

import { api } from '../api.js';
import { iniciarCabecera } from '../cabecera.js';
import { alerta, conBoton, crear, estado, iconoCerrar, mensajeDe, plort } from '../ui.js';
import { conArticulo } from '../fechas.js';

const $ = (selector) => document.querySelector(selector);
const aviso = $('[data-aviso]');
const lista = $('[data-lista]');
let partidaId;

function textoEstado(zona) {
  if (zona.esInicial) return 'Zona inicial';
  if (zona.desbloqueada) return `Desbloqueada ${conArticulo(zona.desbloqueadaEn)}`;
  return `Requiere ${zona.requisitoNombre}`;
}

function textoSlimes(zona) {
  return zona.slimesTotal === 0 ? 'Sin slimes' : `${zona.slimesDescubiertos} de ${zona.slimesTotal} slimes`;
}

function boton(zona) {
  if (zona.desbloqueada) {
    return crear('button', { clase: 'btn btn--secundario btn--mediano', texto: 'Bloquear', atributos: { type: 'button', 'data-zona': zona.id, 'data-accion': 'bloquear' } });
  }
  return crear('button', {
    clase: zona.puedeDesbloquear ? 'btn btn--primario btn--mediano' : 'btn btn--mediano',
    texto: 'Desbloquear',
    atributos: { type: 'button', 'data-zona': zona.id, 'data-accion': 'desbloquear', disabled: !zona.puedeDesbloquear },
  });
}

function fila(zona) {
  const nombre = crear('span', { clase: zona.desbloqueada ? 'zona__nombre' : 'zona__nombre zona__nombre--bloqueada', texto: zona.nombre });
  return crear('li', {}, [
    plort(zona.desbloqueada, 'medio'),
    crear('div', { clase: 'zona__texto' }, [
      crear('div', { clase: 'zona__cabeza' }, [nombre, crear('span', { clase: 'meta', texto: textoEstado(zona) })]),
      crear('p', { clase: 'subtitulo zona__descripcion', texto: zona.descripcion }),
    ]),
    crear('span', { clase: 'zona__slimes', texto: textoSlimes(zona) }),
    crear('div', { clase: 'zona__accion' }, [boton(zona)]),
  ]);
}

function mostrarAviso(titulo, mensaje) {
  const cerrar = crear('button', { clase: 'btn btn--pequeno aviso__cerrar', atributos: { type: 'button', 'aria-label': 'Cerrar aviso' } }, [iconoCerrar()]);
  cerrar.addEventListener('click', () => aviso.replaceChildren());
  aviso.replaceChildren(crear('div', { clase: 'aviso', atributos: { role: 'status' } }, [
    crear('span', {}, [crear('strong', { texto: titulo }), document.createTextNode(' ' + mensaje)]),
    cerrar,
  ]));
}

async function cargar(enfocarZona) {
  const zonas = await api(`/partidas/${partidaId}/zonas`);
  const abiertas = zonas.filter((zona) => zona.desbloqueada).length;
  $('[data-subtitulo]').textContent = `${abiertas} de ${zonas.length} zonas desbloqueadas`;
  lista.replaceChildren(...zonas.map(fila));
  if (enfocarZona) lista.querySelector(`[data-zona="${enfocarZona}"]`)?.focus();
}

lista.addEventListener('click', async (evento) => {
  const pulsado = evento.target.closest('button[data-zona]');
  if (!pulsado) return;
  const zonaId = pulsado.dataset.zona;
  const bloquear = pulsado.dataset.accion === 'bloquear';
  await conBoton(pulsado, async () => {
    try {
      await api(`/partidas/${partidaId}/zonas/${zonaId}`, { metodo: bloquear ? 'DELETE' : 'POST' });
      aviso.replaceChildren();
      await cargar(zonaId);
    } catch (error) {
      mostrarAviso(bloquear ? 'No se ha podido bloquear.' : 'No se ha podido desbloquear.', mensajeDe(error));
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
    estado(lista, 'No se han podido cargar las zonas.', 'li');
  }
}

iniciar();
