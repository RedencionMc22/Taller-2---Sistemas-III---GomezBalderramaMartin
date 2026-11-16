package ar.edu.unse.sistemas3.integracion;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.BalanzaDePrecision;
import ar.edu.unse.sistemas3.dispositivos.Buzzer;
import ar.edu.unse.sistemas3.dispositivos.CerraduraTapa;
import ar.edu.unse.sistemas3.dispositivos.SensorPosicionTapa;
import ar.edu.unse.sistemas3.eventos.EstadoTapa;
import ar.edu.unse.sistemas3.limite.BalanzaDePrecisionIO;
import ar.edu.unse.sistemas3.limite.BuzzerOutput;
import ar.edu.unse.sistemas3.limite.CerraduraTapaOutput;
import ar.edu.unse.sistemas3.limite.DesbloqueoNoPermitidoException;
import ar.edu.unse.sistemas3.limite.SensorPosicionTapaInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de interacción entre los dispositivos simulados y los objetos límite.
 *
 * <p>Cubren las consignas 1 y 2 del taller: los cuatro dispositivos del
 * subsistema «Retiro y evidencia» conectados a sus objetos de interfaz,
 * ejecutando la secuencia de CU-03 y sus verificaciones de seguridad.</p>
 */
@DisplayName("Interacción dispositivos ↔ objetos límite (consignas 1 y 2)")
class InteraccionDispositivosTest {

    private RegistroEventos registro;
    private SensorPosicionTapa sensor;
    private BalanzaDePrecision balanza;
    private Buzzer buzzer;
    private CerraduraTapa cerradura;

    private SensorPosicionTapaInput sensorInput;
    private BalanzaDePrecisionIO balanzaIO;
    private BuzzerOutput buzzerOutput;
    private CerraduraTapaOutput cerraduraOutput;

    @BeforeEach
    void prepararBancoDePruebas() {
        registro = new RegistroEventos();
        sensor = new SensorPosicionTapa();
        balanza = new BalanzaDePrecision();
        buzzer = new Buzzer();
        cerradura = new CerraduraTapa();

        sensorInput = new SensorPosicionTapaInput(sensor, registro);
        balanzaIO = new BalanzaDePrecisionIO(balanza, registro);
        buzzerOutput = new BuzzerOutput(buzzer, registro);
        cerraduraOutput = new CerraduraTapaOutput(cerradura, balanzaIO, registro);
    }

    @Test
    @DisplayName("Secuencia de retiro de CU-03 con los cuatro dispositivos simulados")
    void secuenciaDeRetiroCompleta() throws Exception {
        List<EstadoTapa> eventosTapa = new ArrayList<>();
        sensorInput.agregarEscucha(evento -> eventosTapa.add(evento.estado()));

        // Condición inicial del banco: dosis dispensada, tapa bloqueada y alarma activa.
        balanza.colocarMasa(500);
        buzzerOutput.activar();
        assertTrue(buzzer.estaSonando());
        assertTrue(cerraduraOutput.estaBloqueada());

        // CU-03 paso 2: el paciente levanta la tapa; el sistema silencia el buzzer.
        sensor.abrir();
        buzzerOutput.desactivar();
        assertEquals(List.of(EstadoTapa.ABIERTA), eventosTapa);
        assertFalse(buzzer.estaSonando());
        assertTrue(cerraduraOutput.estaBloqueada(), "la tapa no se desbloquea al abrirla");

        // CU-03 pasos 4 a 6: el paciente retira la dosis y cierra la tapa.
        sensor.cerrar();
        balanza.retirarMasa();
        assertEquals(0.0, balanzaIO.leerMasa());
        assertEquals(List.of(EstadoTapa.ABIERTA, EstadoTapa.CERRADA), eventosTapa);

        // CU-03 paso 7: se bloquea la tapa dentro de RT-06.
        long duracion = cerraduraOutput.bloquear();
        assertTrue(duracion <= CerraduraTapaOutput.LIMITE_RT06_MS);
        assertTrue(cerraduraOutput.estaBloqueada());

        // La bitácora refleja toda la interacción, de los dos lados del límite.
        String texto = registro.comoTexto();
        assertTrue(texto.contains("SensorPosicionTapaInput"));
        assertTrue(texto.contains("BalanzaDePrecisionIO"));
        assertTrue(texto.contains("BuzzerOutput"));
        assertTrue(texto.contains("CerraduraTapaOutput"));
        assertEquals(0, registro.cantidad(RegistroEventos.Nivel.FALLA));
    }

    @Test
    @DisplayName("La tapa nunca se desbloquea con el recipiente vacío ni con masa detectada tarde")
    void interlockDelDesbloqueo() throws Exception {
        // Recipiente vacío: el comando debe rechazarse.
        assertThrows(DesbloqueoNoPermitidoException.class, () -> cerraduraOutput.desbloquear());
        assertTrue(cerradura.estaBloqueada());

        // Llega la dosis: ahora sí se puede desbloquear.
        balanza.colocarMasa(500);
        cerraduraOutput.desbloquear();
        assertFalse(cerradura.estaBloqueada());

        // El paciente retira la dosis y la tapa queda abierta: no se re-bloquea sola.
        balanza.retirarMasa();
        assertFalse(cerradura.estaBloqueada());
    }

    @Test
    @DisplayName("El evento de tapa llega al objeto límite y desde allí a los objetos de control")
    void elEventoLlegaAlControl() {
        List<EstadoTapa> recibidosPorElControl = new ArrayList<>();
        sensorInput.agregarEscucha(evento -> recibidosPorElControl.add(evento.estado()));

        sensor.abrir();
        sensor.cerrar();

        assertEquals(List.of(EstadoTapa.ABIERTA, EstadoTapa.CERRADA), recibidosPorElControl);
        assertEquals(2, registro.cantidad(RegistroEventos.Nivel.INFO));
    }

    @Test
    @DisplayName("Una lectura incoherente del sensor no altera el estado del sistema")
    void lecturaIncoherenteNoAlteraElEstado() {
        sensor.inyectarLecturaIncoherente(EstadoTapa.ABIERTA);

        assertEquals(EstadoTapa.CERRADA, sensorInput.estadoActual());
        assertEquals(1, registro.cantidad(RegistroEventos.Nivel.ALERTA));
        assertFalse(sensorInput.ultimoEvento().esCoherente());
    }

    @Test
    @DisplayName("El buzzer y la cerradura responden sólo a los comandos del objeto límite")
    void losDispositivosDeSalidaNoSeAccionanSolos() {
        assertFalse(buzzer.estaSonando());
        assertTrue(cerradura.estaBloqueada());

        // Un evento de entrada no acciona por sí solo ninguna salida.
        sensor.abrir();
        balanza.colocarMasa(500);

        assertFalse(buzzer.estaSonando());
        assertTrue(cerradura.estaBloqueada());
    }
}
