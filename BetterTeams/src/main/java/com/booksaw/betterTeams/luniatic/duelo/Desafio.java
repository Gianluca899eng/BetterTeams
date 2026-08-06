package com.booksaw.betterTeams.luniatic.duelo;

import java.util.UUID;

/** Un desafio enviado y todavia no aceptado. Vence solo. */
public class Desafio {

	private final UUID retador;
	private final double apuesta;
	/**
	 * Preset de duracion pactado.
	 *
	 * <p>Va en el desafio por el mismo motivo que el monto: las dos partes tienen que
	 * estar de acuerdo con lo que se juega, y cuanto dura es parte de eso. Aceptar un
	 * duelo de 30 minutos y que arranque uno de 7 dias no es aceptar lo mismo.
	 */
	private final String duracionId;
	private final long venceMillis;

	public Desafio(UUID retador, double apuesta, String duracionId, long venceMillis) {
		this.retador = retador;
		this.apuesta = apuesta;
		this.duracionId = duracionId;
		this.venceMillis = venceMillis;
	}

	public UUID getRetador() {
		return retador;
	}

	public double getApuesta() {
		return apuesta;
	}

	public String getDuracionId() {
		return duracionId;
	}

	public boolean vencio(long ahora) {
		return ahora >= venceMillis;
	}
}
