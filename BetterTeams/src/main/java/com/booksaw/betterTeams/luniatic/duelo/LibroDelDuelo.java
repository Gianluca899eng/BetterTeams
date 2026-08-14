package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * El libro que recibe cada participante cuando arranca un duelo.
 *
 * <p>Existe porque lo que se pacta —duracion, pozo, objetivo, quienes entran de cada lado—
 * hoy se dice en un mensaje de chat que se pierde en dos minutos. El libro queda en el
 * inventario y se puede releer cuando aparece la discusion de "yo no sabia".
 *
 * <p>🔑 <b>Los colores de la paleta de Luniatic NO se usan aca.</b> Esa paleta esta hecha
 * para texto claro sobre el fondo oscuro del chat; el libro es papel, y el lavanda del
 * cuerpo (#E4D9FF) sobre papel es practicamente invisible. Se usan codigos legacy oscuros,
 * que es lo unico legible en esta superficie.
 */
public final class LibroDelDuelo {

	/** Tope conservador: el cliente corta la pagina bastante antes de los 256 caracteres. */
	private static final int LINEAS_POR_PAGINA = 10;
	/**
	 * Tope de paginas.
	 *
	 * <p>Un libro escrito no acepta cualquier cantidad —el servidor rechaza pasado el
	 * limite— y un bando con cien miembros generaria decenas de paginas de lista. Se corta
	 * y se avisa en el libro, en vez de que reviente al entregarlo.
	 */
	private static final int MAX_PAGINAS = 45;

	private LibroDelDuelo() {
	}

	/** Arma el item a partir de paginas ya escritas. */
	public static ItemStack armar(String titulo, List<String> paginas) {
		ItemStack libro = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) libro.getItemMeta();
		if (meta == null) {
			return libro;
		}
		meta.setTitle(titulo == null || titulo.isEmpty() ? "Duelo" : recortar(titulo, 32));
		meta.setAuthor("Luniatic");
		meta.setPages(recortarPaginas(paginas));
		libro.setItemMeta(meta);
		return libro;
	}

	/**
	 * Deja el libro dentro del tope, avisando en la ultima pagina.
	 *
	 * <p>Cortar en silencio seria peor que el corte: el que lee no tendria forma de saber
	 * que falta gente en la lista de contra quienes pelea.
	 */
	private static List<String> recortarPaginas(List<String> paginas) {
		if (paginas.size() <= MAX_PAGINAS) {
			return paginas;
		}
		List<String> recortadas = new ArrayList<>(paginas.subList(0, MAX_PAGINAS - 1));
		recortadas.add("§8No entraron todas las paginas en el libro. La lista completa esta en "
				+ "§0/clan info§8.");
		return recortadas;
	}

	/** El titulo que lleva el libro de un duelo pactado. */
	public static String titulo(Team unClan, Team otroClan) {
		return recortar("Duelo: " + nombre(unClan) + " vs " + nombre(otroClan), 32);
	}

	/** El titulo del libro de desafio, el que llega antes de aceptar. */
	public static String tituloDesafio(Team retador) {
		return recortar("Desafio de " + nombre(retador), 32);
	}

	/**
	 * El libro que reciben lider y colideres cuando otro clan los desafia.
	 *
	 * <p>Es el unico de los tres que llega <b>antes</b> de decidir, asi que su trabajo no es
	 * contar algo que ya esta pasando: es que la decision se tome con los numeros a la vista.
	 * Por eso trae la fuerza de los dos bandos y los aliados que entran con el que desafia —
	 * lo mismo que el menu, pero en algo que se puede releer sin tenerlo abierto.
	 *
	 * <p>Solo va al mando: un miembro comun no puede aceptar ni rechazar, y darle un libro
	 * que no puede usar es ruido.
	 */
	public static List<String> paginasDeDesafio(Team retador, Team retado, double apuesta,
			String etiquetaDuracion, int objetivoBajas, int fuerzaRetador, int fuerzaRetado,
			Set<UUID> aliadosDelRetador, long horasParaResponder) {
		List<String> paginas = new ArrayList<>();

		StringBuilder portada = new StringBuilder();
		portada.append("§0§lDesafio a duelo\n\n");
		portada.append("§4").append(nombre(retador)).append("\n");
		portada.append("§0desafia a\n");
		portada.append("§1").append(nombre(retado)).append("\n\n");
		portada.append("§8Apuesta: §0$").append(String.format("%.2f", apuesta)).append(" §8cada uno\n");
		portada.append("§8Duracion: §0").append(etiquetaDuracion).append("\n");
		portada.append("§8Objetivo: §0").append(objetivoBajas).append(" caidas");
		paginas.add(portada.toString());

		StringBuilder fuerzas = new StringBuilder();
		fuerzas.append("§0§lQuienes pelearian\n\n");
		fuerzas.append("§4").append(nombre(retador)).append("§0: ")
				.append(fuerzaRetador).append(" jugadores\n");
		fuerzas.append("§1").append(nombre(retado)).append("§0: ")
				.append(fuerzaRetado).append(" jugadores\n\n");
		fuerzas.append("§8Cuenta clan mas aliados, y solo a los que estan anotados para pelear.");
		paginas.add(fuerzas.toString());

		StringBuilder aliados = new StringBuilder();
		aliados.append("§0§lSus aliados\n\n");
		if (aliadosDelRetador == null || aliadosDelRetador.isEmpty()) {
			aliados.append("§0Pelean solos: ningun aliado entraria con ellos.");
		} else {
			aliados.append("§0Entrarian con ellos:\n\n");
			for (UUID idAliado : aliadosDelRetador) {
				Team aliado = Team.getTeam(idAliado);
				if (aliado != null) {
					aliados.append("§4 ").append(nombre(aliado)).append("\n");
				}
			}
		}
		paginas.add(aliados.toString());

		paginas.add("§0§lSi aceptan\n\n"
				+ "§0Los rivales podran §4matarlos§0 aunque tengan el PvP apagado, y sus "
				+ "aliados tambien.\n\n"
				+ "§0La apuesta sale del banco del clan al empezar.\n\n"
				+ "§8Nadie puede romper ni robar en sus protecciones.");

		StringBuilder comoResponder = new StringBuilder();
		comoResponder.append("§0§lComo responder\n\n");
		comoResponder.append("§0Aceptar:\n§1/clan duelo aceptar ").append(nombre(retador)).append("\n\n");
		comoResponder.append("§0Rechazar:\n§4/clan duelo rechazar ").append(nombre(retador)).append("\n\n");
		comoResponder.append("§8O desde §0/clan §8> Duelos.\n");
		comoResponder.append("§8Vence en ").append(horasParaResponder).append(" h.");
		paginas.add(comoResponder.toString());

		return paginas;
	}

	/**
	 * Las paginas del libro de un duelo.
	 *
	 * <p>Corre <b>una vez por duelo</b>, no por jugador: el mismo texto se le entrega a
	 * todos. Por eso puede permitirse resolver nombres de jugadores desconectados, que es
	 * lo unico caro que hay aca.
	 */
	public static List<String> paginas(Duelo duelo, Team clanA, Team clanB, String etiquetaDuracion) {
		List<String> paginas = new ArrayList<>();

		StringBuilder portada = new StringBuilder();
		portada.append("§0§lDuelo de clanes\n\n");
		portada.append("§8Se pacto entre:\n");
		portada.append("§1").append(nombre(clanA)).append("\n");
		portada.append("§0contra\n");
		portada.append("§4").append(nombre(clanB)).append("\n\n");
		portada.append("§8Duracion: §0").append(etiquetaDuracion).append("\n");
		portada.append("§8Objetivo: §0").append(duelo.getObjetivoBajas()).append(" caidas\n");
		portada.append("§8Pozo: §0$").append(String.format("%.2f", duelo.getPozo()));
		paginas.add(portada.toString());

		paginas.addAll(paginasDeBando("§1" + nombre(clanA), duelo.getBando(duelo.getClanA()), duelo.getClanA()));
		paginas.addAll(paginasDeBando("§4" + nombre(clanB), duelo.getBando(duelo.getClanB()), duelo.getClanB()));

		paginas.add("§0§lLo que cambia\n\n"
				+ "§0Los rivales te pueden §4matar§0, aunque tengas el PvP apagado.\n\n"
				+ "§0Tambien te pueden matar §4los aliados§0 del clan rival.\n\n"
				+ "§0Adentro de tu propia proteccion §4tambien§0.");

		paginas.add("§0§lLo que NO cambia\n\n"
				+ "§0Nadie puede romper ni robar en tu proteccion. El duelo es pelea entre "
				+ "personas, no permiso para griefear.\n\n"
				+ "§8Lo que se te caiga al morir se pierde igual que siempre.");

		paginas.add("§0§lVolver a la pelea\n\n"
				+ "§0Si te mata el clan rival no puedes usar §4/dback§0 para volver al lugar.\n\n"
				+ "§0Tampoco puedes teletransportarte ni reaparecer cerca de una base rival.\n\n"
				+ "§8A tu propia base si.");

		paginas.add("§0§lComo termina\n\n"
				+ "§0Gana el bando que llegue primero al objetivo de caidas.\n\n"
				+ "§0Si se cumple el tiempo, gana el que menos cayo. Empate exacto: cada uno "
				+ "recupera lo suyo.\n\n"
				+ "§8Solo el lider puede rendirse.");

		return paginas;
	}

	/** Como termino el duelo. Cambia el titulo y la explicacion del libro de resultados. */
	public enum Final {
		OBJETIVO,
		TIEMPO,
		RENDICION,
		EMPATE
	}

	/** El titulo del libro de resultados. */
	public static String tituloResumen(Team unClan, Team otroClan) {
		return recortar("Resultado: " + nombre(unClan) + " vs " + nombre(otroClan), 32);
	}

	/**
	 * El libro que reciben todos cuando el duelo termina.
	 *
	 * <p>Es el espejo del de arranque y existe por el mismo motivo: lo que paso —quien gano,
	 * con que marcador, cuanto se llevo— se dice en un mensaje de chat que se pierde. Con el
	 * libro, el que estaba desconectado cuando termino igual se entera, y con el detalle.
	 */
	public static List<String> paginasDeResumen(Duelo duelo, Team clanA, Team clanB,
			Final comoTermino, Team ganador) {
		List<String> paginas = new ArrayList<>();

		StringBuilder portada = new StringBuilder();
		portada.append("§0§lResultado del duelo\n\n");
		portada.append("§1").append(nombre(clanA)).append("\n");
		portada.append("§0contra\n");
		portada.append("§4").append(nombre(clanB)).append("\n\n");
		if (comoTermino == Final.EMPATE || ganador == null) {
			portada.append("§0§lEmpate.\n");
			portada.append("§8Cada clan recupera lo suyo.");
		} else {
			portada.append("§0Gano §1").append(nombre(ganador)).append("§0.\n\n");
			portada.append("§8Se lleva §0$").append(String.format("%.2f", duelo.getPozo()));
		}
		paginas.add(portada.toString());

		StringBuilder detalle = new StringBuilder();
		detalle.append("§0§lComo termino\n\n");
		switch (comoTermino) {
			case OBJETIVO:
				detalle.append("§0Se llego al objetivo de §4")
						.append(duelo.getObjetivoBajas()).append("§0 caidas.\n\n");
				break;
			case TIEMPO:
				detalle.append("§0Se cumplio el tiempo. Gana el que menos cayo.\n\n");
				break;
			case RENDICION:
				detalle.append("§0Uno de los dos se rindio, asi que el pozo entero fue para el "
						+ "otro.\n\n");
				break;
			default:
				detalle.append("§0Se cumplio el tiempo con el marcador igualado.\n\n");
				break;
		}
		detalle.append("§8Marcador de caidas:\n");
		detalle.append("§1").append(nombre(clanA)).append("§0: ")
				.append(duelo.getBajasA()).append("\n");
		detalle.append("§4").append(nombre(clanB)).append("§0: ")
				.append(duelo.getBajasB()).append("\n\n");
		detalle.append("§8Menos caidas es mejor.");
		paginas.add(detalle.toString());

		paginas.add("§0§lLo que vuelve a la normalidad\n\n"
				+ "§0Tu §1/pvp§0 vuelve a mandar: nadie te puede pegar si lo tienes apagado.\n\n"
				+ "§0Ya puedes usar §1/dback§0, volar y teletransportarte cerca de sus bases.\n\n"
				+ "§8Las protecciones nunca se tocaron.");

		return paginas;
	}

	/** Una o mas paginas con los miembros de un bando, partidas si no entran. */
	private static List<String> paginasDeBando(String tituloBando, Set<UUID> clanes, UUID principal) {
		List<String> paginas = new ArrayList<>();
		List<String> lineas = new ArrayList<>();

		for (UUID idClan : ordenarConPrincipalPrimero(clanes, principal)) {
			Team clan = Team.getTeam(idClan);
			if (clan == null) {
				continue;
			}
			lineas.add("§8" + (idClan.equals(principal) ? "" : "aliado: ") + "§1" + clan.getName());
			for (TeamPlayer miembro : clan.getMembers().getClone()) {
				lineas.add("§0 " + nombreDe(miembro.getPlayerUUID()));
			}
		}
		if (lineas.isEmpty()) {
			lineas.add("§8(sin miembros)");
		}

		for (int i = 0; i < lineas.size(); i += LINEAS_POR_PAGINA) {
			StringBuilder pagina = new StringBuilder();
			pagina.append(tituloBando).append("§0§l:\n\n");
			for (int j = i; j < Math.min(i + LINEAS_POR_PAGINA, lineas.size()); j++) {
				pagina.append(lineas.get(j)).append("\n");
			}
			paginas.add(pagina.toString());
		}
		return paginas;
	}

	/** El clan que pacto va primero: es el que da el nombre al bando. */
	private static Set<UUID> ordenarConPrincipalPrimero(Set<UUID> clanes, UUID principal) {
		Set<UUID> ordenados = new LinkedHashSet<>();
		if (clanes.contains(principal)) {
			ordenados.add(principal);
		}
		ordenados.addAll(clanes);
		return ordenados;
	}

	/**
	 * El nombre de un jugador que puede estar desconectado.
	 *
	 * <p>Devuelve el UUID recortado si nunca se lo vio: es feo, pero es mejor que una linea
	 * en blanco en la lista de contra quienes vas a pelear.
	 */
	private static String nombreDe(UUID jugador) {
		String nombre = Bukkit.getOfflinePlayer(jugador).getName();
		return nombre != null ? nombre : jugador.toString().substring(0, 8);
	}

	private static String nombre(Team clan) {
		return clan == null ? "?" : com.booksaw.betterTeams.luniatic.Texto.limpiar(clan.getName());
	}

	private static String recortar(String texto, int largo) {
		return texto.length() <= largo ? texto : texto.substring(0, largo);
	}
}
