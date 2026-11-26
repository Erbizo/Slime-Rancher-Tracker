// Crear cuenta: los tres errores de campo salen a la vez; un nombre ya usado (409) va bajo "Usuario".
import { conectarFormulario } from './acceso.js';

conectarFormulario('/auth/registro', ['nombreUsuario', 'contrasena', 'confirmacion'], 'nombreUsuario');
