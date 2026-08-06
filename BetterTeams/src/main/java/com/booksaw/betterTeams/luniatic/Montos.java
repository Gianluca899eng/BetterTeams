package com.booksaw.betterTeams.luniatic;

import java.math.BigDecimal;

/**
 * Montos de plata escritos como los escribe un jugador.
 *
 * <p>Resuelve dos cosas distintas que se cruzan:
 *
 * <ul>
 * <li><b>El separador decimal.</b> Aca se escribe {@code 1.000,50}, no
 * {@code 1,000.50}. Si hay coma, la coma es el decimal y los puntos son de miles; si
 * no hay coma, el punto es el decimal, que es lo que espera {@code BigDecimal}.
 * <li><b>El sufijo de escala.</b> {@code 30k}, {@code 1,5m}, {@code 2b}. Nadie quiere
 * escribir 30000 en el chat, y menos con la plata de un duelo de por medio.
 * </ul>
 *
 * <p>Devuelve una cadena lista para {@code new BigDecimal(...)}. No parsea a
 * {@code double} ni valida rangos: eso lo decide cada comando, que es el que sabe si
 * un negativo o un cero tienen sentido en su caso.
 *
 * <p>El original vivia en {@code DueloCommand} y nacio de un defecto real: el mensaje
 * que explicaba como aceptar un duelo dictaba el monto con el separador del sistema
 * —{@code 0,00} en espaniol— y el comando lo rechazaba. Le decia al jugador que
 * escribiera algo imposible.
 */
public final class Montos {

	/** Sufijos aceptados, en el orden de {@link #ESCALAS}. */
	private static final String SUFIJOS = "kmb";

	private static final long[] ESCALAS = {1_000L, 1_000_000L, 1_000_000_000L};

	private Montos() {
	}

	/**
	 * Normaliza el monto escrito a algo que {@code BigDecimal} entienda.
	 *
	 * @param texto lo que escribio el jugador
	 * @return la cadena normalizada; puede seguir siendo invalida, y ahi el
	 * {@code BigDecimal} del llamador tira y el comando responde el error que
	 * corresponda
	 */
	public static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String limpio = texto.trim();

		// El sufijo se saca primero, para que la parte numerica pase por la misma regla
		// de separadores de siempre y no haya que duplicarla.
		long escala = 1L;
		if (!limpio.isEmpty()) {
			int indice = SUFIJOS.indexOf(Character.toLowerCase(limpio.charAt(limpio.length() - 1)));
			if (indice >= 0) {
				escala = ESCALAS[indice];
				limpio = limpio.substring(0, limpio.length() - 1).trim();
			}
		}

		String numero = limpio.indexOf(',') >= 0
				? limpio.replace(".", "").replace(',', '.')
				: limpio;

		if (escala == 1L) {
			return numero;
		}
		// La multiplicacion va en BigDecimal y no en double: 0,1k tiene que dar 100
		// exacto, y toPlainString evita que un monto grande salga en notacion
		// cientifica, que BigDecimal despues acepta pero el jugador no reconoce.
		return new BigDecimal(numero).multiply(BigDecimal.valueOf(escala)).toPlainString();
	}
}
