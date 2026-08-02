package com.booksaw.betterTeams.luniatic.duelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El monto de la apuesta, escrito como se escribe aca.
 *
 * <p>Existe por un defecto real: el mensaje que explicaba como aceptar un duelo
 * formateaba el monto con el separador del sistema —{@code 0,00} en espaniol— y el
 * comando lo rechazaba. Le decia al jugador que escribiera algo imposible.
 */
@DisplayName("Monto de la apuesta")
class MontoDuelloTest {

	private double parsear(String texto) {
		return new BigDecimal(DueloCommand.normalizarMonto(texto)).doubleValue();
	}

	@Test
	@DisplayName("con coma decimal, que es como se escribe aca")
	void comaDecimal() {
		assertThat(parsear("0,00")).isEqualTo(0.0);
		assertThat(parsear("100,50")).isEqualTo(100.5);
	}

	@Test
	@DisplayName("con punto decimal, que es lo que espera BigDecimal")
	void puntoDecimal() {
		assertThat(parsear("100.50")).isEqualTo(100.5);
	}

	@Test
	@DisplayName("el punto de miles no se come el numero")
	void puntoDeMiles() {
		assertThat(parsear("1.000,50")).isEqualTo(1000.5);
	}

	@Test
	@DisplayName("un entero pelado no cambia")
	void enteroPelado() {
		assertThat(parsear("1000")).isEqualTo(1000.0);
		assertThat(parsear(" 250 ")).isEqualTo(250.0);
	}
}
