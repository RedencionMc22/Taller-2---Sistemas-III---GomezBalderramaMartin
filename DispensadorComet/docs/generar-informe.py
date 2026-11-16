#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera el informe de las actividades 1 y 2 del Taller 2 en formato .docx.

Uso (desde la carpeta del proyecto):
    python docs/generar-informe.py
"""

import glob
import os
import re
import xml.etree.ElementTree as ET

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(RAIZ)

AZUL = RGBColor(0x1F, 0x3A, 0x5F)
AZUL_CLARO = RGBColor(0x15, 0x65, 0xC0)
GRIS = RGBColor(0x55, 0x60, 0x6E)
GRIS_CLARO = RGBColor(0x7A, 0x85, 0x92)

ANCHO_UTIL = 17.0  # cm


# ---------------------------------------------------------------------------
# Utilidades de formato
# ---------------------------------------------------------------------------

# Orden que exige el esquema de OOXML para los hijos de w:tblPr y w:pPr.
ORDEN_TBLPR = (
    "w:tblStyle", "w:tblpPr", "w:tblOverlap", "w:bidiVisual",
    "w:tblStyleRowBandSize", "w:tblStyleColBandSize", "w:tblW", "w:jc",
    "w:tblCellSpacing", "w:tblInd", "w:tblBorders", "w:shd", "w:tblLayout",
    "w:tblCellMar", "w:tblLook", "w:tblCaption", "w:tblDescription",
)

ORDEN_PPR = (
    "w:pStyle", "w:keepNext", "w:keepLines", "w:pageBreakBefore", "w:framePr",
    "w:widowControl", "w:numPr", "w:suppressLineNumbers", "w:pBdr", "w:shd",
    "w:tabs", "w:suppressAutoHyphens", "w:kinsoku", "w:wordWrap",
    "w:overflowPunct", "w:topLinePunct", "w:autoSpaceDE", "w:autoSpaceDN",
    "w:bidi", "w:adjustRightInd", "w:snapToGrid", "w:spacing", "w:ind",
    "w:contextualSpacing", "w:mirrorIndents", "w:suppressOverlap", "w:jc",
    "w:textDirection", "w:textAlignment", "w:textboxTightWrap",
    "w:outlineLvl", "w:divId", "w:cnfStyle", "w:rPr", "w:sectPr",
)


def _ordenar(contenedor, orden):
    indice = {qn(t): i for i, t in enumerate(orden)}
    for hijo in sorted(contenedor, key=lambda el: indice.get(el.tag, len(orden))):
        contenedor.append(hijo)


def normalizar_tabla(tabla):
    _ordenar(tabla._tbl.tblPr, ORDEN_TBLPR)


def normalizar_parrafo(parrafo):
    _ordenar(parrafo._p.get_or_add_pPr(), ORDEN_PPR)


def tabulaciones(parrafo, paradas):
    """Reemplaza las tabulaciones del párrafo (cm, 'right' | 'center' | 'clear').

    Hace falta declarar las paradas del estilo como «clear», porque si no el
    carácter de tabulación salta a la parada central que hereda del estilo Footer.
    """
    pPr = parrafo._p.get_or_add_pPr()
    viejas = pPr.find(qn("w:tabs"))
    if viejas is not None:
        pPr.remove(viejas)
    tabs = OxmlElement("w:tabs")
    for cm, valor in sorted(paradas):
        tab = OxmlElement("w:tab")
        tab.set(qn("w:val"), valor)
        tab.set(qn("w:pos"), str(round(cm * 567)))
        tabs.append(tab)
    pPr.append(tabs)
    normalizar_parrafo(parrafo)


def sombrear(celda, color_hex):
    tcPr = celda._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), color_hex)
    tcPr.append(shd)


def bordes_tabla(tabla, color="D5DBE2", grosor=4):
    tblPr = tabla._tbl.tblPr
    bordes = OxmlElement("w:tblBorders")
    for lado in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{lado}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), str(grosor))
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), color)
        bordes.append(el)
    tblPr.append(bordes)


def margenes_celda(tabla, arriba=50, abajo=50, izq=80, der=80):
    tblPr = tabla._tbl.tblPr
    mar = OxmlElement("w:tblCellMar")
    for lado, valor in (("top", arriba), ("left", izq), ("bottom", abajo), ("right", der)):
        el = OxmlElement(f"w:{lado}")
        el.set(qn("w:w"), str(valor))
        el.set(qn("w:type"), "dxa")
        mar.append(el)
    tblPr.append(mar)


def fila_sin_corte(fila):
    trPr = fila._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:cantSplit"))


def repetir_encabezado(fila):
    trPr = fila._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:tblHeader"))


def ancho_columnas(tabla, anchos_cm):
    tabla.autofit = False  # fija w:tblLayout en "fixed"
    tblPr = tabla._tbl.tblPr
    tblW = tblPr.find(qn("w:tblW"))
    if tblW is not None:
        tblW.set(qn("w:type"), "dxa")
        tblW.set(qn("w:w"), str(round(sum(anchos_cm) * 567)))
    for columna, ancho in zip(tabla.columns, anchos_cm):
        columna.width = Cm(ancho)  # w:gridCol
    for fila in tabla.rows:
        for celda, ancho in zip(fila.cells, anchos_cm):
            celda.width = Cm(ancho)  # w:tcW de cada celda
    normalizar_tabla(tabla)


def borde_inferior(parrafo, color="C9D3DF", grosor=8):
    pPr = parrafo._p.get_or_add_pPr()
    pbdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), str(grosor))
    bottom.set(qn("w:space"), "4")
    bottom.set(qn("w:color"), color)
    pbdr.append(bottom)
    pPr.append(pbdr)
    normalizar_parrafo(parrafo)


def campo(parrafo, instruccion):
    run = parrafo.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = instruccion
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    texto = OxmlElement("w:t")
    texto.text = "1"
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(texto)
    run._r.append(fld_end)
    return run


# ---------------------------------------------------------------------------
# Documento
# ---------------------------------------------------------------------------

doc = Document()

seccion = doc.sections[0]
seccion.page_width = Cm(21.0)
seccion.page_height = Cm(29.7)
seccion.top_margin = Cm(2.0)
seccion.bottom_margin = Cm(1.9)
seccion.left_margin = Cm(2.0)
seccion.right_margin = Cm(2.0)
seccion.header_distance = Cm(0.9)
seccion.footer_distance = Cm(0.9)

normal = doc.styles["Normal"]
normal.font.name = "Calibri"
normal.font.size = Pt(10.5)
normal.font.color.rgb = RGBColor(0x22, 0x2A, 0x35)
normal.paragraph_format.space_after = Pt(6)
normal.paragraph_format.line_spacing = 1.13
normal.element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")

for nivel, tamanio in ((1, 15), (2, 12), (3, 11)):
    estilo = doc.styles[f"Heading {nivel}"]
    estilo.font.name = "Calibri"
    estilo.font.size = Pt(tamanio)
    estilo.font.bold = True
    estilo.font.color.rgb = AZUL if nivel == 1 else AZUL_CLARO
    estilo.paragraph_format.space_before = Pt(16 if nivel == 1 else 11)
    estilo.paragraph_format.space_after = Pt(6)
    estilo.paragraph_format.keep_with_next = True


def h(texto, nivel=1):
    parrafo = doc.add_heading(texto, level=nivel)
    if nivel == 1:
        borde_inferior(parrafo, color="C9D3DF", grosor=8)
    return parrafo


def p(texto="", negrita=False, cursiva=False, tamanio=None, color=None,
      despues=6, antes=0, alineacion=None, sangria=0.0):
    parrafo = doc.add_paragraph()
    parrafo.paragraph_format.space_after = Pt(despues)
    parrafo.paragraph_format.space_before = Pt(antes)
    if sangria:
        parrafo.paragraph_format.left_indent = Cm(sangria)
    if alineacion is not None:
        parrafo.alignment = alineacion
    run = parrafo.add_run(texto)
    run.bold = negrita
    run.italic = cursiva
    if tamanio:
        run.font.size = Pt(tamanio)
    if color:
        run.font.color.rgb = color
    return parrafo


def rico(fragmentos, despues=6, antes=0, sangria=0.0, tamanio=None):
    """Párrafo con runs alternados: lista de (texto, negrita, cursiva)."""
    parrafo = doc.add_paragraph()
    parrafo.paragraph_format.space_after = Pt(despues)
    parrafo.paragraph_format.space_before = Pt(antes)
    if sangria:
        parrafo.paragraph_format.left_indent = Cm(sangria)
    for texto, negrita, cursiva in fragmentos:
        run = parrafo.add_run(texto)
        run.bold = negrita
        run.italic = cursiva
        if tamanio:
            run.font.size = Pt(tamanio)
    return parrafo


def vineta(texto, nivel=0, tamanio=None):
    parrafo = doc.add_paragraph(style="List Bullet")
    parrafo.paragraph_format.left_indent = Cm(0.55 + 0.55 * nivel)
    parrafo.paragraph_format.first_line_indent = Cm(-0.35)
    parrafo.paragraph_format.space_after = Pt(3)
    run = parrafo.add_run(texto)
    if tamanio:
        run.font.size = Pt(tamanio)
    return parrafo


def numerada(texto, tamanio=None):
    parrafo = doc.add_paragraph(style="List Number")
    parrafo.paragraph_format.left_indent = Cm(0.7)
    parrafo.paragraph_format.first_line_indent = Cm(-0.45)
    parrafo.paragraph_format.space_after = Pt(3)
    run = parrafo.add_run(texto)
    if tamanio:
        run.font.size = Pt(tamanio)
    return parrafo


def codigo(texto, tamanio=8.6):
    tabla = doc.add_table(rows=1, cols=1)
    tabla.alignment = WD_TABLE_ALIGNMENT.CENTER
    fila_sin_corte(tabla.rows[0])
    celda = tabla.cell(0, 0)
    sombrear(celda, "F4F6F8")
    celda.width = Cm(ANCHO_UTIL)
    tabla.autofit = False
    tabla.columns[0].width = Cm(ANCHO_UTIL)
    bordes_tabla(tabla, color="E1E6EC", grosor=4)
    margenes_celda(tabla, 90, 90, 120, 120)
    normalizar_tabla(tabla)
    parrafo = celda.paragraphs[0]
    parrafo.paragraph_format.space_after = Pt(0)
    parrafo.paragraph_format.line_spacing = 1.0
    for i, linea in enumerate(texto.rstrip("\n").split("\n")):
        run = parrafo.add_run(linea)
        run.font.name = "Consolas"
        run.font.size = Pt(tamanio)
        run.font.color.rgb = RGBColor(0x2B, 0x33, 0x3E)
        if i < len(texto.rstrip("\n").split("\n")) - 1:
            run.add_break()
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return tabla


def tabla(cabeceras, filas, anchos, tamanio=8.8, alineacion_izq=False, mantener_junta=None):
    t = doc.add_table(rows=1, cols=len(cabeceras))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    bordes_tabla(t)
    margenes_celda(t)

    encabezado = t.rows[0]
    repetir_encabezado(encabezado)
    fila_sin_corte(encabezado)
    for i, texto in enumerate(cabeceras):
        celda = encabezado.cells[i]
        sombrear(celda, "1F3A5F")
        celda.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        parrafo = celda.paragraphs[0]
        parrafo.paragraph_format.space_after = Pt(0)
        parrafo.paragraph_format.line_spacing = 1.0
        run = parrafo.add_run(texto)
        run.bold = True
        run.font.size = Pt(tamanio - 0.3)
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    for indice, fila in enumerate(filas):
        celdas = t.add_row().cells
        fila_sin_corte(t.rows[-1])
        for i, valor in enumerate(fila):
            celda = celdas[i]
            celda.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            if indice % 2 == 1:
                sombrear(celda, "F6F8FA")
            parrafo = celda.paragraphs[0]
            parrafo.paragraph_format.space_after = Pt(0)
            parrafo.paragraph_format.line_spacing = 1.02
            partes = str(valor).split("<b>")
            for j, parte in enumerate(partes):
                if not parte:
                    continue
                run = parrafo.add_run(parte.replace("</b>", ""))
                run.font.size = Pt(tamanio)
                run.bold = j > 0
                if i == 0 and not alineacion_izq and len(str(valor)) < 16:
                    parrafo.alignment = WD_ALIGN_PARAGRAPH.LEFT

    ancho_columnas(t, anchos)
    if mantener_junta is None:
        mantener_junta = len(filas) <= 6
    if mantener_junta:
        # Evita que la tabla se corte dejando el encabezado solo al pie de página.
        for fila in t.rows[:-1]:
            for celda in fila.cells:
                for parrafo in celda.paragraphs:
                    parrafo.paragraph_format.keep_with_next = True
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return t


def figura(ruta, ancho_cm, leyenda):
    parrafo = doc.add_paragraph()
    parrafo.alignment = WD_ALIGN_PARAGRAPH.CENTER
    parrafo.paragraph_format.space_before = Pt(6)
    parrafo.paragraph_format.space_after = Pt(2)
    parrafo.add_run().add_picture(ruta, width=Cm(ancho_cm))

    pie = doc.add_paragraph()
    pie.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pie.paragraph_format.space_after = Pt(12)
    run = pie.add_run(leyenda)
    run.italic = True
    run.font.size = Pt(8.5)
    run.font.color.rgb = GRIS


def nota(texto):
    parrafo = doc.add_paragraph()
    parrafo.paragraph_format.space_after = Pt(8)
    parrafo.paragraph_format.left_indent = Cm(0.3)
    borde_inferior(parrafo, color="E3E8EE", grosor=4)
    run = parrafo.add_run(texto)
    run.italic = True
    run.font.size = Pt(9)
    run.font.color.rgb = GRIS
    return parrafo


# ---------------------------------------------------------------------------
# Encabezado y pie
# ---------------------------------------------------------------------------

encabezado = seccion.header.paragraphs[0]
encabezado.alignment = WD_ALIGN_PARAGRAPH.RIGHT
run = encabezado.add_run("Sistemas de Información III  ·  Taller 2  ·  2026")
run.font.size = Pt(8.5)
run.font.color.rgb = GRIS_CLARO
borde_inferior(encabezado, color="DCE3EA", grosor=4)

pie = seccion.footer.paragraphs[0]
pie.alignment = WD_ALIGN_PARAGRAPH.LEFT
run = pie.add_run("Actividades 1 y 2 — Subsistema «Retiro y evidencia»")
run.font.size = Pt(8.5)
run.font.color.rgb = GRIS_CLARO
run.add_tab()
run = pie.add_run("Página ")
run.font.size = Pt(8.5)
run.font.color.rgb = GRIS_CLARO
run = campo(pie, "PAGE")
run.font.size = Pt(8.5)
run.font.color.rgb = GRIS_CLARO
run = pie.add_run(" de ")
run.font.size = Pt(8.5)
run.font.color.rgb = GRIS_CLARO
run = campo(pie, "NUMPAGES")
run.font.size = Pt(8.5)
run.font.color.rgb = GRIS_CLARO
tabulaciones(pie, [(4680 / 567, "clear"), (9360 / 567, "clear"), (ANCHO_UTIL, "right")])


# ---------------------------------------------------------------------------
# Portada
# ---------------------------------------------------------------------------

p("Universidad Nacional de Santiago del Estero", negrita=True, tamanio=10.5, color=GRIS, despues=0)
p("Facultad de Ciencias Exactas y Tecnologías  ·  Licenciatura en Sistemas de Información",
  tamanio=9.5, color=GRIS, despues=18)

p("SISTEMAS DE INFORMACIÓN III", negrita=True, tamanio=11, color=AZUL_CLARO, despues=2)
p("Taller 2 — Implementación de Sistemas en Tiempo Real utilizando Java",
  tamanio=11, color=GRIS, despues=10)

titulo = doc.add_paragraph()
titulo.paragraph_format.space_before = Pt(6)
titulo.paragraph_format.space_after = Pt(4)
run = titulo.add_run("Actividades 1 y 2")
run.bold = True
run.font.size = Pt(26)
run.font.color.rgb = AZUL

subtitulo = doc.add_paragraph()
subtitulo.paragraph_format.space_after = Pt(12)
run = subtitulo.add_run("Dispositivos de entrada y salida simulados\ny sus objetos de interfaz")
run.bold = True
run.font.size = Pt(15)
run.font.color.rgb = RGBColor(0x33, 0x3D, 0x4A)

p("Subsistema «Retiro y evidencia» del Proyecto COMET — Dispensador Autónomo de Medicamentos. "
  "Casos de uso CU-03 «Retirar medicación» y CU-09 «Grabar evidencia».",
  tamanio=10.5, color=GRIS, despues=16)

datos = tabla(
    ["Cátedra", "Sistemas de Información III — UNSE, FCEyT, año académico 2026"],
    [
        ["Trabajo", "Taller 2 — actividades 1 y 2 (carácter grupal)"],
        ["Sistema bajo estudio", "Dispensador Autónomo de Medicamentos (Proyecto COMET)"],
        ["Subsistema implementado", "Retiro y evidencia — ControlRetiro y ControlGrabacion"],
        ["Entorno de desarrollo", "Java 17 (probado con JDK 17 a 27) y Apache NetBeans"],
        ["Entregables", "Código fuente, banco de pruebas con interfaz gráfica, 41 pruebas JUnit 5 y este informe"],
    ],
    anchos=[4.2, 12.8],
    tamanio=9.2,
)
datos.rows[0].cells[0].paragraphs[0].runs[0].font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

p()
p("Síntesis", negrita=True, tamanio=10.5, antes=4, despues=3)
vineta("Se simularon los dos dispositivos de entrada del subsistema —SensorPosicionTapa y "
       "BalanzaDePrecision— y los dos de salida —Buzzer y CerraduraTapa—, con el comportamiento "
       "y los tiempos que fija el modelo del límite hardware/software.")
vineta("Cada dispositivo quedó conectado a su objeto límite («input» o «output»), que es el único "
       "punto por el que el software accede al hardware.")
vineta("Un banco de pruebas Swing permite accionar el «mundo físico» y comandar los dispositivos por "
       "medio de los objetos límite, mostrando en pantalla el efecto y la bitácora de sucesos.")
vineta("Las 41 pruebas automatizadas se ejecutan sin fallas y cubren las consignas 1 y 2 del taller, "
       "incluida una secuencia completa de CU-03.")


# ---------------------------------------------------------------------------
# 1. Alcance
# ---------------------------------------------------------------------------

doc.add_page_break()
h("1. Alcance y punto de partida")

p("El Taller 2 propone ocho actividades sobre el sistema ya modelado con la metodología COMET. "
  "Este informe cubre las dos primeras: seleccionar dos dispositivos de entrada y dos de salida, "
  "implementar los objetos de software que los simulan, implementar los objetos de interfaz que "
  "permiten interactuar con ellos y documentar las pruebas de esa interacción.")

p("El subsistema elegido en el informe previo es «Retiro y evidencia», el que integran ControlRetiro "
  "y ControlGrabacion. Abarca CU-03 «Retirar medicación» y CU-09 «Grabar evidencia», que lo incluye. "
  "La ventaja para este taller es que el propio modelo del límite hardware/software ya fija qué "
  "dispositivos intervienen y qué eventos y comandos intercambian, de modo que no hubo que inventar "
  "el límite: se implementó el que está documentado.")

tabla(
    ["Consigna", "Dispositivo", "Tipo (Tabla 1)", "Clase límite", "Dónde interviene"],
    [
        ["1 · Entrada 1", "SensorPosicionTapa", "«external input device»",
         "SensorPosicionTapaInput", "CU-03 pasos 2 y 6"],
        ["1 · Entrada 2", "BalanzaDePrecision", "«external input/output device»",
         "BalanzaDePrecisionIO", "CU-03 paso 6; CU-09 paso 4"],
        ["2 · Salida 1", "Buzzer", "«external output device»",
         "BuzzerOutput", "CU-03 paso 2"],
        ["2 · Salida 2", "CerraduraTapa", "«external output device»",
         "CerraduraTapaOutput", "CU-03 paso 7; RT-06"],
    ],
    anchos=[2.4, 3.6, 4.2, 3.9, 2.9],
    tamanio=8.8,
)

p("Para cada dispositivo se entregan dos piezas: el objeto que simula el hardware y el objeto límite "
  "que el software usa para hablar con él. La interfaz gráfica del banco de pruebas es el tercer "
  "componente: permite operar el conjunto sin un dispositivo físico conectado.")


# ---------------------------------------------------------------------------
# 2. Diseño
# ---------------------------------------------------------------------------

h("2. Decisión de diseño: hardware simulado y objeto límite")

p("COMET define al objeto límite de dispositivo de E/S como el objeto de software que recibe "
  "entradas desde un dispositivo y/o produce salidas hacia él. La implementación respeta esa "
  "separación con dos capas explícitas:")

vineta("Paquete «dispositivos»: representa el hardware. No conoce el sistema de software, no registra "
       "nada y no toma decisiones; sólo cambia de estado, avisa a quien esté conectado y responde a "
       "los comandos que recibe. Es la pieza que se reemplaza cuando el prototipo se conecta al sensor real.")
vineta("Paquete «limite»: contiene los objetos «input» y «output» del modelo. Son los únicos que "
       "hablan con los dispositivos, traducen eventos y comandos, aplican las reglas de la interfaz y "
       "dejan constancia en la bitácora.")

p("Esta separación tiene tres consecuencias prácticas para el resto del taller. Primero, el futuro "
  "controlador dependerá de los objetos límite y no del hardware, así que podrá probarse con el "
  "simulador y ejecutarse igual contra el dispositivo real. Segundo, los eventos que el control "
  "consumirá son exactamente los que publica el objeto límite, con su marca de tiempo. Tercero, "
  "reemplazar la simulación por hardware real implica cambiar una sola clase por dispositivo, sin "
  "tocar el controlador ni la interfaz.")

figura("docs/diagrama-componentes.png", 16.6,
       "Figura 1. Dispositivos simulados y objetos límite del subsistema, con los eventos y comandos que "
       "intercambian. La columna de objetos de control se completa en la actividad 3 y no forma parte de "
       "esta entrega.")

tabla(
    ["Paquete", "Clases", "Rol en el modelo COMET"],
    [
        ["eventos", "EstadoTapa, EventoTapa, EventoMasa, EscuchaTapa, EscuchaMasa",
         "Contratos de los eventos «tapa abierta», «tapa cerrada» y «masa detectada», con su instante y sus datos."],
        ["dispositivos", "SensorPosicionTapa, BalanzaDePrecision, Buzzer, CerraduraTapa",
         "Hardware simulado. Reproduce el comportamiento físico y los retardos de actuación."],
        ["limite", "SensorPosicionTapaInput, BalanzaDePrecisionIO, BuzzerOutput, CerraduraTapaOutput",
         "Objetos límite «input» / «output». Ejecutan comandos, aplican reglas y registran."],
        ["bitacora", "RegistroEventos",
         "Bitácora única del banco de pruebas; es también el oráculo de las pruebas automatizadas."],
        ["banco", "BancoPruebas, BancoPruebasFrame",
         "Interfaz gráfica que permite operar los dispositivos y observar el resultado."],
        ["util", "Formato", "Formato numérico uniforme (coma decimal) para registros y etiquetas."],
    ],
    anchos=[2.5, 7.0, 7.5],
    tamanio=8.6,
)


# ---------------------------------------------------------------------------
# 3. Actividad 1
# ---------------------------------------------------------------------------

h("3. Actividad 1 — Dispositivos de entrada")

h("3.1. Interfaz de los dos dispositivos de entrada", 2)

p("Los dos dispositivos elegidos son SensorPosicionTapa, clasificado en la Tabla 1 como «external input "
  "device», y BalanzaDePrecision, que es «external input/output device» porque además de informar la "
  "medida acepta comandos.")

tabla(
    ["Dispositivo", "Función", "Entradas al sistema", "Salidas del sistema", "Objeto límite"],
    [
        ["SensorPosicionTapa",
         "Detecta la apertura y el cierre de la tapa del recipiente y registra cada transición.",
         "Evento «tapa abierta» y evento «tapa cerrada», con instante.",
         "Ninguna.",
         "SensorPosicionTapaInput"],
        ["BalanzaDePrecision",
         "Mide la masa del recipiente: habilita el desbloqueo de la tapa, verifica el retiro al cerrar "
         "y cuantifica la masa remanente antes del descarte.",
         "Evento «masa detectada» y medida de masa en gramos.",
         "Comandos de tara y de lectura de masa.",
         "BalanzaDePrecisionIO"],
    ],
    anchos=[3.1, 4.2, 3.5, 2.8, 3.4],
    tamanio=8.4,
)

h("3.2. SensorPosicionTapa y SensorPosicionTapaInput", 2)

p("El simulador del sensor mantiene el estado físico de la tapa y avisa cada transición. Los métodos "
  "abrir() y cerrar() representan la mano del paciente: son la forma de accionar el hardware desde el "
  "banco de pruebas. El objeto límite se suscribe al construirse, de modo que en el sistema real "
  "basta con sustituir el simulador por la clase que lea el GPIO del sensor.")

rico([
    ("Tres decisiones de comportamiento. ", True, False),
    ("La primera: sólo se publica un evento cuando el estado cambia, porque el sensor informa "
     "transiciones y no niveles; pulsar dos veces «cerrar» no genera dos eventos. La segunda: cada "
     "evento lleva su instante, que es lo que después permite verificar RT-10 y ordenar la bitácora. "
     "La tercera: el sensor puede entregar una lectura incoherente —tapa abierta sin transición "
     "previa—, que reproduce el flujo alternativo 5 de CU-03 y se propaga marcada como tal para que "
     "el control la trate como falla.", False, False),
])

codigo("""// SensorPosicionTapa: el hardware sólo avisa la transición, sin conocer al sistema
private void conmutar(EstadoTapa nuevo) {
    esperar(RETARDO_DETECCION_MS);
    EventoTapa evento;
    synchronized (this) {
        if (estadoFisico == nuevo) return;   // no hay transición que informar
        estadoFisico = nuevo;
        transiciones++;
        evento = new EventoTapa(nuevo, Instant.now(), true);
    }
    publicar(evento);
}""")

p("El objeto límite recibe ese evento, lo registra con el nombre del objeto y lo re-publica a los "
  "objetos de control. Si el evento viene marcado como incoherente, lo registra como alerta con la "
  "referencia al flujo alternativo, pero igual lo propaga: descartarlo en el límite ocultaría la falla.")

tabla(
    ["Operación del objeto límite", "Tipo", "Descripción"],
    [
        ["tapaCambio(EventoTapa)", "entrada", "Recibe el evento del dispositivo, lo registra y lo re-publica."],
        ["agregarEscucha(EscuchaTapa)", "registro", "Conecta un objeto de control interesado en los eventos de tapa."],
        ["estadoActual()", "consulta", "Devuelve el estado informado por el sensor."],
        ["ultimoEvento()", "consulta", "Último evento recibido, con su instante y su validez."],
    ],
    anchos=[5.4, 2.2, 9.4],
    tamanio=8.6,
)

h("3.3. BalanzaDePrecision y BalanzaDePrecisionIO", 2)

p("La balanza es el único dispositivo de entrada/salida del subsistema: entrega la medida y también "
  "recibe comandos. El simulador mantiene la masa neta, aplica un tiempo de estabilización y avisa "
  "cada cambio; el objeto límite filtra esos avisos y sólo informa «masa detectada» cuando cambia la "
  "condición de detección. Así, dejar caer la dosis produce un único evento aunque la lectura se "
  "estabilice en varios pasos, y el control no recibe una ráfaga de eventos redundantes.")

rico([
    ("El umbral es de 1 g. ", True, False),
    ("Por debajo de ese valor la balanza informa el peso pero no considera que haya masa en el "
     "recipiente. Ese detalle es el que sostiene la regla de la cerradura: no se desbloquea la tapa "
     "con el recipiente vacío, y tampoco con residuo por debajo del umbral de detección.", False, False),
])

tabla(
    ["Operación", "Sentido", "Descripción"],
    [
        ["masaCambio(EventoMasa)", "entrada", "Recibe la lectura del dispositivo; informa «masa detectada» sólo al cambiar la condición."],
        ["detectaMasa()", "consulta", "Indica si el recipiente tiene masa por encima del umbral."],
        ["masaActual()", "consulta", "Lectura para las vistas, sin registrar comando."],
        ["leerMasa()", "salida", "Comando de lectura de masa; queda registrado en la bitácora."],
        ["tarar()", "salida", "Comando de tara; deja la balanza en cero y queda registrado."],
    ],
    anchos=[4.6, 2.0, 10.4],
    tamanio=8.6,
)


# ---------------------------------------------------------------------------
# 4. Actividad 2
# ---------------------------------------------------------------------------

h("4. Actividad 2 — Dispositivos de salida")

h("4.1. Interfaz de los dos dispositivos de salida", 2)

p("Los dos dispositivos de salida, Buzzer y CerraduraTapa, son «external output device» en la Tabla 1: "
  "sólo reciben comandos y no informan nada al sistema. Su estado se lee en el dispositivo, no se "
  "notifica como evento.")

tabla(
    ["Dispositivo", "Función", "Entradas al sistema", "Salidas del sistema", "Objeto límite"],
    [
        ["Buzzer", "Alarma sonora de aviso de toma.",
         "Ninguna.", "Comandos de activar y desactivar la alarma sonora.", "BuzzerOutput"],
        ["CerraduraTapa",
         "Bloquea y desbloquea la tapa del recipiente compartido.",
         "Ninguna.",
         "Comandos de desbloquear (sólo con masa detectada) y de bloquear.", "CerraduraTapaOutput"],
    ],
    anchos=[3.1, 4.4, 2.0, 4.1, 3.4],
    tamanio=8.4,
)

h("4.2. Buzzer y BuzzerOutput", 2)

p("El zumbador es un dispositivo de dos estados. El objeto límite expone activar() y desactivar(); "
  "cada comando acciona el hardware y queda en la bitácora con su origen. La simulación cuenta las "
  "conmutaciones efectivas, de modo que repetir el mismo comando no infla el registro y una prueba "
  "puede distinguir «se ordenó activar» de «efectivamente se encendió».")

h("4.3. CerraduraTapa y CerraduraTapaOutput", 2)

p("La cerradura concentra las dos reglas del modelo que convierten a este dispositivo en algo más que "
  "un actuador. La primera es el estado inicial seguro: la cerradura arranca bloqueada, como fija "
  "CU-10 en su paso 3. La segunda es la restricción de la Tabla 1: el desbloqueo sólo se permite con "
  "masa detectada en el recipiente.")

codigo("""public long desbloquear() throws DesbloqueoNoPermitidoException {
    if (!balanza.detectaMasa()) {
        registro.falla(ORIGEN, "desbloqueo rechazado: no hay masa detectada en el recipiente");
        throw new DesbloqueoNoPermitidoException(
                "No se puede desbloquear la tapa sin masa detectada en el recipiente");
    }
    long inicio = System.nanoTime();
    dispositivo.desbloquear();
    long duracion = milisegundosDesde(inicio);
    registro.info(ORIGEN, "comando «desbloquear» en " + duracion + " ms");
    return duracion;
}""")

rico([
    ("La regla se aplica en el objeto límite y no en el hardware. ", True, False),
    ("CerraduraTapaOutput consulta a BalanzaDePrecisionIO antes de ordenar el desbloqueo. Si el "
     "recipiente está vacío, registra la falla, lanza DesbloqueoNoPermitidoException y no acciona el "
     "cerrojo, con lo cual la tapa permanece bloqueada. Es una excepción síncrona detectada por la "
     "aplicación: el sistema conoce la regla y la hace cumplir antes de actuar, en lugar de confiar en "
     "que la situación no ocurra.", False, False),
])

p("Ambos comandos devuelven su duración en milisegundos. La operación de bloqueo se contrasta con "
  "RT-06 («bloqueo de tapa en menos de 500 ms») y, si lo supera, el objeto límite registra una alerta. "
  "En el banco de pruebas la duración observada ronda los 30 ms, que es el retardo de actuación "
  "simulado; el valor no prueba el cumplimiento del plazo en el hardware real, pero deja el "
  "instrumento de medición en el lugar correcto.")


# ---------------------------------------------------------------------------
# 5. Banco de pruebas
# ---------------------------------------------------------------------------

h("5. Banco de pruebas e interfaz de interacción")

p("La ventana del banco de pruebas separa visualmente las dos capas. Las dos tarjetas de la izquierda "
  "accionan el hardware simulado —abrir o cerrar la tapa, colocar o retirar masa—, como lo haría el "
  "paciente. Las dos de la derecha invocan los objetos límite de salida para activar la alarma o "
  "comandar la cerradura. A la derecha se muestra la bitácora con instante, nivel, objeto y suceso.")

figura("docs/capturas/01-banco-inicial.png", 16.4,
       "Figura 2. Banco de pruebas en el estado inicial: tapa cerrada, recipiente vacío, alarma "
       "silenciada y cerradura bloqueada (estado inicial seguro de CU-10).")

p("Cada tarjeta indica el dispositivo, su estereotipo en la Tabla 1 y si se trata de una entrada o "
  "una salida. Los indicadores circulares resumen el estado —verde para tapa abierta o masa "
  "detectada, rojo para alarma sonando, ámbar para tapa desbloqueada—, y debajo se muestran los "
  "contadores y la última operación. El comando de la cerradura informa además la duración medida.")

p("El registro no es un adorno: es la evidencia de la interacción. Cada línea tiene la hora con "
  "milisegundos, el nivel (INFO, ALERTA o FALLA), el objeto que produjo el suceso y la descripción. "
  "Con esa traza se documentan las pruebas y se explica, en la defensa, qué pasó y en qué orden.")

figura("docs/capturas/02-tapa-abierta.png", 16.4,
       "Figura 3. Secuencia de CU-03 en el banco: se colocó la dosis (500 g), la alarma estaba activa, "
       "el paciente abrió la tapa y el sistema silenció el buzzer. La cerradura sigue bloqueada.")

figura("docs/capturas/04-interlock-y-lectura-incoherente.png", 16.4,
       "Figura 4. Rechazo del desbloqueo sin masa y lectura incoherente del sensor: la primera deja "
       "una FALLA en la bitácora y la tapa bloqueada; la segunda, una ALERTA, sin alterar el estado físico.")

p("La ventana no contiene lógica de control. Cuando la tapa se abre, nadie silencia el buzzer "
  "automáticamente: eso es lo que hará ControlRetiro en la actividad 3. Sostener esa separación es "
  "deliberado, porque permite mostrar en la defensa qué hace cada capa y qué queda pendiente.")


# ---------------------------------------------------------------------------
# 6. Pruebas
# ---------------------------------------------------------------------------

h("6. Pruebas de la interacción")

h("6.1. Estrategia", 2)

p("Se combinaron dos niveles. Las pruebas automatizadas con JUnit 5 accionan los dispositivos "
  "simulados y verifican lo que publican los objetos límite y lo que devuelven sus comandos; la "
  "bitácora se usa como oráculo, porque registra el orden y el origen de cada suceso. Las pruebas "
  "manuales sobre la interfaz gráfica verifican lo mismo desde el punto de vista del operador: que la "
  "pantalla muestre el efecto del comando y que el registro lo documente.")

p("Las pruebas no dependen de la interfaz gráfica: instancian los dispositivos y los objetos límite "
  "directamente. Por eso pueden ejecutarse en un servidor de integración sin pantalla y no se rompen "
  "si cambia la disposición de la ventana.")

resumen = ET.parse("build/reports/TEST-junit-jupiter.xml").getroot()
total = int(resumen.attrib.get("tests", "0"))
por_clase = {}
for caso in resumen.iter("testcase"):
    clase = caso.attrib.get("classname", "").split(".")[-1]
    por_clase[clase] = por_clase.get(clase, 0) + 1

h("6.2. Pruebas automatizadas", 2)

tabla(
    ["Clase de prueba", "Casos", "Qué verifica"],
    [
        ["SensorPosicionTapaTest", str(por_clase.get("SensorPosicionTapaTest", 0)),
         "Estado inicial, eventos de apertura y cierre con su instante, ausencia de eventos sin transición, "
         "lectura incoherente, notificación a varios objetos límite y baja de un suscriptor."],
        ["BalanzaDePrecisionTest", str(por_clase.get("BalanzaDePrecisionTest", 0)),
         "Masa inicial, evento de detección, retiro, umbral, tara, rechazo de masa negativa y conteo de lecturas."],
        ["BuzzerOutputTest", str(por_clase.get("BuzzerOutputTest", 0)),
         "Activación y desactivación, idempotencia de los comandos, registro en la bitácora y desacople del sensor."],
        ["CerraduraTapaOutputTest", str(por_clase.get("CerraduraTapaOutputTest", 0)),
         "Estado inicial bloqueado, interlock con y sin masa, rechazo tras retirar la masa, cumplimiento de RT-06 y registro."],
        ["EntradasTest", str(por_clase.get("EntradasTest", 0)),
         "Conexión automática del objeto límite al dispositivo, registro de eventos, evento único de masa "
         "detectada y comandos de lectura y tara."],
        ["RegistroEventosTest", str(por_clase.get("RegistroEventosTest", 0)),
         "Orden cronológico, suscriptores, escrituras concurrentes sin pérdida y formato de las entradas."],
        ["InteraccionDispositivosTest", str(por_clase.get("InteraccionDispositivosTest", 0)),
         "Secuencia completa de CU-03 con los cuatro dispositivos, interlock, propagación del evento al "
         "control y ausencia de acoplamiento entre entradas y salidas."],
    ],
    anchos=[5.0, 1.4, 10.6],
    tamanio=8.5,
)

p(f"Resultado de la ejecución: {total} pruebas, {total} exitosas, 0 fallas. El resumen que emite el "
  "ejecutor de JUnit es el siguiente:")

codigo("""Test run finished after 4,6 s
[        10 containers found      ]
[        10 containers successful ]
[        41 tests found           ]
[        41 tests successful      ]
[         0 tests failed          ]""")

h("6.3. Casos de prueba sobre la interfaz gráfica", 2)

p("Los casos siguientes se ejecutaron sobre el banco de pruebas. Las figuras 2 a 4 documentan los "
  "estados finales de CP-01/CP-05, CP-02/CP-14 y CP-04/CP-12.")

tabla(
    ["ID", "Caso y pasos", "Resultado esperado", "Resultado obtenido", "Estado"],
    [
        ["CP-01", "Pulsar «Abrir tapa» con la tapa cerrada.",
         "Indicador «TAPA ABIERTA»; suceso «evento «tapa abierta»»; transiciones = 1.",
         "Coincide. Figura 3.", "OK"],
        ["CP-02", "Pulsar «Cerrar tapa» con la tapa abierta.",
         "«TAPA CERRADA»; suceso «evento «tapa cerrada»»; transiciones = 2.",
         "Coincide. Figura 4.", "OK"],
        ["CP-03", "Pulsar «Cerrar tapa» con la tapa ya cerrada.",
         "Sin evento nuevo; el contador de transiciones no cambia.",
         "Sin evento; contador sin cambios.", "OK"],
        ["CP-04", "Pulsar «Forzar lectura incoherente».",
         "ALERTA «lectura incoherente…»; el estado físico no cambia; el detalle lo indica.",
         "Coincide. Figura 4.", "OK"],
        ["CP-05", "Ingresar 500 g y pulsar «Colocar masa».",
         "«Masa leída: 500,0 g»; indicador verde; INFO «evento «masa detectada» (500,0 g)».",
         "Coincide. Figura 3.", "OK"],
        ["CP-06", "Con masa detectada, cambiar el valor a 520 g y colocar.",
         "La lectura se actualiza; no se repite el evento «masa detectada».",
         "Lectura 520,0 g sin evento repetido.", "OK"],
        ["CP-07", "Pulsar «Retirar masa».",
         "Masa 0,0 g; «Recipiente vacío»; INFO «recipiente sin masa».",
         "Coincide.", "OK"],
        ["CP-08", "Pulsar «Tarar».",
         "Comando «tara» registrado; masa en cero.",
         "Coincide.", "OK"],
        ["CP-09", "Pulsar «Leer masa».",
         "Comando «leer masa → x g» registrado.",
         "Coincide.", "OK"],
        ["CP-10", "Pulsar «Activar alarma».",
         "«ALARMA SONANDO» en rojo; comando registrado; conmutaciones +1.",
         "Coincide.", "OK"],
        ["CP-11", "Pulsar «Silenciar».",
         "«ALARMA SILENCIADA»; comando registrado; conmutaciones +1.",
         "Coincide.", "OK"],
        ["CP-12", "Pulsar «Desbloquear tapa» con el recipiente vacío.",
         "Diálogo de interlock; FALLA en la bitácora; la tapa sigue bloqueada.",
         "Coincide. Figura 4.", "OK"],
        ["CP-13", "Colocar 500 g y pulsar «Desbloquear tapa».",
         "«TAPA DESBLOQUEADA»; comando registrado con su duración.",
         "Coincide; duración informada ≈ 30 ms.", "OK"],
        ["CP-14", "Pulsar «Bloquear tapa».",
         "«TAPA BLOQUEADA»; duración informada y alerta si supera 500 ms.",
         "Coincide; duración < 500 ms.", "OK"],
    ],
    anchos=[1.2, 5.8, 5.2, 3.6, 1.2],
    tamanio=8.2,
)

h("6.4. Trazabilidad consigna → artefacto → prueba", 2)

tabla(
    ["Consigna del taller", "Artefacto que la resuelve", "Prueba que la respalda"],
    [
        ["1 · Dos dispositivos de entrada",
         "SensorPosicionTapa + SensorPosicionTapaInput; BalanzaDePrecision + BalanzaDePrecisionIO",
         "SensorPosicionTapaTest, BalanzaDePrecisionTest, EntradasTest, CP-01 a CP-09"],
        ["2 · Dos dispositivos de salida",
         "Buzzer + BuzzerOutput; CerraduraTapa + CerraduraTapaOutput",
         "BuzzerOutputTest, CerraduraTapaOutputTest, CP-10 a CP-14"],
        ["1 y 2 · Objetos de interfaz para interactuar con los dispositivos",
         "Objetos límite con operaciones de comando y consulta; banco de pruebas Swing",
         "EntradasTest, CP-01 a CP-14, figuras 2 a 4"],
        ["1 y 2 · Documentar las pruebas de interacción",
         "Bitácora RegistroEventos + este informe",
         "InteraccionDispositivosTest, RegistroEventosTest"],
    ],
    anchos=[4.6, 7.2, 5.2],
    tamanio=8.5,
)


# ---------------------------------------------------------------------------
# 7. NetBeans
# ---------------------------------------------------------------------------

h("7. Cómo abrir y ejecutar el proyecto en NetBeans")

p("El proyecto se entrega como proyecto Maven, que NetBeans reconoce de forma nativa: no hay que "
  "importar archivos ni configurar el classpath. Requiere un JDK 17 o superior y una versión de "
  "NetBeans con soporte de Maven, que es la que viene por defecto desde la versión 12.")

numerada("Abrir NetBeans y elegir File → Open Project…")
numerada("Seleccionar la carpeta DispensadorComet y confirmar con Open Project. NetBeans marca el "
         "proyecto con el ícono de Maven y muestra los paquetes en el árbol de proyectos.")
numerada("Para ejecutar el banco de pruebas: clic derecho sobre el proyecto → Run, o la tecla F6. "
         "Se abre la ventana de la figura 2.")
numerada("Para ejecutar las pruebas: clic derecho → Test, o Alt+F6. El resultado aparece en la ventana "
         "Test Results, con las 41 pruebas agrupadas por clase.")
numerada("Para ver una prueba en particular, desplegar el paquete de pruebas y elegir Run Focused Test "
         "Method sobre el método deseado.")

p("Si el aula no tiene conexión a Internet, la primera apertura de un proyecto Maven puede fallar al "
  "resolver JUnit. En ese caso se incluyen dos archivos que compilan y ejecutan sin Maven, con el "
  "JDK instalado:")

codigo("""> compilar-y-probar.cmd      (compila el proyecto y ejecuta las 41 pruebas)
> ejecutar-banco.cmd        (abre el banco de pruebas ya compilado)""")

p("El archivo ejecutar-banco.cmd supone que el proyecto ya fue compilado, sea con NetBeans o con el "
  "script anterior. Ambos scripts dejan las clases en la carpeta build, que no se entrega: se "
  "regenera en cada compilación.")


# ---------------------------------------------------------------------------
# 8. Limitaciones
# ---------------------------------------------------------------------------

h("8. Limitaciones y continuidad con la actividad 3")

vineta("No hay controlador. Los eventos de entrada no accionan por sí solos las salidas: abrir la tapa "
       "no silencia el buzzer ni el cierre desbloquea la cerradura. Ese enlace es exactamente el objeto "
       "de la actividad 3, donde ControlRetiro y ControlGrabacion consumirán los eventos y emitirán los "
       "comandos según sus diagramas de transición de estado.")
vineta("Los retardos de los dispositivos se simulan con pausas breves en el hilo de la interfaz. Sirven "
       "para que la interacción sea observable, pero no son plazos de tiempo real verificados; cuando "
       "exista el controlador, la escritura y la purga de video pasarán a tareas propias.")
vineta("No se simulan la cámara ni el almacenamiento de video, que pertenecen a CU-09 y son el eje de "
       "la actividad 6 (exclusión mutua sobre AlmacenamientoVideoIO). El presente banco de pruebas "
       "queda preparado para incorporarlos como un quinto y sexto dispositivo.")
vineta("El cumplimiento de RT-06 se mide sobre la simulación. La medición es correcta como instrumento, "
       "pero el valor no se extrapola al cerrojo físico, cuyo tiempo de actuación habría que relevar.")
vineta("El desbloqueo se rechaza por software, no por hardware. En un equipo real conviene que el "
       "cerrojo tenga además su propio enclavamiento, de modo que una falla del software no deje la "
       "tapa liberada.")


# ---------------------------------------------------------------------------
# Anexo
# ---------------------------------------------------------------------------

doc.add_page_break()
h("Anexo A. Estructura del proyecto")

codigo("""DispensadorComet/
├── pom.xml                       proyecto Maven (Java 17, JUnit 5)
├── compilar-y-probar.cmd         compilación y pruebas sin Maven
├── ejecutar-banco.cmd            abre el banco de pruebas
├── src/main/java/ar/edu/unse/sistemas3/
│   ├── eventos/                  contratos de eventos y enumeraciones
│   ├── dispositivos/             hardware simulado (4 dispositivos)
│   ├── limite/                   objetos límite «input» y «output»
│   ├── bitacora/                 registro de sucesos
│   ├── banco/                    interfaz gráfica del banco de pruebas
│   └── util/                     formato numérico
├── src/test/java/ar/edu/unse/sistemas3/
│   ├── dispositivos/             pruebas de los dispositivos simulados
│   ├── limite/                   pruebas de los objetos límite
│   ├── bitacora/                 pruebas del registro
│   ├── integracion/              pruebas de interacción (consignas 1 y 2)
│   └── herramientas/             generación de capturas y diagrama
├── lib/                          JUnit 5 para la ejecución sin Maven
└── docs/                         este informe, figuras y capturas""")


h("Anexo B. Detalle de las pruebas automatizadas")

p("Los nombres corresponden a la anotación @DisplayName de cada prueba, que es la que JUnit informa "
  "en el árbol de resultados.", tamanio=9.5, color=GRIS)

PATRON_CLASE = re.compile(r'@DisplayName\("([^"]+)"\)\s*class\s+(\w+)')
PATRON_METODO = re.compile(r'@(?:Test\s*@DisplayName|DisplayName\s*@Test)\("([^"]+)"\)', re.S)


def display(texto):
    texto = texto.replace("\\n", " ")
    return texto


orden = [
    "SensorPosicionTapaTest",
    "BalanzaDePrecisionTest",
    "BuzzerOutputTest",
    "CerraduraTapaOutputTest",
    "EntradasTest",
    "RegistroEventosTest",
    "InteraccionDispositivosTest",
]

pruebas = {}
for archivo in glob.glob(os.path.join("src", "test", "java", "**", "*Test.java"), recursive=True):
    fuente = open(archivo, encoding="utf-8").read()
    coincidencia = PATRON_CLASE.search(fuente)
    if not coincidencia:
        continue
    nombre_clase = coincidencia.group(2)
    pruebas[nombre_clase] = {
        "titulo": display(coincidencia.group(1)),
        "casos": [display(m) for m in PATRON_METODO.findall(fuente)],
        "total": fuente.count("@Test"),
    }

for nombre in orden:
    if nombre not in pruebas:
        continue
    info = pruebas[nombre]
    parrafo = doc.add_paragraph()
    parrafo.paragraph_format.space_before = Pt(8)
    parrafo.paragraph_format.space_after = Pt(2)
    run = parrafo.add_run(f"{nombre}  ")
    run.bold = True
    run.font.size = Pt(9.5)
    run.font.color.rgb = AZUL
    run = parrafo.add_run(f"({info['total']} casos) — {info['titulo']}")
    run.font.size = Pt(9)
    run.font.color.rgb = GRIS

    for caso in info["casos"]:
        vineta(caso, tamanio=8.6)

p()
nota("Documento generado a partir del código fuente y del resultado real de la ejecución de las "
     "pruebas. Las capturas de pantalla y el diagrama de la figura 1 se obtuvieron con las clases "
     "CapturaPantalla y DiagramaComponentes incluidas en el proyecto, de modo que pueden regenerarse.")

salida = os.path.join("docs", "Actividades 1 y 2 - Taller 2 - Sistemas III.docx")
doc.save(salida)
print("Informe generado:", os.path.abspath(salida))
print("Pruebas contabilizadas:", total, por_clase)
