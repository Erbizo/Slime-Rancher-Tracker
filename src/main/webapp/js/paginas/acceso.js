// Lógica común de Entrar y Crear cuenta: enviar el formulario y mostrar los errores del servidor.

import { api } from '../api.js';
import { olvidarPartida } from '../estado.js';
import { alerta, conBoton, limpiarErrores, mensajeDe, mostrarErrores } from '../ui.js';

/**
 * @param ruta '/auth/login' o '/auth/registro'
 * @param campos nombres de los campos del formulario que se envían
 * @param campo409 campo bajo el que se pone un 409 (o null para mostrarlo como alerta)
 */
export function conectarFormulario(ruta, campos, campo409) {
  const formulario = document.querySelector('[data-formulario]');
  const aviso = document.querySelector('[data-aviso]');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limpiarErrores(formulario);
    aviso.replaceChildren();
    const cuerpo = Object.fromEntries(campos.map((nombre) => [nombre, formulario.elements[nombre].value]));
    await conBoton(formulario.querySelector('[type="submit"]'), async () => {
      try {
        await api(ruta, { metodo: 'POST', cuerpo, redirigirSi401: false });
        olvidarPartida(); // la partida guardada podría ser de otro usuario
        window.location.href = 'index.html';
      } catch (error) {
        const errores = error.status === 409 && campo409 ? { [campo409]: error.mensaje } : error.campos;
        if (mostrarErrores(formulario, errores).length > 0 || Object.keys(errores ?? {}).length === 0) {
          alerta(aviso, mensajeDe(error));
        }
      }
    });
  });
}
