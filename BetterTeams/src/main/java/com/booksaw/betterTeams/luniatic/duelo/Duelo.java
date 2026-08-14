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
	/**
	 * Cuanto se pacto que durara. Va en el duelo y no en el manager porque cada duelo
	 * elige su duracion: sin esto la barra de progreso no sabria contra que medir.
	 */
	private final long duracionMillis;
	/**
	 * Caidas que hacen falta para ganar, pactadas junto con la duracion.
	 *
	 * <p>Escala con el preset a proposito: con un objetivo fijo, un duelo de siete dias
	 * se resolveria en la primera escaramuza y elegir la duracion no significaria nada.
	 */
	private final int objetivoBajas;
	/** Bajas de cada clan principal. Vive y muere con el duelo. */
	private int bajasA;
	private int bajasB;
	/**
	 * Los jugadores que entran a este duelo, <b>congelados al arrancar</b>.
	 *
	 * <p>Sale de la preferencia de cada uno ({@link DueloPreferencias}), pero se guarda
	 * aca y no se vuelve a consultar: si se leyera en vivo, bastaria con bajarse cuando te
	 * estan por matar. Por el mismo motivo, quien entra al clan con el duelo empezada
	 * tampoco queda adentro.
	 *
	 * <p>Mientras no se congelen, participan todos: es como se levanta un duelo guardado
	 * por una version anterior a esta lista. <b>La marca es el booleano, no que la lista
	 * este vacia</b> — un duelo donde todos se bajaron tiene lista vacia y no significa
	 * "todos".
	 */
	private final Set<UUID> participantes = new HashSet<>();
	private boolean participantesCongelados;

	public Duelo(UUID clanA, Set<UUID> aliadosA, UUID clanB, Set<UUID> aliadosB,
			double apuesta, long finMillis, long duracionMillis, int objetivoBajas) {
		this.clanA = clanA;
		this.clanB = clanB;
		this.apuesta = apuesta;
		this.finMillis = finMillis;
		this.duracionMillis = duracionMillis;
		this.objetivoBajas = objetivoBajas;

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

	public long getDuracionMillis() {
		return duracionMillis;
	}

	public int getObjetivoBajas() {
		return objetivoBajas;
	}

	/**
	 * Repone el marcador al levantar un duelo guardado.
	 *
	 * <p>Sin esto, un reinicio a mitad de duelo devolveria el marcador a cero y el que
	 * iba ganando perderia su ventaja: seria peor que cancelarlo.
	 */
	public void reponerBajas(int bajasA, int bajasB) {
		this.bajasA = Math.max(0, bajasA);
		this.bajasB = Math.max(0, bajasB);
	}

	public int getBajasA() {
		return bajasA;
	}

	public int getBajasB() {
		return bajasB;
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

	/**
	 * Congela quienes entran a este duelo. Se llama una sola vez, al arrancar el duelo o
	 * al levantarlo del archivo.
	 */
	public void congelarParticipantes(Set<UUID> jugadores) {
		participantes.clear();
		if (jugadores != null) {
			participantes.addAll(jugadores);
		}
		participantesCongelados = true;
	}

	/**
	 * Si ese jugador entro a este duelo.
	 *
	 * <p>Sin congelar contesta que si a todos, que es como se leen los duelos guardados por
	 * una version anterior a esta lista.
	 */
	public boolean esParticipante(UUID jugador) {
		return !participantesCongelados || participantes.contains(jugador);
	}

	public boolean tieneParticipantesCongelados() {
		return participantesCongelados;
	}

	/**
	 * Suma a alguien al duelo ya empezada. <b>Solo lo usa el staff</b>.
	 *
	 * <p>Existe para el caso real: alguien se olvido de anotarse y su clan esta peleando
	 * sin el. No lo puede hacer el jugador porque entonces la lista congelada no serviria
	 * de nada.
	 *
	 * <p>Devuelve false si no habia nada que hacer: o ya estaba, o el duelo es de los
	 * viejos donde participan todos.
	 */
	public boolean agregarParticipante(UUID jugador) {
		return participantesCongelados && jugador != null && participantes.add(jugador);
	}

	public Set<UUID> getParticipantes() {
		return Collections.unmodifiableSet(participantes);
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
