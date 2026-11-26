// Modo claro/oscuro. Por defecto sigue al sistema (prefers-color-scheme);
// el botón [data-alternar-tema] fuerza uno y lo recuerda en localStorage.
// Se carga en <head> sin defer para que no haya parpadeo al abrir la página.
(function () {
  var raiz = document.documentElement;
  try {
    var guardado = localStorage.getItem('tema');
    if (guardado === 'claro' || guardado === 'oscuro') raiz.dataset.tema = guardado;
  } catch (e) { /* almacenamiento no disponible: se sigue al sistema */ }

  function temaActual() {
    if (raiz.dataset.tema) return raiz.dataset.tema;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro';
  }

  function actualizarBotones() {
    var oscuro = temaActual() === 'oscuro';
    document.querySelectorAll('[data-alternar-tema]').forEach(function (boton) {
      boton.setAttribute('aria-label', oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
      boton.setAttribute('aria-pressed', String(oscuro));
    });
  }

  document.addEventListener('click', function (evento) {
    var boton = evento.target.closest('[data-alternar-tema]');
    if (!boton) return;
    var nuevo = temaActual() === 'oscuro' ? 'claro' : 'oscuro';
    raiz.dataset.tema = nuevo;
    try { localStorage.setItem('tema', nuevo); } catch (e) { /* sin persistencia */ }
    actualizarBotones();
  });

  document.addEventListener('DOMContentLoaded', actualizarBotones);
})();
