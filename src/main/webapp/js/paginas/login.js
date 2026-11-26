// Entrar: un 401 se muestra como alerta genérica encima del formulario.
import { conectarFormulario } from './acceso.js';

conectarFormulario('/auth/login', ['nombreUsuario', 'contrasena'], null);
