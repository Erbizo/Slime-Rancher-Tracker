# Guía visual — Slime Rancher Tracker

Diseño **aprobado**. La interfaz real (`src/main/webapp`) tiene que quedar como estas pantallas: mismos colores, tipografías, formas, estructura y textos. Solo cambian los datos, que vendrán de la API.

| Carpeta | Qué hay |
|---|---|
| `capturas/` | Cómo debe verse cada pantalla en modo claro (1280 px y una a 390 px de móvil) y varias en modo oscuro |
| `DATOS_DEMO.md` | Los datos de `init.sql` que salen en las capturas (zonas, slimes, logros, usuarios y su progreso) |
| `pantallas/` | HTML + `estilos.css` + `tema.js` de referencia. Ábrelos en el navegador. El CSS ya tiene los valores exactos de los dos modos: úsalo como base de `css/` |

> Los HTML de `pantallas/` son estáticos: muestran un estado fijo y algunos estados de error de ejemplo (marcados con un comentario `<!-- Estado de ejemplo -->`). La lógica real va en los módulos JS que pide el prompt de la fase 5.

## Capturas

| Pantalla | Archivo | Estado que enseña |
|---|---|---|
| Panel de progreso | `capturas/01-panel.png` | Partida al 58 % |
| Slimepedia | `capturas/02-slimepedia.png` | Lucky (sin descubrir, zona abierta) seleccionado |
| Zonas | `capturas/03-zonas.png` | Aviso 409 al intentar bloquear Dry Reef |
| Logros | `capturas/04-logros.png` | 6 de 15, un logro con nota |
| Entrar | `capturas/05-entrar.png` | Error 401 de login |
| Crear cuenta | `capturas/06-registro.png` | Error 400 con los tres campos mal a la vez |
| Mis partidas | `capturas/07-partidas.png` | Error 409 de nombre repetido + confirmación de borrado |
| Panel en móvil | `capturas/08-panel-movil.png` | 390 px: todo en una columna |
| Panel (oscuro) | `capturas/11-panel-oscuro.png` | Mismo estado que 01 en modo oscuro |
| Slimepedia (oscuro) | `capturas/12-slimepedia-oscuro.png` | Mismo estado que 02 en modo oscuro |
| Zonas (oscuro) | `capturas/13-zonas-oscuro.png` | Mismo estado que 03 en modo oscuro |
| Entrar (oscuro) | `capturas/14-entrar-oscuro.png` | Mismo estado que 05 en modo oscuro |

## Dirección (charles-unslop)

- **Producto:** tracker de progreso de Slime Rancher 1 por partida: zonas, Slimepedia y logros.
- **Carácter:** blando, curioso, ordenado.
- **Mundo:** los menús del juego y el agua y el cielo del Far, Far Range. De ahí sale el fondo verde agua y la forma redondeada de todo.
- **Momento:** la Slimepedia. Los slimes sin descubrir son siluetas grises con "?" y al marcarlos aparecen con su color y su cara.
- **Firma:** todas las marcas de "hecho" tienen forma de **plort** (gota girada 45°): pips del panel, zonas, logros y últimos cambios.
- **Concepto:** la web es la Slimepedia de tu partida; el color solo aparece cuando lo has descubierto.

## Colores

Dos modos con los mismos tokens. Por defecto la web **sigue al sistema** (`prefers-color-scheme`); el botón de sol/luna de la cabecera (arriba a la derecha en entrar/registro) fuerza uno u otro y se recuerda en `localStorage` (clave `tema`, atributo `data-tema` en `<html>`). Ver `pantallas/tema.js`.

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `--fondo` | `#E0F3EF` | `#0C1D1C` | Fondo de página |
| `--superficie` | `#FFFFFF` | `#132A28` | Tarjetas, cabecera |
| `--elevado` | `#CDEAE4` | `#1F3E3B` | Elemento activo del menú, control segmentado, botón desactivado |
| `--suave` | `#EEF8F6` | `#183330` | Fondo de campos y notas |
| `--texto` | `#0F2E2C` | `#E6F4F1` | Texto principal |
| `--texto-2` | `#3D5E5A` | `#B0CBC6` | Texto secundario |
| `--texto-3` | `#4E6E6A` | `#8FAEA9` | Meta, fechas, etiquetas |
| `--linea` / `--linea-fuerte` | `rgba(15,46,44,.10)` / `.16` | `rgba(230,244,241,.10)` / `.18` | Bordes finos |
| `--pendiente` | `rgba(15,46,44,.28)` | `rgba(230,244,241,.34)` | Borde de los plorts sin hacer |
| `--acento` | `#E0156E` | `#E0156E` | Único acento: fondo del botón principal, selección, plorts hechos |
| `--acento-hover` | `#B80F59` | `#C4105F` | Hover del acento, fondo del botón de borrar |
| `--acento-texto` | `#C4105F` | `#FF7DB0` | Enlaces y texto en color de acento (contraste ≥ 4,5:1 sobre el fondo) |
| `--error-campo` | `#B80F59` | `#FF8DBA` | Mensajes de error bajo los campos, botón "Borrar" de texto |
| `--error-fondo` / `--error-texto` | `#FDE7F0` / `#9A0E4B` | `#3B1526` / `#FFB8D3` | Alertas y confirmación de borrado |
| `--aviso-borde` | `#F4A3C3` | `#8A2D57` | Borde del aviso de 409 |
| `--silueta` | `#C3DCD7` | `#2E4D49` | Slimes sin descubrir |
| `--ojo` | `#0F2E2C` | `#0F2E2C` | Ojos y boca de los slimes (siempre oscuros, van sobre su color) |
| `--btn-oscuro-fondo` / `-texto` | `#0F2E2C` / `#FFFFFF` | `#E6F4F1` / `#0F2E2C` | Botón "Guardar nota" |

En oscuro el acento como **fondo** no cambia (el texto blanco encima sigue a 4,7:1), pero como **texto** pasa a `--acento-texto` rosa claro para leerse sobre el fondo oscuro. Los colores de los slimes son los mismos en los dos modos.

El acento es poco: botón principal, selección y marcas de progreso. El resto del color está en los slimes:

| Slime | Color | Slime | Color | Slime | Color |
|---|---|---|---|---|---|
| Pink | `#FF7EB6` | Rad | `#4FD63A` | Quantum | `#A070FF` |
| Rock | `#6F8FC0` | Crystal | `#FF8CC6` | Dervish | `#22C7D9` |
| Tabby | `#FF9A3C` | Puddle | `#2E9BFF` | Tangle | `#F2508F` |
| Phosphor | `#C6EA2E` | Boom | `#FF5A30` | Mosaic | `#9DB2C7` |
| Lucky | `#B8A98F` | Honey | `#FFC928` | Fire | `#FF6A13` |
| Gold | `#FFB800` | Hunter | `#8C5CFF` | Saber | `#F08A1C` |

El color de cada slime vendrá de la BD (añadir columna `color CHAR(7)` a `slime`) o de un mapa en el JS; no lo pongas a mano en cada HTML.

## Tipografía

- **Nunito** (400, 500, 600) para toda la interfaz. 15 px de base. Números con `font-variant-numeric: tabular-nums`.
- **Fredoka** (500) solo para la marca, el título de cada página (40 px; 32 px en entrar/registro) y el número grande del panel (128 px; 96 px en móvil).
- Nunca pesos 700–900, nunca mayúsculas con espaciado, sin cursiva para enfatizar.

## Formas y componentes

- **Slime:** `border-radius: 50% 50% 44% 44% / 64% 64% 36% 36%`. Tamaños 26×21 (panel), 68×56 (fichas), 104×84 (detalle). Ojos y boca en `--texto`. El logotipo es un slime rosa del acento.
- **Plort:** cuadrado con `border-radius: 2px 50% 50% 50%` girado 45°. Hecho = relleno de acento; pendiente = borde de 1,5 px.
- **Radios:** tarjetas 16 px, fichas 14 px, botones y campos 10–12 px, tarjeta de acceso 20 px.
- **Profundidad:** una sola señal por superficie: sombra suave en tarjetas, borde fino en botones secundarios.
- **Botones:** principal en acento con texto blanco (44–48 px de alto); secundario blanco con borde; borrar en `--acento-hover`; desactivado en `--elevado`.
- **Iconos:** solo trazo (estilo Lucide, 16 px, trazo 1,75). Nada de emojis.
- **Espaciado:** rejilla de 4 px. Separación entre grupos con espacio, no con líneas divisorias.

## Pantallas

**Cabecera común** (todas menos entrar/registro): marca a la izquierda, menú Panel / Zonas / Slimepedia / Logros con el activo sobre `--elevado`, a la derecha el selector "Partida **nombre**" (lleva a Mis partidas), el botón de tema (luna en claro, sol en oscuro, 40×40, con `aria-label`) y "Salir". En Mis partidas, en vez del selector pone "Conectado como **usuario**" (es texto, no un botón).

1. **Panel:** cabecera con "Partida, actualizada…", el porcentaje global gigante y la frase debajo. A la derecha, tarjeta con tres filas (Zonas, Slimepedia, Logros): un plort por elemento y "X de Y". Debajo, dos columnas (`minmax(0,1fr) 320px`; una sola bajo 880 px): "Slimepedia por zona" con un mini slime por slime de la zona (en su color o silueta) y "zona bloqueada" si toca; a la derecha "Ahora puedes" (el primer paso en acento, el resto en blanco) y "Últimos cambios".
2. **Slimepedia:** título y contador; filtro de zona (select) y control segmentado Todos / Descubiertos / Por descubrir. Cuadrícula de fichas (`auto-fill, minmax(156px,1fr)`) y panel de detalle a la derecha. Ficha seleccionada con borde de acento de 2 px. Las de zonas bloqueadas, al 72 % de opacidad. Detalle según estado: descubierto → nota (máx. 200) + "Guardar nota" (oscuro) + "Quitar de la Slimepedia"; sin descubrir con zona abierta → "Marcar como descubierto" (acento); zona bloqueada → "Primero tienes que desbloquear X." + enlace a Zonas.
3. **Zonas:** lista ordenada en una tarjeta. Cada fila: plort, nombre + estado ("Zona inicial", "Desbloqueada el…", "Requiere X"), descripción, "N de M slimes" y botón (Bloquear / Desbloquear / Desbloquear desactivado). Los 409 se muestran en un aviso con borde rosa encima de la lista, con botón de cerrar.
4. **Logros:** una tarjeta por categoría (Exploración, Slimes, Rancho, Secretos) con "X de Y". Cada logro: check en forma de plort (botón de 44×44 con `role="checkbox"`), nombre, fecha, descripción y nota opcional sobre `--suave`.
5. **Entrar / Crear cuenta:** columna de 400 px centrada, fila de slimes de colores asomando por encima de la tarjeta. Errores de campo debajo de cada input (borde de acento) y el 401 como alerta genérica.
6. **Mis partidas:** campo + "Crear partida" arriba (error debajo), lista con nombre (enlace al panel), fecha, porcentaje en Fredoka, "Renombrar" y "Borrar". Borrar abre una confirmación en línea sobre `--error-fondo`.

## Reglas que no se pueden romper

- Nada de gradientes, brillos, glassmorphism, emojis ni sombras de color.
- Un solo acento. No añadir colores nuevos de interfaz.
- Contraste mínimo 4,5:1 en texto, en los dos modos.
- Ningún color escrito a mano fuera de `:root`: todo con tokens, para que el modo oscuro funcione solo.
- Sin imágenes, logos, fuentes ni sprites oficiales del juego: los slimes se dibujan con CSS.
- Responsive desde 375 px sin scroll horizontal.
