package com.booksaw.betterTeams.luniatic;

import java.util.Locale;

/**
 * Resuelve el nombre de un comando tal como aparece en una lista de configuracion: en
 * minusculas, sin la barra, sin argumentos y sin las dos formas de esquivarlo.
 *
 * <p>Las dos formas son distintas y hay que sacar las dos:
 *
 * <ul>
 * <li><b>El namespace.</b> {@code /cmi:dback} es el mismo comando que {@code /dback}.
 * <li><b>El comando envoltorio.</b> {@code /cmi dback} tambien lo es, y es ademas la forma
 *     canonica: el alias {@code /dback} lo genera CMI desde su {@code Alias.yml}. Cortar en
 *     el primer espacio lo dejaba en {@code cmi}, que no esta en ninguna lista, asi que
 *     cualquier comando de CMI se colaba escribiendolo con el prefijo.
 * </ul>
 *
 * <p>Hoy el segundo caso lo tapa de rebote la lista blanca de ProAntiTab, donde {@code cmi}
 * no figura. Eso no es una defensa: alcanza con agregarlo ahi, o con tener el bypass de
 * ProAntiTab, para que el hueco se abra.
 */
public final class Comandos {

	/** Comandos que solo son un prefijo del comando de verdad. */
	private static final String ENVOLTORIO = "cmi";

	private Comandos() {
	}

	/**
	 * El nombre del comando que el jugador ejecuto de verdad, listo para comparar contra una
	 * lista. Devuelve cadena vacia si el mensaje no tiene ninguno.
	 */
	public static String nombre(String mensaje) {
		if (mensaje == null) {
			return "";
		}
		String texto = mensaje.trim();
		if (texto.startsWith("/")) {
			texto = texto.substring(1);
		}

		String primero = limpiar(primerToken(texto));
		if (!ENVOLTORIO.equals(primero)) {
			return primero;
		}
		String segundo = limpiar(primerToken(resto(texto)));
		return segundo.isEmpty() ? primero : segundo;
	}

	private static String primerToken(String texto) {
		int espacio = texto.indexOf(' ');
		return espacio >= 0 ? texto.substring(0, espacio) : texto;
	}

	private static String resto(String texto) {
		int espacio = texto.indexOf(' ');
		return espacio >= 0 ? texto.substring(espacio + 1).trim() : "";
	}

	private static String limpiar(String token) {
		int namespace = token.indexOf(':');
		if (namespace >= 0) {
			token = token.substring(namespace + 1);
		}
		return token.toLowerCase(Locale.ROOT);
	}
}
