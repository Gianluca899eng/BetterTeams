package com.booksaw.betterTeams.luniatic.duelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que cada duelo se procese una sola vez.
 *
 * <p>Existe por un defecto real: el mapa de duelos en curso guarda el mismo objeto
 * bajo la clave de cada clan participante, asi que recorrer sus valores lo devolvia
 * dos veces o mas. Cada vuelta de mas pagaba la apuesta otra vez, o sea que apagar
 * el servidor imprimia plata.
 */
@DisplayName("Duelos unicos al recorrer el mapa")
class DueloUnicosTest {

	private Duelo duelo(double apuesta) {
		return new Duelo(UUID.randomUUID(), Collections.emptySet(),
				UUID.randomUUID(), Collections.emptySet(), apuesta, 0L);
	}

	@Test
	@DisplayName("el mismo duelo repetido se procesa una sola vez")
	void unDueloRepetidoSeCuentaUnaVez() {
		Duelo unico = duelo(100);
		// Asi queda el mapa: una entrada por clan, todas apuntando al mismo duelo.
		List<Duelo> comoEstaEnElMapa = List.of(unico, unico);

		assertThat(DueloManager.unicos(comoEstaEnElMapa)).hasSize(1);
	}

	@Test
	@DisplayName("con aliados el duelo aparece muchas veces y sigue siendo uno")
	void conAliadosSigueSiendoUno() {
		Duelo unico = duelo(500);
		List<Duelo> conCuatroAliados = List.of(unico, unico, unico, unico, unico, unico);

		assertThat(DueloManager.unicos(conCuatroAliados)).hasSize(1);
	}

	@Test
	@DisplayName("duelos distintos no se pisan entre si")
	void duelosDistintosSobreviven() {
		Duelo uno = duelo(100);
		Duelo otro = duelo(100);
		List<Duelo> mapa = List.of(uno, uno, otro, otro);

		Collection<Duelo> resultado = DueloManager.unicos(mapa);

		assertThat(resultado).hasSize(2);
		assertThat(resultado).containsExactlyInAnyOrder(uno, otro);
	}

	@Test
	@DisplayName("dos duelos con la misma apuesta siguen siendo dos")
	void mismaApuestaNoLosFusiona() {
		// Duelo no implementa equals: la deduplicacion tiene que ser por identidad,
		// o dos duelos iguales en valores se comerian el uno al otro.
		Set<Duelo> resultado = new LinkedHashSet<>(DueloManager.unicos(
				new ArrayList<>(List.of(duelo(250), duelo(250)))));

		assertThat(resultado).hasSize(2);
	}

	@Test
	@DisplayName("una lista vacia no explota")
	void listaVacia() {
		assertThat(DueloManager.unicos(Collections.emptyList())).isEmpty();
	}
}
