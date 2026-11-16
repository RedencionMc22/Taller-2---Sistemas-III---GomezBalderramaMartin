# Dispensador Autónomo de Medicamentos — Taller 2 (actividades 1 y 2)

Simulación de los dispositivos de entrada y salida del subsistema **«Retiro y evidencia»**
del Proyecto COMET, con sus objetos límite y un banco de pruebas con interfaz gráfica.
Trabajo práctico de **Sistemas de Información III** (UNSE, FCEyT, 2026).

| Consigna | Dispositivo simulado | Objeto límite | Tipo (Tabla 1) |
| --- | --- | --- | --- |
| 1 · Entrada | `SensorPosicionTapa` | `SensorPosicionTapaInput` | «external input device» |
| 1 · Entrada | `BalanzaDePrecision` | `BalanzaDePrecisionIO` | «external input/output device» |
| 2 · Salida | `Buzzer` | `BuzzerOutput` | «external output device» |
| 2 · Salida | `CerraduraTapa` | `CerraduraTapaOutput` | «external output device» |

Los objetos de control (`ControlRetiro` y `ControlGrabacion`) corresponden a la actividad 3 y
**no** forman parte de esta entrega: el banco de pruebas no contiene lógica de control.

## Requisitos

- JDK 17 o superior (`java -version`).
- Apache NetBeans con soporte de Maven (12 o posterior), o bien los scripts incluidos.
- No hace falta conexión a Internet: JUnit 5 viaja en `lib/`.

## Cómo abrir y ejecutar en NetBeans

1. **File → Open Project…** y elegir esta carpeta (`DispensadorComet`).
2. **Run** (F6) abre el banco de pruebas.
3. **Test** (Alt+F6) ejecuta las 41 pruebas JUnit 5.

## Cómo ejecutar sin NetBeans ni Maven

```bat
compilar-y-probar.cmd    :: compila y ejecuta las 41 pruebas
ejecutar-banco.cmd       :: abre el banco de pruebas ya compilado
```

Ambos dejan las clases en `build/`, que se regenera en cada compilación y no se entrega.

## Estructura

```
src/main/java/ar/edu/unse/sistemas3/
├── eventos/        contratos de eventos y enumeraciones (EstadoTapa, EventoTapa, ...)
├── dispositivos/   hardware simulado: SensorPosicionTapa, BalanzaDePrecision, Buzzer, CerraduraTapa
├── limite/         objetos límite «input» / «output» y DesbloqueoNoPermitidoException
├── bitacora/       RegistroEventos, la bitácora que oficia de oráculo de las pruebas
├── banco/          BancoPruebas (main) y BancoPruebasFrame (interfaz Swing)
└── util/           formato numérico uniforme

src/test/java/ar/edu/unse/sistemas3/
├── dispositivos/   pruebas de los dispositivos simulados
├── limite/         pruebas de los objetos límite
├── bitacora/       pruebas del registro
├── integracion/    pruebas de interacción entre dispositivos y objetos límite
└── herramientas/   generación de las capturas y del diagrama del informe
```

## Informe

- `docs/Actividades 1 y 2 - Taller 2 - Sistemas III.docx` — entrega escrita (12 páginas).
- `docs/generar-informe.py` — regenera el informe a partir del código y de
  `build/reports/TEST-junit-jupiter.xml` (`python docs/generar-informe.py`).
- `docs/capturas/` — capturas del banco de pruebas.
- `docs/diagrama-componentes.png` — figura 1, generada por `DiagramaComponentes`.

Las capturas y el diagrama se regeneran ejecutando `CapturaPantalla` y `DiagramaComponentes`
(paquete `herramientas`, en el árbol de pruebas), sin abrir ventanas.
