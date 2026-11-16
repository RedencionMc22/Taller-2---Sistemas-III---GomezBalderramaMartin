package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.BalanzaDePrecision;
import ar.edu.unse.sistemas3.dispositivos.CerraduraTapa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CerraduraTapaOutput: objeto límite del dispositivo de salida (actividad 2)")
class CerraduraTapaOutputTest {

    private CerraduraTapa cerradura;
    private BalanzaDePrecision balanza;
    private BalanzaDePrecisionIO balanzaIO;
    private CerraduraTapaOutput salida;
    private RegistroEventos registro;

    @BeforeEach
    void preparar() {
        cerradura = new CerraduraTapa();
        balanza = new BalanzaDePrecision();
        registro = new RegistroEventos();
        balanzaIO = new BalanzaDePrecisionIO(balanza, registro);
        salida = new CerraduraTapaOutput(cerradura, balanzaIO, registro);
    }

    @Test
    @DisplayName("La cerradura arranca bloqueada: estado inicial seguro de CU-10")
    void arrancaBloqueada() {
        assertTrue(salida.estaBloqueada());
    }

    @Test
    @DisplayName("El comando «bloquear» deja la tapa bloqueada")
    void bloquearDejaLaTapaBloqueada() {
        salida.bloquear();

        assertTrue(cerradura.estaBloqueada());
        assertTrue(salida.estaBloqueada());
    }

    @Test
    @DisplayName("El comando «desbloquear» exige masa detectada: si no hay masa, falla")
    void desbloquearSinMasaLanzaExcepcion() {
        DesbloqueoNoPermitidoException excepcion =
                assertThrows(DesbloqueoNoPermitidoException.class, () -> salida.desbloquear());

        assertTrue(excepcion.getMessage().contains("masa detectada"));
        assertTrue(salida.estaBloqueada(), "la tapa debe seguir bloqueada");
        assertEquals(1, registro.cantidad(RegistroEventos.Nivel.FALLA));
    }

    @Test
    @DisplayName("Con masa detectada el comando «desbloquear» se ejecuta")
    void desbloquearConMasaFunciona() throws Exception {
        balanza.colocarMasa(500);

        long duracion = salida.desbloquear();

        assertFalse(cerradura.estaBloqueada());
        assertTrue(duracion <= CerraduraTapaOutput.LIMITE_RT06_MS);
    }

    @Test
    @DisplayName("Si la masa se retira, el desbloqueo vuelve a rechazarse")
    void desbloqueoRechazadoTrasRetirarLaMasa() {
        balanza.colocarMasa(500);

        balanza.retirarMasa();

        assertThrows(DesbloqueoNoPermitidoException.class, () -> salida.desbloquear());
        assertTrue(salida.estaBloqueada());
    }

    @Test
    @DisplayName("El bloqueo cumple RT-06: menos de 500 ms")
    void bloqueoCumpleRt06() {
        long duracion = salida.bloquear();

        assertTrue(duracion < CerraduraTapaOutput.LIMITE_RT06_MS,
                "el bloqueo tardó " + duracion + " ms y el límite es " + CerraduraTapaOutput.LIMITE_RT06_MS + " ms");
        assertTrue(registro.entradas().get(0).mensaje().contains("RT-06"));
    }

    @Test
    @DisplayName("Los comandos quedan registrados con su duración")
    void registraComandosConDuracion() throws Exception {
        balanza.colocarMasa(500);

        salida.desbloquear();
        salida.bloquear();

        java.util.List<RegistroEventos.Entrada> comandos = registro.entradas().stream()
                .filter(entrada -> CerraduraTapaOutput.ORIGEN.equals(entrada.origen()))
                .toList();
        assertEquals(2, comandos.size());
        assertTrue(comandos.get(0).mensaje().contains("desbloquear"));
        assertTrue(comandos.get(0).mensaje().contains("ms"));
        assertTrue(comandos.get(1).mensaje().contains("bloquear"));
    }
}
