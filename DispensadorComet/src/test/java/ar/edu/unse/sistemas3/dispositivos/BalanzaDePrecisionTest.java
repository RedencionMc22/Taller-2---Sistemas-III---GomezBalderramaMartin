package ar.edu.unse.sistemas3.dispositivos;

import ar.edu.unse.sistemas3.eventos.EventoMasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BalanzaDePrecision: dispositivo de entrada/salida simulado (actividad 1)")
class BalanzaDePrecisionTest {

    @Test
    @DisplayName("Arranca en cero y sin masa detectada")
    void arrancaEnCero() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();

        assertEquals(0.0, balanza.leerMasa());
        assertFalse(balanza.detectaMasa());
    }

    @Test
    @DisplayName("Colocar masa informa la medida y el evento «masa detectada»")
    void colocarMasaInformaMasaDetectada() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();
        List<EventoMasa> eventos = new ArrayList<>();
        balanza.agregarEscucha(eventos::add);

        balanza.colocarMasa(500);

        assertEquals(500.0, balanza.leerMasa());
        assertTrue(balanza.detectaMasa());
        assertEquals(1, eventos.size());
        assertEquals(500.0, eventos.get(0).masaGramos());
        assertTrue(eventos.get(0).detectaMasa());
    }

    @Test
    @DisplayName("Retirar la masa deja el recipiente vacío")
    void retirarMasaDejaRecipienteVacio() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();
        balanza.colocarMasa(500);

        balanza.retirarMasa();

        assertEquals(0.0, balanza.leerMasa());
        assertFalse(balanza.detectaMasa());
    }

    @Test
    @DisplayName("Por debajo del umbral no se considera masa detectada")
    void porDebajoDelUmbralNoDetecta() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();

        balanza.colocarMasa(BalanzaDePrecision.UMBRAL_MASA_G / 2);

        assertFalse(balanza.detectaMasa());
    }

    @Test
    @DisplayName("Tarar deja la balanza en cero y cuenta la operación")
    void tararDejaLaBalanzaEnCero() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();
        balanza.colocarMasa(250);

        balanza.tarar();

        assertEquals(0.0, balanza.leerMasa());
        assertEquals(1, balanza.taras());
    }

    @Test
    @DisplayName("Rechaza una masa negativa")
    void rechazaMasaNegativa() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();

        assertThrows(IllegalArgumentException.class, () -> balanza.colocarMasa(-10));
    }

    @Test
    @DisplayName("Cada lectura de masa se contabiliza como comando")
    void contabilizaLecturas() {
        BalanzaDePrecision balanza = new BalanzaDePrecision();

        balanza.leerMasa();
        balanza.leerMasa();

        assertEquals(2, balanza.lecturas());
    }
}
