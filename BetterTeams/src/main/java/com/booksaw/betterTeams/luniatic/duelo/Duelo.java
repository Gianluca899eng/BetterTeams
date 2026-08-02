package com.booksaw.betterTeams.luniatic.duelo;

import java.util.UUID;

/**
 * Un duelo pactado en curso entre dos clanes.
 *
 * <p>No guarda muertes a proposito. Contar bajas reconstruiria el marcador de
 * dominancia que el servidor decidio no tener: el duelo se resuelve porque
 * alguien se rinde o porque se acaba el tiempo, no por quien mato mas.
 */
public class Duelo {

	private final UUID clanA;
	private final UUID clanB;
	/** Lo que aporto cada clan. El pozo total es el doble. */
	private final double apuesta;
	private final long finMillis;

	public Duelo(UUID clanA, UUID clanB, double apuesta, long finMillis) {
		this.clanA = clanA;
		this.clanB = clanB;
		this.apuesta = apuesta;
		this.finMillis = finMillis;
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
