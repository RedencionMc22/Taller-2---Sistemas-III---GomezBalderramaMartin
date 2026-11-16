package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.BalanzaDePrecision;
import ar.edu.unse.sistemas3.dispositivos.SensorPosicionTapa;
import ar.edu.unse.sistemas3.eventos.EstadoTapa;
import ar.edu.unse.sistemas3.eventos.EventoTapa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Objetos límite de entrada: SensorPosicionTapaInput y BalanzaDePrecisionIO (actividad 1)")
class EntradasTest {

    private RegistroEventos registro;
    private SensorPosicionTapa sensor;
    private SensorPosicionTapaInput sensorInput;
    private BalanzaDePrecision balanza;
    private BalanzaDePrecisionIO balanzaIO;

    @BeforeEach
    void preparar() {
        registro = new RegistroEventos();
        sensor = new SensorPosicionTapa();
        sensorInput = new SensorPosicionTapaInput(sensor, registro);
        balanza = new BalanzaDePrecision();
        balanzaIO = new BalanzaDePrecisionIO(balanza, registro);
    }

    @Test
    @DisplayName("El objeto límite se conecta al dispositivo al construirse")
    void seConectaAlDispositivo() {
        List<EventoTapa> recibidos = new ArrayList<>();
        sensorInput.agregarEscucha(recibidos::add);

        sensor.abrir();

        assertEquals(1, recibidos.size());
        assertEquals(EstadoTapa.ABIERTA, recibidos.get(0).estado());
    }

    @Test
    @DisplayName("Registra el evento recibido con el nombre del objeto límite")
    void registraElEventoRecibido() {
        sensor.abrir();

        assertEquals(1, registro.cantidad());
        assertEquals(SensorPosicionTapaInput.ORIGEN, registro.entradas().get(0).origen());
        assertTrue(registro.entradas().get(0).mensaje().contains("tapa abierta"));
        assertNotNull(sensorInput.ultimoEvento());
    }

    @Test
    @DisplayName("La lectura incoherente se registra como alerta y se propaga marcada")
    void lecturaIncoherenteSeRegistraComoAlerta() {
        List<EventoTapa> recibidos = new ArrayList<>();
        sensorInput.agregarEscucha(recibidos::add);

        sensor.inyectarLecturaIncoherente(EstadoTapa.ABIERTA);

        assertEquals(1, registro.cantidad(RegistroEventos.Nivel.ALERTA));
        assertTrue(registro.entradas().get(0).mensaje().contains("incoherente"));
        assertFalse(recibidos.get(0).esCoherente());
    }

    @Test
    @DisplayName("BalanzaDePrecisionIO informa masa detectada una sola vez por detección")
    void informaMasaDetectadaUnaSolaVez() {
        List<ar.edu.unse.sistemas3.eventos.EventoMasa> avisos = new ArrayList<>();
        balanzaIO.agregarEscucha(avisos::add);

        balanza.colocarMasa(500);
        balanza.colocarMasa(520);
        balanza.colocarMasa(480);
        assertEquals(1, avisos.size(), "mientras siga habiendo masa no se repite el evento");

        balanza.retirarMasa();
        assertEquals(2, avisos.size(), "al vaciarse el recipiente se informa el cambio");
        assertFalse(avisos.get(1).detectaMasa());
    }

    @Test
    @DisplayName("BalanzaDePrecisionIO ejecuta los comandos de lectura y de tara")
    void ejecutaComandosDeLecturaYTara() {
        balanza.colocarMasa(300);

        assertEquals(300.0, balanzaIO.leerMasa());
        balanzaIO.tarar();

        assertEquals(0.0, balanzaIO.masaActual());
        assertFalse(balanzaIO.detectaMasa());
        assertTrue(registro.comoTexto().contains("comando «tara»"));
        assertTrue(registro.comoTexto().contains("comando «leer masa»"));
    }
}
