// Cliente de la API: fetch con la cookie de sesión, timeout de 10 s y errores uniformes.

const TIEMPO_MAXIMO_MS = 10000;

/** Error de la API con el formato común del servidor: status, mensaje y errores por campo. */
export class ApiError extends Error {
  constructor(status, mensaje, campos = {}) {
    super(mensaje);
    this.status = status;
    this.mensaje = mensaje;
    this.campos = campos;
  }
}

/**
 * Llama a la API (rutas relativas a /api, p. ej. '/partidas').
 * Un 401 lleva a la pantalla de entrar, salvo que se pida lo contrario (login y registro).
 */
export async function api(ruta, { metodo = 'GET', cuerpo, redirigirSi401 = true } = {}) {
  const control = new AbortController();
  const temporizador = setTimeout(() => control.abort(), TIEMPO_MAXIMO_MS);
  let respuesta;
  try {
    respuesta = await fetch('api' + ruta, {
      method: metodo,
      credentials: 'same-origin',
      headers: cuerpo === undefined ? {} : { 'Content-Type': 'application/json' },
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      signal: control.signal,
    });
  } catch (error) {
    throw new ApiError(0, error.name === 'AbortError'
      ? 'El servidor está tardando demasiado. Inténtalo de nuevo en un momento.'
      : 'No se puede conectar con el servidor. Comprueba tu conexión.');
  } finally {
    clearTimeout(temporizador);
  }

  if (respuesta.status === 204) {
    return null;
  }
  const datos = await respuesta.json().catch(() => null);
  if (respuesta.ok) {
    return datos;
  }
  if (respuesta.status === 401 && redirigirSi401) {
    window.location.href = 'login.html';
    return new Promise(() => {}); // la página se va: no seguimos
  }
  throw new ApiError(respuesta.status, datos?.mensaje ?? 'Ha ocurrido un error inesperado.', datos?.campos ?? {});
}
