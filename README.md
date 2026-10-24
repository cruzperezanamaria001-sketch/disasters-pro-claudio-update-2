# 🌪️ Natural Disasters — Forge 1.20.1

Mod de **desastres naturales** que se activan con un **control** (panel con botones) o con comandos.

## Desastres incluidos

| Desastre | Qué hace |
|---|---|
| ⚫ **Agujero negro** | **Modelo 3D**: esfera negra con halo, disco de acreción giratorio y anillo de luz. Atrae entidades, devora bloques y mata lo que cae al núcleo |
| 🌊 **Tsunami** | **Ola 3D** curva con cresta de espuma + agua real que arrasa en la dirección en la que miras |
| 🌪️ **Tornado** | **Embudo 3D** giratorio de 3 capas. Vaga por el mapa, levanta mobs/jugadores y arranca bloques (que salen volando como bloques reales) |
| ☄️ **Meteorito** | **Roca 3D** incandescente girando, con estela de fuego. Explota y deja un cráter con el núcleo |
| 🌠 **Lluvia de meteoritos** | Muchos meteoritos sobre una zona |
| 🌋 **Volcán** | Cono de bloques reales que crece del suelo, cráter de lava, bombas de magma y **columna de ceniza 3D** |
| ❄️ **Ventisca** | **Nube 3D** blanca sobre la zona, nieve horizontal, lentitud, congelación y nieve/hielo en el mundo |
| 💧 **Inundación** | El agua sube por capas en toda la zona, se mantiene y luego baja |
| 🫨 **Terremoto** | Sacude entidades, agrieta el suelo, derrumba estructuras débiles y abre fisuras profundas |
| ⛈️ **Tormenta eléctrica** | **Nubarrón 3D** oscuro sobre la zona y rayos constantes (prefieren jugadores y mobs) |

> Los desastres 3D son **entidades visuales** (`DisasterVisualEntity`) dibujadas con geometría propia
> (esferas, discos, embudos, olas...). Las texturas están en `assets/disasters/textures/entity/`:
> puedes reemplazarlas por las tuyas manteniendo el nombre. La lógica y la geometría están en
> `entity/DisasterVisualEntity.java` y `client/DisasterVisualRenderer.java`.

## Cómo se usa

1. Consigue el **Control de Desastres** (pestaña *Herramientas y utilidades* del creativo, o crafteo abajo, o `/give @s disasters:disaster_controller`).
2. **Clic derecho** → se abre el panel.
3. Elige **Intensidad** (Baja / Media / EXTREMA) y **Objetivo** (*Donde miro* / *Sobre mí*).
4. Pulsa el desastre que quieras. El botón **Detener todos los desastres** los frena (y limpia el agua).

**Comandos** (requieren ser OP):
```
/disaster spawn <tipo> [intensidad 1-3]     # tipos: black_hole tsunami tornado meteor meteor_shower volcano blizzard flood earthquake thunderstorm
/disaster stop
```

**Crafteo** (opcional):
```
R D R        R = Redstone
D E D        D = Diamante
R D R        E = Ojo de Ender
```

## Permisos y configuración

Por defecto solo **OPs o jugadores en creativo** pueden usar el control. Se cambia en
`config/disasters-common.toml` (se crea al abrir el mundo):

| Opción | Por defecto | Para qué sirve |
|---|---|---|
| `blockDamage` | `true` | Si es `false`, los desastres **no rompen ni cambian bloques** (solo efectos y daño a entidades) |
| `requirePermission` | `true` | `false` = cualquiera con el item puede usarlo (útil en supervivencia) |
| `durationMultiplier` | `1.0` | Multiplica la duración de los desastres |
| `maxActiveDisasters` | `6` | Límite de desastres a la vez |

> ⚠️ **Haz copia de seguridad del mundo** antes de probar con intensidad EXTREMA. Estos desastres destruyen terreno y construcciones de verdad, y los de intensidad alta pueden bajar los TPS.
> Los desastres activos **no se guardan**: si cierras el mundo, se cancelan.

## Compilar el mod con GitHub (sin instalar nada)

1. Crea un repositorio nuevo en GitHub.
2. Sube **todo el contenido de esta carpeta** (incluida la carpeta oculta `.github`).
   La forma más fácil, desde una terminal dentro de la carpeta:
   ```bash
   git init
   git add .
   git commit -m "Natural Disasters mod"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```
   (con *GitHub Desktop* también funciona: "Add local repository" → Publish).
3. Ve a la pestaña **Actions** del repo: se ejecuta solo el workflow **Build mod** (tarda 3-6 min la primera vez).
4. Cuando termine en verde, entra a esa ejecución y descarga el artefacto **desastres-naturales-forge-1.20.1** (dentro está el `.jar`).
5. Mete el `.jar` en la carpeta `mods` de tu Minecraft **1.20.1 con Forge 47.x**.

**Releases automáticos:** si creas un tag `v1.0.0` (`git tag v1.0.0 && git push --tags`), el workflow publica el `.jar` en la sección *Releases*.

### Compilar en tu PC (opcional)
Necesitas JDK 17. Luego: `./gradlew build` → el jar queda en `build/libs/`.
Para abrirlo en IntelliJ: *Open* → selecciona la carpeta → espera a que Gradle sincronice → `./gradlew genIntellijRuns`.

## Estructura

```
src/main/java/com/desastres/
├─ DisastersMod.java            # clase principal
├─ config/DisasterConfig.java   # opciones
├─ item/ , registry/            # el item "Control de Desastres"
├─ entity/                      # entidad visual 3D (sincroniza forma/tamaño al cliente)
├─ network/                     # paquete cliente→servidor del panel
├─ client/                      # pantalla (GUI) del control
└─ disaster/
   ├─ Disaster.java             # clase base
   ├─ DisasterType.java         # lista de desastres (añade aquí los tuyos)
   ├─ DisasterManager.java      # tick global y spawn
   ├─ DisasterCommands.java     # /disaster
   └─ impl/                     # un archivo por desastre
```

### Añadir tu propio desastre
1. Crea `impl/MiDesastre.java` que extienda `Disaster` y sobreescriba `tick()` (llama a `finish()` cuando acabe; `onEnd()` para limpiar).
2. Añade una línea en el enum `DisasterType`.
3. Añade las claves de traducción `disasters.disaster.<id>` y `.desc` en `assets/disasters/lang/*.json`.
   El botón aparece solo en el panel.

## Licencia
MIT — úsalo y modifícalo como quieras.
