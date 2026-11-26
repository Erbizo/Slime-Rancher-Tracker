# Datos de prueba para `init.sql`

Son los datos que aparecen en las capturas de `capturas/`. Si `init.sql` carga exactamente esto, la web real con el usuario demo se puede comparar pantalla a pantalla con el diseño.

> Las zonas requisito y la zona de cada slime son aproximadas. Revísalas con lo que sabes del juego y, si cambias algo, apúntalo en `AI_WORKFLOW.md` como corrección manual.

## Zonas

| orden | nombre | inicial | requisito | descripción |
|---|---|---|---|---|
| 1 | The Ranch | sí | — | Tu rancho. Siempre abierta: aquí empieza cada partida. |
| 2 | Dry Reef | no | The Ranch | Islas de roca roja junto al rancho. |
| 3 | Indigo Quarry | no | Dry Reef | Cantera de piedra azul con pozas. |
| 4 | Moss Blanket | no | Dry Reef | Bosque húmedo y cerrado. |
| 5 | Ancient Ruins | no | Moss Blanket | Ruinas antiguas con teletransportes. |
| 6 | Glass Desert | no | Ancient Ruins | Desierto de dunas y cristal. |
| 7 | The Wilds | no | Glass Desert | Selva salvaje lejos del rancho. |

## Slimes (18)

| nombre | zona | dieta | rareza | color |
|---|---|---|---|---|
| Pink | Dry Reef | OMNIVORO | COMUN | `#FF7EB6` |
| Rock | Dry Reef | VERDURA | COMUN | `#6F8FC0` |
| Tabby | Dry Reef | CARNE | COMUN | `#FF9A3C` |
| Phosphor | Dry Reef | FRUTA | COMUN | `#C6EA2E` |
| Lucky | Dry Reef | CARNE | ESPECIAL | `#B8A98F` |
| Rad | Indigo Quarry | VERDURA | COMUN | `#4FD63A` |
| Crystal | Indigo Quarry | VERDURA | RARO | `#FF8CC6` |
| Puddle | Indigo Quarry | ESPECIAL | RARO | `#2E9BFF` |
| Boom | Moss Blanket | CARNE | COMUN | `#FF5A30` |
| Honey | Moss Blanket | FRUTA | COMUN | `#FFC928` |
| Hunter | Moss Blanket | CARNE | RARO | `#8C5CFF` |
| Gold | Moss Blanket | ESPECIAL | ESPECIAL | `#FFB800` |
| Quantum | Ancient Ruins | FRUTA | RARO | `#A070FF` |
| Dervish | Glass Desert | VERDURA | RARO | `#22C7D9` |
| Tangle | Glass Desert | CARNE | RARO | `#F2508F` |
| Mosaic | Glass Desert | VERDURA | RARO | `#9DB2C7` |
| Fire | Glass Desert | ESPECIAL | RARO | `#FF6A13` |
| Saber | The Wilds | CARNE | ESPECIAL | `#F08A1C` |

## Logros (15)

| categoría | nombre | descripción |
|---|---|---|
| EXPLORACION | Más allá del rancho | Desbloquea tu primera zona fuera de The Ranch. |
| EXPLORACION | Piedra azul | Desbloquea Indigo Quarry. |
| EXPLORACION | Musgo hasta las rodillas | Desbloquea Moss Blanket. |
| EXPLORACION | Lo que dejaron los antiguos | Desbloquea Ancient Ruins. |
| EXPLORACION | Arena de cristal | Desbloquea Glass Desert. |
| EXPLORACION | Todo el mapa | Desbloquea todas las zonas. |
| SLIMES | Primer contacto | Descubre tu primer slime. |
| SLIMES | Media Slimepedia | Descubre 9 slimes distintos. |
| SLIMES | Brillo dorado | Descubre el Gold slime. |
| SLIMES | Slimepedia completa | Descubre los 18 slimes. |
| RANCHO | Primer corral | Construye un corral en el rancho. |
| RANCHO | Silo lleno | Llena un silo hasta arriba. |
| RANCHO | Diez días sin Tarr | Pasa diez días seguidos sin que aparezca un Tarr. |
| SECRETOS | Diario escondido | Encuentra un diario oculto fuera del rancho. |
| SECRETOS | Camino de vuelta | Activa un teletransporte de regreso al rancho. |

## Usuarios y partidas

Contraseñas guardadas con un hash BCrypt **generado de verdad** (no inventado).

| usuario | contraseña | para qué |
|---|---|---|
| `rancher` | `slime1234` | Usuario demo de las capturas y del README |
| `vecina` | `slime5678` | Segundo usuario, para probar el 403 al tocar partidas ajenas |

Las fechas se guardan relativas a `NOW()` para que el panel siempre enseñe "hoy" y "ayer":

### `rancher` · Primera partida (58 %)

- **Zonas (5/7):** The Ranch (NOW() − 23 días), Dry Reef (− 23 días), Indigo Quarry (− 17 días), Moss Blanket (− 14 días), Ancient Ruins (NOW() − 2 minutos).
- **Slimes (11/18):** Pink, Rock, Tabby, Phosphor (− 23 días), Rad, Crystal, Puddle (− 16 días), Boom (− 13 días), Honey (NOW() − 1 día − 47 min), Hunter (NOW() − 1 día − 32 min), Quantum (NOW() − 11 minutos, nota: "Aparece al lado de los teletransportes de las ruinas.").
- **Logros (6/15):** Más allá del rancho (− 23 días), Piedra azul (− 17 días), Musgo hasta las rodillas (− 14 días), Lo que dejaron los antiguos (NOW(), nota: "Lo conseguí justo antes de cerrar la partida."), Primer contacto (− 23 días), Media Slimepedia (− 12 días).
- Progreso: (5/7 + 11/18 + 6/15) / 3 = 57,5 % → se muestra **58 %** (redondeo `HALF_UP`).

### `rancher` · Modo Rush (22 %)

- Zonas (3/7): The Ranch, Dry Reef, Indigo Quarry. Slimes (3/18): Pink, Rock, Rad. Logros (1/15): Primer contacto. Todo con fecha NOW() − 11 días.
- Progreso: (3/7 + 3/18 + 1/15) / 3 = 22,1 % → **22 %**.

### `vecina` · Mi rancho

- Solo The Ranch desbloqueada (lo que crea el registro).
