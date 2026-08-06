package com.booksaw.betterTeams.luniatic.duelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	@Test
	@DisplayName("sufijo de escala: 30k, 2m, 1b")
	void sufijoDeEscala() {
		assertThat(parsear("30k")).isEqualTo(30_000.0);
		assertThat(parsear("2m")).isEqualTo(2_000_000.0);
		assertThat(parsear("1b")).isEqualTo(1_000_000_000.0);
	}

	@Test
	@DisplayName("el sufijo no distingue mayusculas ni le molesta un espacio")
	void sufijoTolerante() {
		assertThat(parsear("30K")).isEqualTo(30_000.0);
		assertThat(parsear("2M")).isEqualTo(2_000_000.0);
		assertThat(parsear("30 k")).isEqualTo(30_000.0);
	}

	@Test
	@DisplayName("el sufijo se lleva bien con el decimal de aca")
	void sufijoConDecimal() {
		assertThat(parsear("1,5k")).isEqualTo(1500.0);
		assertThat(parsear("1.5k")).isEqualTo(1500.0);
		assertThat(parsear("0,1k")).isEqualTo(100.0);
		assertThat(parsear("2,25m")).isEqualTo(2_250_000.0);
	}

	@Test
	@DisplayName("un monto grande no sale en notacion cientifica")
	void sinNotacionCientifica() {
		// toPlainString: si saliera "1.0E+9" BigDecimal lo aceptaria igual, pero
		// cualquier mensaje que le muestre el monto al jugador quedaria ilegible.
		assertThat(DueloCommand.normalizarMonto("1b")).doesNotContain("E");
	}

	@Test
	@DisplayName("lo que no es un monto sigue siendo invalido")
	void basuraSigueSiendoInvalida() {
		// El contrato es que devuelve una cadena y que el BigDecimal del llamador tira.
		assertThatThrownBy(() -> parsear("k")).isInstanceOf(NumberFormatException.class);
		assertThatThrownBy(() -> parsear("30kk")).isInstanceOf(NumberFormatException.class);
		assertThatThrownBy(() -> parsear("treinta")).isInstanceOf(NumberFormatException.class);
	}
}
