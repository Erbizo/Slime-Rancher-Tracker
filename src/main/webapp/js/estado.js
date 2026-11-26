// Partida seleccionada. Se guarda en localStorage; un ?partida=ID en la URL manda sobre lo guardado.

const CLAVE = 'partidaSeleccionada';

export function leerPartidaId() {
  const enUrl = new URLSearchParams(window.location.search).get('partida');
  if (enUrl && /^\d+$/.test(enUrl)) {
    guardarPartidaId(enUrl);
    return Number(enUrl);
  }
  try {
    const guardada = localStorage.getItem(CLAVE);
    return guardada && /^\d+$/.test(guardada) ? Number(guardada) : null;
  } catch {
    return null;
  }
}

export function guardarPartidaId(id) {
  try {
    localStorage.setItem(CLAVE, String(id));
  } catch {
    // sin almacenamiento: se usará la primera partida cada vez
  }
}

export function olvidarPartida() {
  try {
    localStorage.removeItem(CLAVE);
  } catch {
    // nada que borrar
  }
}
