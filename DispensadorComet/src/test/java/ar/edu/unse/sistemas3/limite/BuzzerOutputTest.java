package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.Buzzer;
import ar.edu.unse.sistemas3.dispositivos.SensorPosicionTapa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuzzerOutput: objeto límite del dispositivo de salida (actividad 2)")
class BuzzerOutputTest {

    private Buzzer buzzer;
    private BuzzerOutput salida;
    private RegistroEventos registro;

    @BeforeEach
    void preparar() {
        buzzer = new Buzzer();
        registro = new RegistroEventos();
        salida = new BuzzerOutput(buzzer, registro);
    }

    @Test
    @DisplayName("El comando «activar alarma sonora» enciende el dispositivo")
    void activarEnciendeElBuzzer() {
        salida.activar();

        assertTrue(buzzer.estaSonando());
        assertTrue(salida.estaActivo());
        assertEquals(1, salida.conmutaciones());
    }

    @Test
    @DisplayName("El comando «desactivar alarma sonora» apaga el dispositivo")
    void desactivarApagaElBuzzer() {
        salida.activar();

        salida.desactivar();

        assertFalse(buzzer.estaSonando());
        assertFalse(salida.estaActivo());
        assertEquals(2, salida.conmutaciones());
    }

    @Test
    @DisplayName("Repetir el mismo comando no produce una nueva conmutación")
    void comandoRepetidoNoConmuta() {
        salida.activar();
        salida.activar();

        assertEquals(1, salida.conmutaciones());
        assertTrue(salida.estaActivo());
    }

    @Test
    @DisplayName("Cada comando queda registrado en la bitácora con su origen")
    void registraLosComandos() {
        salida.activar();
        salida.desactivar();

        assertEquals(2, registro.cantidad());
        assertEquals(BuzzerOutput.ORIGEN, registro.entradas().get(0).origen());
        assertTrue(registro.entradas().get(0).mensaje().contains("activar alarma sonora"));
        assertTrue(registro.entradas().get(1).mensaje().contains("desactivar alarma sonora"));
    }

    @Test
    @DisplayName("El objeto límite no depende del sensor de tapa para operar")
    void operaSinElSensorDeTapa() {
        // El control decide cuándo activar; el objeto límite sólo ejecuta el comando.
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        SensorPosicionTapaInput entrada = new SensorPosicionTapaInput(sensor, registro);

        salida.activar();

        assertTrue(entrada.ultimoEvento() == null);
        assertTrue(salida.estaActivo());
    }
}
