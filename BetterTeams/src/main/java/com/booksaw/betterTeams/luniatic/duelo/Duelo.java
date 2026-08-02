package com.booksaw.betterTeams.luniatic.duelo;

import java.util.UUID;

/**
 * Un duelo pactado en curso entre dos clanes.
 *
 * <p><b>Cuenta las bajas, y eso NO contradice la regla de no tener un marcador de
 * dominancia.</b> La primera version no las contaba, y quedaba sin forma de
 * resolverse: rendirse costaba el pozo y aguantar el reloj lo devolvia, asi que la
 * jugada optima era siempre esconderse hasta que terminara. Nadie se rendia nunca.
 *
 * <p>Lo toxico es el <b>registro publico y permanente</b>, no medir un combate
 * puntual. Este contador es privado —solo lo ven los dos clanes—, se borra cuando
 * el duelo termina, no se guarda en ningun lado y no toca el puntaje del clan.
 */
public class Duelo {

	private final UUID clanA;
	private final UUID clanB;
	/** Lo que aporto cada clan. El pozo total es el doble. */
	private final double apuesta;
	private final long finMillis;
	/** Bajas que le hizo el rival. Vive y muere con el duelo. */
	private int bajasA;
	private int bajasB;

	public Duelo(UUID clanA, UUID clanB, double apuesta, long finMillis) {
		this.clanA = clanA;
		this.clanB = clanB;
		this.apuesta = apuesta;
		this.finMillis = finMillis;
	}

	/** Suma una baja al clan que perdio al jugador. Devuelve el total. */
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
		if (clanA.equals(clan)) {
			return bajasA;
		}
		if (clanB.equals(clan)) {
			return bajasB;
		}
		return 0;
	}

	/**
	 * Quien va ganando, o null si estan iguales. Gana el que MENOS bajas tiene.
	 */
	public UUID getGanandoAhora() {
		if (bajasA == bajasB) {
			return null;
		}
		return bajasA < bajasB ? clanA : clanB;
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
		return clanA.equals(clan) || clanB.equals(clan);
	}

	/** Devuelve el otro participante, o null si el clan dado no esta en el duelo. */
	public UUID rivalDe(UUID clan) {
		if (clanA.equals(clan)) {
			return clanB;
		}
		if (clanB.equals(clan)) {
			return clanA;
		}
		return null;
	}

	public long segundosRestantes(long ahora) {
		return Math.max(0, (finMillis - ahora) / 1000);
	}
}
