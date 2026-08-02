package com.booksaw.betterTeams.luniatic.duelo;

import java.util.UUID;

/** Un desafio enviado y todavia no aceptado. Vence solo. */
public class Desafio {

	private final UUID retador;
	private final double apuesta;
	private final long venceMillis;

	public Desafio(UUID retador, double apuesta, long venceMillis) {
		this.retador = retador;
		this.apuesta = apuesta;
		this.venceMillis = venceMillis;
	}

	public UUID getRetador() {
		return retador;
	}

	public double getApuesta() {
		return apuesta;
	}

	public boolean vencio(long ahora) {
		return ahora >= venceMillis;
	}
}
