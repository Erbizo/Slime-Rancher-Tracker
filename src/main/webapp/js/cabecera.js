// Cabecera común: nombre de la partida seleccionada y botón "Salir".

import { api, ApiError } from './api.js';
import { guardarPartidaId, leerPartidaId, olvidarPartida } from './estado.js';

/** Conecta el enlace "Salir": cierra la sesión y vuelve a entrar. */
export function conectarSalir() {
  document.querySelector('[data-salir]')?.addEventListener('click', async (evento) => {
    evento.preventDefault();
    try {
      await api('/auth/logout', { metodo: 'POST' });
    } finally {
      olvidarPartida();
      window.location.href = 'login.html';
    }
  });
}

/**
 * Devuelve la partida con la que trabaja la página. Si no hay ninguna elegida (o ya no es del usuario),
 * coge la última modificada; si el usuario no tiene partidas, lo manda a crear una.
 */
export async function iniciarCabecera() {
  conectarSalir();
  let partida = null;
  const id = leerPartidaId();
  if (id !== null) {
    try {
      partida = await api(`/partidas/${id}`);
    } catch (error) {
      if (!(error instanceof ApiError) || ![403, 404].includes(error.status)) throw error;
      olvidarPartida();
    }
  }
  if (!partida) {
    const partidas = await api('/partidas');
    if (partidas.length === 0) {
      window.location.href = 'partidas.html';
      return new Promise(() => {});
    }
    partida = partidas[0];
    guardarPartidaId(partida.id);
  }
  document.querySelector('[data-nombre-partida]').textContent = partida.nombre;
  return partida;
}
