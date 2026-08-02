package com.booksaw.betterTeams.luniatic.duelo;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Un duelo pactado en curso entre dos bandos.
 *
 * <p>Cada bando es el clan que pacto mas sus aliados del momento en que arranco.
 * Los aliados pelean pero <b>no son parte de la apuesta</b>: el pozo lo ponen y se
 * lo llevan los dos clanes principales.
 *
 * <p><b>Cuenta las bajas, y eso NO contradice la regla de no tener un marcador de
 * dominancia.</b> La primera version no las contaba y quedaba sin forma de
 * resolverse: rendirse costaba el pozo y aguantar el reloj lo devolvia, asi que la
 * jugada optima era siempre esconderse. Lo toxico es el <b>registro publico y
 * permanente</b>, no medir un combate puntual: este contador es privado, se borra
 * cuando el duelo termina y no toca el puntaje del clan.
 *
 * <p><b>Solo cuentan las caidas de los dos clanes principales.</b> Si contaran las
 * de los aliados, sumar aliados te haria mas facil perder —mas cuerpos que pueden
 * caer— y abriria la puerta a que un aliado tire el duelo a proposito. Un aliado
 * que mata a un principal si suma: se gana bajando al clan que acepto el duelo.
 */
public class Duelo {

	private final UUID clanA;
	private final UUID clanB;
	/** Clan principal mas sus aliados, congelado al arrancar. */
	private final Set<UUID> bandoA;
	private final Set<UUID> bandoB;
	/** Lo que aporto cada clan principal. El pozo total es el doble. */
	private final double apuesta;
	private final long finMillis;
	/** Bajas de cada clan principal. Vive y muere con el duelo. */
	private int bajasA;
	private int bajasB;

	public Duelo(UUID clanA, Set<UUID> aliadosA, UUID clanB, Set<UUID> aliadosB,
			double apuesta, long finMillis) {
		this.clanA = clanA;
		this.clanB = clanB;
		this.apuesta = apuesta;
		this.finMillis = finMillis;

		this.bandoA = new LinkedHashSet<>();
		this.bandoA.add(clanA);
		this.bandoA.addAll(aliadosA);

		this.bandoB = new LinkedHashSet<>();
		this.bandoB.add(clanB);
		this.bandoB.addAll(aliadosB);

		// Un clan aliado de los dos no puede pelear contra si mismo: se queda afuera.
		Set<UUID> enLosDos = new HashSet<>(bandoA);
		enLosDos.retainAll(bandoB);
		enLosDos.remove(clanA);
		enLosDos.remove(clanB);
		bandoA.removeAll(enLosDos);
		bandoB.removeAll(enLosDos);
	}

	public UUID getClanA() {
		return clanA;
	}

	public UUID getClanB() {
		return clanB;
	}

	public double getApuesta() {
		return apuesta;
	}

	public double getPozo() {
		return apuesta * 2;
	}

	public long getFinMillis() {
		return finMillis;
	}

	public boolean vencio(long ahora) {
		return ahora >= finMillis;
	}

	public boolean participa(UUID clan) {
		return bandoA.contains(clan) || bandoB.contains(clan);
	}

	/** Si ese clan es uno de los dos que pactaron, no un aliado arrastrado. */
	public boolean esPrincipal(UUID clan) {
		return clanA.equals(clan) || clanB.equals(clan);
	}

	public Set<UUID> getBando(UUID clan) {
		if (bandoA.contains(clan)) {
			return Collections.unmodifiableSet(bandoA);
		}
		if (bandoB.contains(clan)) {
			return Collections.unmodifiableSet(bandoB);
		}
		return Collections.emptySet();
	}

	/** Los del otro bando, o vacio si el clan no participa. */
	public Set<UUID> getBandoRival(UUID clan) {
		if (bandoA.contains(clan)) {
			return Collections.unmodifiableSet(bandoB);
		}
		if (bandoB.contains(clan)) {
			return Collections.unmodifiableSet(bandoA);
		}
		return Collections.emptySet();
	}

	/** El clan que pacto del lado de este, sea el mismo o el que lo arrastro. */
	public UUID getPrincipalDe(UUID clan) {
		if (bandoA.contains(clan)) {
			return clanA;
		}
		if (bandoB.contains(clan)) {
			return clanB;
		}
		return null;
	}

	/** El clan que pacto del otro lado. */
	public UUID rivalDe(UUID clan) {
		if (bandoA.contains(clan)) {
			return clanB;
		}
		if (bandoB.contains(clan)) {
			return clanA;
		}
		return null;
	}

	public Set<UUID> todosLosClanes() {
		Set<UUID> todos = new LinkedHashSet<>(bandoA);
		todos.addAll(bandoB);
		return todos;
	}

	/** Suma una baja al clan principal que perdio al jugador. Devuelve el total. */
	public int sumarBaja(UUID clanCaido) {
		if (clanA.equals(clanCaido)) {
			return ++bajasA;
		}
		if (clanB.equals(clanCaido)) {
			return ++bajasB;
		}
		return 0;
	}

	public int getBajas(UUID clan) {
		if (bandoA.contains(clan)) {
			return bajasA;
		}
		if (bandoB.contains(clan)) {
			return bajasB;
		}
		return 0;
	}

	/** Quien va ganando, o null si estan iguales. Gana el que MENOS bajas tiene. */
	public UUID getGanandoAhora() {
		if (bajasA == bajasB) {
			return null;
		}
		return bajasA < bajasB ? clanA : clanB;
	}

	public long segundosRestantes(long ahora) {
		return Math.max(0, (finMillis - ahora) / 1000);
	}
}
