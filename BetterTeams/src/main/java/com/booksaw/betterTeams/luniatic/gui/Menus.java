package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.luniatic.Texto;
import com.booksaw.betterTeams.luniatic.duelo.Desafio;
import com.booksaw.betterTeams.luniatic.duelo.Duelo;
import com.booksaw.betterTeams.luniatic.duelo.DueloManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Menu de clanes.
 *
 * <p>Sigue el mismo lenguaje visual que el menu de misiones de LuniaticPerf: marco
 * de cristal gris, interior mas claro, y navegacion fija abajo.
 *
 * <p><b>Los textos van escritos aca y no en los archivos de idioma</b>, igual que
 * en el menu de misiones. Es un menu propio de Luniatic, no de upstream: meterlo
 * en messages.yml agregaria ~60 claves a dos archivos que hay que mantener
 * sincronizados con cada version nueva, sin ganar nada.
 *
 * <p>🔑 <b>El color hay que convertirlo a mano.</b> Los mensajes del plugin viajan
 * como texto crudo con codigos {@code &#RRGGBB} y etiquetas MiniMessage, y el
 * inventario los muestra tal cual: hay que pasarlos por
 * {@code toAdventure} y despues serializarlos a legacy. Sin eso el menu se ve
 * con los codigos escritos, que fue exactamente el primer intento.
 */
public final class Menus {

	private static final int TAM = 54;
	private static final int SLOT_ANTERIOR = 45;
	private static final int SLOT_VOLVER = 48;
	private static final int SLOT_CERRAR = 49;
	private static final int SLOT_SIGUIENTE = 53;

	/** Interior del cofre: cuatro filas de siete. */
	private static final int[] CONTENIDO = {
			10, 11, 12, 13, 14, 15, 16,
			19, 20, 21, 22, 23, 24, 25,
			28, 29, 30, 31, 32, 33, 34,
			37, 38, 39, 40, 41, 42, 43
	};

	// Rejilla de las pantallas fijas. Los botones caen siempre en las mismas
	// columnas: una fila con 6 items y otra con 5 corridos se ve desprolijo.
	/** Centro de la primera fila, para el titulo de la pantalla. */
	private static final int CABECERA = 13;
	/** Fila de arriba, sus siete columnas. */
	private static final int[] FILA_A = {19, 20, 21, 22, 23, 24, 25};
	/** Fila de abajo, sus siete columnas. Se usan las impares para centrar. */
	private static final int[] FILA_B = {28, 29, 30, 31, 32, 33, 34};
	/** Centro de la ultima fila, para la accion que cierra la pantalla. */
	private static final int PIE = 40;

	// Regla fija para cualquier par de si/no, en cualquier pantalla: el NO a la
	// izquierda y el SI a la derecha, siempre. Tenerlo al reves en una pantalla y
	// no en otra hace que la gente confirme de memoria y se equivoque.
	private static final int SLOT_NO = 29;
	private static final int SLOT_SI = 33;

	// Paleta de Luniatic.
	private static final String MARCA = "&#9235FF";
	private static final String CUERPO = "&#E4D9FF";
	private static final String ETIQUETA = "&#7162FF";
	private static final String ERROR = "&#FF4554";


	private Menus() {
	}

	// ------------------------------------------------------------------ portada

	public static void abrirPortada(Player jugador) {
		Team clan = Team.getTeam(jugador);
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Clanes", clan == null ? "sin clan" : limpiar(clan.getName()));

		if (clan == null) {
			inv.setItem(CABECERA, boton(Material.PAPER, "Todavia no tenes clan",
					CUERPO + "Un clan te da chat propio, cofre compartido,",
					CUERPO + "casa del clan y banco en comun.",
					"",
					CUERPO + "A los abiertos entras con un clic;",
					CUERPO + "a los cerrados hay que ser invitado."));

			inv.setItem(FILA_A[1], boton(Material.COMPASS, "Explorar clanes",
					"Mira todos los clanes del servidor", "y sumate a los que esten abiertos."));
			holder.asignar(FILA_A[1], j -> abrirLista(j, 0, null));

			inv.setItem(FILA_A[3], boton(Material.WRITABLE_BOOK, "Crear tu clan",
					CUERPO + "Es lo unico que hay que escribir,",
					CUERPO + "porque el nombre lo elegis vos:",
					"", ETIQUETA + "/team create <nombre>"));
			holder.asignar(FILA_A[3], j -> {
				j.closeInventory();
				mensaje(j, "Para crear tu clan escribi " + ETIQUETA + "/team create <nombre>");
			});

			inv.setItem(FILA_A[5], boton(Material.NETHER_STAR, "Ranking",
					"Los clanes con mas puntaje."));
			holder.asignar(FILA_A[5], j -> abrirRanking(j));
		} else {
			TeamPlayer yo = clan.getTeamPlayer(jugador);
			boolean mando = yo != null && yo.getRank() != PlayerRank.DEFAULT;

			inv.setItem(CABECERA, boton(Material.BOOK, limpiar(clan.getName()), datosClan(clan)));
			holder.asignar(CABECERA, j -> abrirFicha(j, clan.getName(), () -> abrirPortada(j)));

			// Fila de arriba: las siete cosas del dia a dia, una por columna.
			inv.setItem(FILA_A[0], boton(Material.PLAYER_HEAD, "Miembros",
					CUERPO + "Conectados: " + MARCA + clan.getMembers().getOnlinePlayers().size()
							+ CUERPO + " de " + MARCA + clan.getMembers().size(),
					"", ETIQUETA + "Clic para ver la lista"));
			holder.asignar(FILA_A[0], j -> abrirMiembros(j, 0));

			inv.setItem(FILA_A[1], boton(Material.ENDER_CHEST, "Cofre del clan",
					"El cofre compartido de todo el clan."));
			holder.asignar(FILA_A[1], j -> comando(j, "team echest"));

			inv.setItem(FILA_A[2], boton(Material.GOLD_INGOT, "Banco del clan",
					CUERPO + "Saldo: " + MARCA + "$" + clan.getBalance(),
					"", ETIQUETA + "Clic para poner o sacar plata"));
			holder.asignar(FILA_A[2], Menus::abrirBanco);

			inv.setItem(FILA_A[3], boton(Material.RED_BED, "Casa del clan",
					"Te lleva a la casa del clan.",
					"",
					mando ? CUERPO + "Para moverla, entra en Ajustes."
							: CUERPO + "Solo el mando puede moverla."));
			holder.asignar(FILA_A[3], j -> comando(j, "team home"));

			inv.setItem(FILA_A[4], boton(Material.OAK_SIGN, "Warps del clan",
					"Los puntos guardados del clan.",
					"", ETIQUETA + "Clic para viajar a uno"));
			holder.asignar(FILA_A[4], Menus::abrirWarps);

			inv.setItem(FILA_A[5], boton(Material.SHIELD, "Aliados",
					"Los clanes con los que no te pegas."));
			holder.asignar(FILA_A[5], j -> abrirAliados(j));

			inv.setItem(FILA_A[6], itemDuelos(clan));
			holder.asignar(FILA_A[6], Menus::abrirDuelos);

			// Fila de abajo: lo que mira hacia afuera del clan, centrado.
			inv.setItem(FILA_B[1], boton(Material.COMPASS, "Explorar clanes",
					"Mira todos los clanes del servidor."));
			holder.asignar(FILA_B[1], j -> abrirLista(j, 0, null));

			inv.setItem(FILA_B[3], boton(Material.NETHER_STAR, "Ranking",
					"Los clanes con mas puntaje."));
			holder.asignar(FILA_B[3], j -> abrirRanking(j));

			inv.setItem(FILA_B[5], boton(Material.COMPARATOR, "Ajustes del clan",
					mando ? "Abrir o cerrar el clan, chat, color y mas."
							: ERROR + "Solo el mando puede tocar esto."));
			holder.asignar(FILA_B[5], j -> abrirAjustes(j));

			// Si es el unico duenio no puede irse: el plugin se lo va a negar. En ese
			// caso lo que corresponde ofrecer es disolver, no salir.
			boolean unicoDuenio = yo != null && yo.getRank() == PlayerRank.OWNER
					&& clan.getRank(PlayerRank.OWNER).size() == 1;
			if (unicoDuenio) {
				inv.setItem(PIE, boton(Material.TNT, ERROR + "Disolver el clan",
						CUERPO + "Sos el unico duenio, asi que no podes irte:",
						CUERPO + "o le pasas el mando a alguien, o lo disolves.",
						"", ERROR + "Se pierden el cofre y el banco."));
				holder.asignar(PIE, j -> abrirConfirmacion(j, "Disolver el clan",
						new String[]{
								CUERPO + "Se borra " + MARCA + limpiar(clan.getName()) + CUERPO + " para todos.",
								ERROR + "Se pierden los items del cofre y la plata del banco."
						}, "team disband confirm", () -> abrirPortada(j)));
			} else {
				inv.setItem(PIE, boton(Material.IRON_DOOR, ERROR + "Salir del clan",
						CUERPO + "Te vas de " + MARCA + limpiar(clan.getName()) + CUERPO + ".",
						"", ETIQUETA + "Clic para salir"));
				holder.asignar(PIE, j -> abrirConfirmacion(j, "Salir del clan",
						new String[]{
								CUERPO + "Te vas de " + MARCA + limpiar(clan.getName()) + CUERPO + ".",
								CUERPO + "Perdes el acceso al cofre y al banco del clan."
						}, "team leave", () -> abrirPortada(j)));
			}
		}

		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------ duelos

	private static ItemStack itemDuelos(Team clan) {
		DueloManager manager = Main.plugin.getDueloManager();
		if (manager == null || !manager.isHabilitado()) {
			return boton(Material.GRAY_DYE, CUERPO + "Duelos",
					CUERPO + "Estan desactivados en este servidor.");
		}
		Duelo duelo = manager.getDuelo(clan);
		if (duelo != null) {
			Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
			return boton(Material.NETHERITE_SWORD, ERROR + "Duelo en curso",
					CUERPO + "Contra " + MARCA + (rival == null ? "?" : limpiar(rival.getName())),
					CUERPO + "Caidas: vos " + MARCA + duelo.getBajas(clan.getID())
							+ CUERPO + ", ellos " + MARCA
							+ (rival == null ? "?" : duelo.getBajas(rival.getID())),
					"", ETIQUETA + "Clic para ver el duelo");
		}
		List<Desafio> recibidos = manager.getDesafiosRecibidos(clan);
		if (!recibidos.isEmpty()) {
			Team retador = Team.getTeam(recibidos.get(0).getRetador());
			return boton(Material.BELL, MARCA + "Te desafiaron",
					CUERPO + (retador == null ? "?" : limpiar(retador.getName()))
							+ CUERPO + " quiere duelo por " + MARCA + "$"
							+ fmt(recibidos.get(0).getApuesta()),
					recibidos.size() > 1
							? CUERPO + "y " + MARCA + (recibidos.size() - 1) + CUERPO + " invitacion(es) mas"
							: "",
					"", ETIQUETA + "Clic para responder");
		}
		return boton(Material.IRON_SWORD, "Duelos",
				CUERPO + "Un duelo pactado contra otro clan,",
				CUERPO + "con una apuesta que ponen los dos.",
				"", ETIQUETA + "Clic para entrar");
	}

	public static void abrirDuelos(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		DueloManager manager = Main.plugin.getDueloManager();
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Duelos", limpiar(clan.getName()));

		if (manager == null || !manager.isHabilitado()) {
			inv.setItem(CABECERA, boton(Material.GRAY_DYE, CUERPO + "Duelos desactivados",
					CUERPO + "El servidor los tiene apagados."));
			volverA(inv, holder, Menus::abrirPortada);
			jugador.openInventory(inv);
			return;
		}

		Duelo duelo = manager.getDuelo(clan);
		List<Desafio> recibidos = manager.getDesafiosRecibidos(clan);
		// Lider y colider, igual que el comando: si el menu mostrara el boton a un
		// miembro comun, seria un boton que no hace nada.
		boolean mando = tieneMando(clan, jugador);

		if (duelo != null) {
			Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
			String nombreRival = rival == null ? "?" : limpiar(rival.getName());
			inv.setItem(CABECERA, boton(Material.NETHERITE_SWORD, ERROR + "Duelo contra " + nombreRival,
					CUERPO + "Pozo: " + MARCA + "$" + fmt(duelo.getPozo()),
					CUERPO + "Objetivo: " + MARCA + manager.getObjetivoBajas() + CUERPO + " caidas",
					"",
					CUERPO + "Ustedes cayeron " + MARCA + duelo.getBajas(clan.getID()) + CUERPO + " veces",
					CUERPO + "Ellos cayeron " + MARCA
							+ (rival == null ? "?" : duelo.getBajas(rival.getID())) + CUERPO + " veces",
					"",
					CUERPO + "Quedan " + MARCA
							+ duelo.segundosRestantes(System.currentTimeMillis()) / 60
							+ CUERPO + " minutos. Al vencer gana el que menos cayo."));

			inv.setItem(FILA_B[1], boton(Material.CLOCK, "Actualizar",
					CUERPO + "Vuelve a leer el marcador."));
			holder.asignar(FILA_B[1], Menus::abrirDuelos);

			if (!duelo.esPrincipal(clan.getID())) {
				Team principal = Team.getTeam(duelo.getPrincipalDe(clan.getID()));
				inv.setItem(FILA_B[5], boton(Material.SHIELD, CUERPO + "Entraste como aliado",
						CUERPO + "El duelo lo pacto " + MARCA
								+ (principal == null ? "?" : limpiar(principal.getName())) + CUERPO + ".",
						CUERPO + "Solo ellos pueden rendirlo, y el pozo",
						CUERPO + "es de ellos: ustedes solo pelean.",
						"", ERROR + "Mientras dure, pueden matarte."));
			} else if (mando) {
				inv.setItem(FILA_B[5], boton(Material.WHITE_BANNER, ERROR + "Rendirse",
						CUERPO + "Cortas el duelo y el pozo entero",
						CUERPO + "se lo lleva " + MARCA + nombreRival + CUERPO + ".",
						"", CUERPO + "Sirve cuando ya esta perdido y",
						CUERPO + "no vale la pena esperar al reloj."));
				holder.asignar(FILA_B[5], j -> abrirConfirmacion(j, "Rendirse",
						new String[]{
								CUERPO + "El pozo de " + MARCA + "$" + fmt(duelo.getPozo())
										+ CUERPO + " se lo lleva " + MARCA + nombreRival + CUERPO + "."
						}, "team duelo rendirse", () -> abrirDuelos(j)));
			}
		} else {
			inv.setItem(CABECERA, boton(Material.IRON_SWORD, "Duelos pactados",
					CUERPO + "Los dos clanes tienen que estar de acuerdo:",
					CUERPO + "uno desafia con un monto y el otro lo acepta.",
					"",
					CUERPO + "Pierde el que llegue a " + MARCA + manager.getObjetivoBajas()
							+ CUERPO + " caidas.",
					CUERPO + "Si se cumple el tiempo, gana el que menos cayo.",
					"",
					CUERPO + "No se toca nada de los claims: es pelea",
					CUERPO + "entre personas, no permiso para romper."));

			if (mando) {
				inv.setItem(FILA_B[1], boton(Material.IRON_SWORD, "Desafiar a un clan",
						CUERPO + "Elegi contra quien y cuanto.",
						CUERPO + "No hace falta que esten conectados."));
				holder.asignar(FILA_B[1], j -> abrirElegirRival(j, 0));

				inv.setItem(FILA_B[5], boton(
						recibidos.isEmpty() ? Material.GRAY_DYE : Material.BELL,
						recibidos.isEmpty() ? CUERPO + "Sin invitaciones"
								: MARCA + "Invitaciones (" + recibidos.size() + ")",
						recibidos.isEmpty() ? CUERPO + "Nadie te desafio por ahora."
								: CUERPO + "Clic para aceptarlas o rechazarlas"));
				if (!recibidos.isEmpty()) {
					holder.asignar(FILA_B[5], j -> abrirInvitaciones(j));
				}
			} else {
				inv.setItem(FILA_B[3], boton(Material.BARRIER,
						CUERPO + "Solo lider y colider pactan duelos"));
			}
		}

		volverA(inv, holder, Menus::abrirPortada);
		jugador.openInventory(inv);
	}

	/**
	 * Las invitaciones a duelo que le llegaron al clan.
	 *
	 * <p>Son varias porque varios clanes pueden desafiarte a la vez. Cada una se
	 * acepta o se rechaza por separado, y rechazar avisa al que la mando: dejarla
	 * vencer en silencio es peor que decir que no.
	 */
	public static void abrirInvitaciones(Player jugador) {
		Team clan = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (clan == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		List<Desafio> recibidos = manager.getDesafiosRecibidos(clan);

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Invitaciones", recibidos.size() + " sin responder");
		vaciarContenido(inv);

		boolean mando = tieneMando(clan, jugador);
		int i = 0;
		for (Desafio desafio : recibidos) {
			if (i >= CONTENIDO.length) {
				break;
			}
			Team retador = Team.getTeam(desafio.getRetador());
			if (retador == null) {
				continue;
			}
			String nombre = retador.getName();
			String monto = fmt(desafio.getApuesta());
			inv.setItem(CONTENIDO[i], boton(Material.BELL, limpiar(nombre),
					CUERPO + "Apuesta: " + MARCA + "$" + monto + CUERPO + " cada uno",
					CUERPO + "Ellos tienen " + MARCA + retador.getMembers().size()
							+ CUERPO + " miembros",
					"",
					mando ? MARCA + "Clic izquierdo: aceptar" : CUERPO + "Solo lider y colider responden",
					mando ? ERROR + "Clic derecho: rechazar" : ""));
			// Un nombre con espacios correria el monto a otro argumento. Hoy no puede
			// pasar, pero depende de una config que alguien puede vaciar.
			if (mando && argumentoSeguro(nombre)) {
				holder.asignarConClic(CONTENIDO[i],
						j -> abrirConfirmacion(j, "Aceptar el duelo",
								new String[]{
										CUERPO + "Contra " + MARCA + limpiar(nombre) + CUERPO + ".",
										CUERPO + "Se te van " + MARCA + "$" + monto
												+ CUERPO + " del banco del clan.",
										CUERPO + "Los recuperas doblados si ganan."
								}, "team duelo aceptar " + nombre, () -> abrirInvitaciones(j)),
						j -> ejecutarYVolver(j, "team duelo rechazar " + nombre,
								() -> abrirInvitaciones(j)));
			}
			i++;
		}

		if (i == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Sin invitaciones",
					CUERPO + "Nadie desafio a tu clan por ahora."));
		}

		volverA(inv, holder, Menus::abrirDuelos);
		jugador.openInventory(inv);
	}

	/** Elegir contra quien, sin escribir el nombre. */
	public static void abrirElegirRival(Player jugador, int pagina) {
		Team propio = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (propio == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		Main.plugin.getFoliaLib().getScheduler().runAsync(t -> {
			List<Team> candidatos = new ArrayList<>();
			for (Team otro : buscar(null)) {
				if (!otro.getID().equals(propio.getID()) && manager.estaLibre(otro)) {
					candidatos.add(otro);
				}
			}
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador, t2 -> {
				int porPagina = CONTENIDO.length;
				int paginas = Math.max(1, (int) Math.ceil(candidatos.size() / (double) porPagina));
				int actual = Math.max(0, Math.min(pagina, paginas - 1));

				MenuHolder holder = new MenuHolder();
				Inventory inv = crear(holder, "Desafiar", (actual + 1) + "/" + paginas);
				vaciarContenido(inv);

				int desde = actual * porPagina;
				for (int i = desde; i < candidatos.size() && i - desde < porPagina; i++) {
					Team rival = candidatos.get(i);
					inv.setItem(CONTENIDO[i - desde], boton(Material.IRON_SWORD,
							limpiar(rival.getName()), datosClan(rival)));
					String nombre = rival.getName();
					holder.asignar(CONTENIDO[i - desde], j -> abrirElegirApuesta(j, nombre));
				}
				if (candidatos.isEmpty()) {
					inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "No hay a quien desafiar",
							CUERPO + "No hay otro clan libre en este momento."));
				}

				if (actual > 0) {
					inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
					holder.asignar(SLOT_ANTERIOR, j -> abrirElegirRival(j, actual - 1));
				}
				if (actual < paginas - 1) {
					inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
					holder.asignar(SLOT_SIGUIENTE, j -> abrirElegirRival(j, actual + 1));
				}
				volverA(inv, holder, Menus::abrirDuelos);
				jugador.openInventory(inv);
			});
		});
	}

	/** Elegir cuanto, sin escribir el monto. */
	public static void abrirElegirApuesta(Player jugador, String nombreRival) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Apuesta", "contra " + limpiar(nombreRival));

		inv.setItem(CABECERA, boton(Material.PAPER, "Cuanto se juega",
				CUERPO + "Cada clan pone lo mismo y el ganador",
				CUERPO + "se lleva las dos partes.",
				"",
				CUERPO + "Tu banco tiene " + MARCA + "$" + clan.getBalance() + CUERPO + ".",
				"",
				CUERPO + "El otro clan tiene que aceptar el mismo",
				CUERPO + "monto para que arranque."));

		if (!argumentoSeguro(nombreRival)) {
			// Ver argumentoSeguro: con espacios en el nombre, el monto se corre.
			inv.setItem(CABECERA, boton(Material.BARRIER, ERROR + "Ese clan no se puede desafiar",
					CUERPO + "Su nombre tiene caracteres que rompen el comando."));
			volverA(inv, holder, j -> abrirElegirRival(j, 0));
			jugador.openInventory(inv);
			return;
		}

		int[] opciones = {0, 100, 1000, 10000};
		Material[] iconos = {Material.PAPER, Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.GOLD_BLOCK};
		int[] slots = {FILA_B[0], FILA_B[2], FILA_B[4], FILA_B[6]};
		for (int i = 0; i < opciones.length; i++) {
			int monto = opciones[i];
			inv.setItem(slots[i], boton(iconos[i], monto == 0 ? "Sin apuesta" : "$" + monto,
					monto == 0 ? CUERPO + "Solo por el orgullo." : CUERPO + "Cada clan pone $" + monto));
			holder.asignar(slots[i], j -> abrirElegirDuracion(j, nombreRival, monto));
		}

		volverA(inv, holder, j -> abrirElegirRival(j, 0));
		jugador.openInventory(inv);
	}

	/**
	 * Tercer paso del desafio: cuanto dura.
	 *
	 * <p>Va despues del monto y antes de confirmar, y es una pantalla propia y no un
	 * detalle de la anterior porque <b>la duracion cambia lo que es el duelo</b>: media
	 * hora es una pelea pactada, siete dias es una guerra. Cada preset trae su objetivo
	 * de caidas, asi que el boton muestra los dos numeros juntos.
	 *
	 * <p>Los dos clanes tienen que pactar la misma, igual que el monto: aceptar un duelo
	 * de 30 minutos no puede arrancar uno de una semana.
	 */
	public static void abrirElegirDuracion(Player jugador, String nombreRival, int monto) {
		Team clan = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (clan == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Duracion", "contra " + limpiar(nombreRival));

		inv.setItem(CABECERA, boton(Material.CLOCK, "Cuanto dura",
				CUERPO + "Termina antes si un clan llega al",
				CUERPO + "objetivo de caidas.",
				"",
				CUERPO + "El otro clan tiene que aceptar la misma",
				CUERPO + "duracion para que arranque."));

		// De menor a mayor compromiso: el color dice solo que lo de abajo es mas serio.
		Material[] iconos = {Material.LIME_DYE, Material.YELLOW_DYE, Material.ORANGE_DYE, Material.RED_DYE};
		List<DueloManager.Duracion> duraciones = manager.getDuraciones();
		for (int i = 0; i < duraciones.size() && i < FILA_B.length; i++) {
			DueloManager.Duracion duracion = duraciones.get(i);
			inv.setItem(FILA_B[i], boton(iconos[Math.min(i, iconos.length - 1)], duracion.etiqueta,
					CUERPO + "Objetivo: " + MARCA + duracion.bajas + CUERPO + " caidas del rival.",
					"", ETIQUETA + "Clic para mandar el desafio"));
			holder.asignar(FILA_B[i], j -> abrirConfirmacion(j, "Desafiar",
					new String[]{
							CUERPO + "Le mandas el desafio a " + MARCA + limpiar(nombreRival) + CUERPO + ".",
							CUERPO + "Apuesta: " + MARCA + "$" + monto + CUERPO + " cada uno.",
							CUERPO + "Dura: " + MARCA + duracion.etiqueta + CUERPO + ", a "
									+ MARCA + duracion.bajas + CUERPO + " caidas.",
							"",
							CUERPO + "No pasa nada hasta que ellos acepten."
					}, "team duelo " + nombreRival + " " + monto + " " + duracion.id,
					() -> abrirElegirDuracion(j, nombreRival, monto)));
		}

		volverA(inv, holder, j -> abrirElegirApuesta(j, nombreRival));
		jugador.openInventory(inv);
	}

	// -------------------------------------------------------------------- lista

	public static void abrirLista(Player jugador, int pagina, String filtro) {
		// Ordenar puede recorrer todos los clanes: se hace fuera del hilo del tick.
		Main.plugin.getFoliaLib().getScheduler().runAsync(t -> {
			List<Team> clanes = buscar(filtro);
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador,
					t2 -> jugador.openInventory(construirLista(jugador, clanes, pagina, filtro)));
		});
	}

	private static Inventory construirLista(Player jugador, List<Team> clanes, int pagina, String filtro) {
		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(clanes.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Explorar clanes",
				(actual + 1) + "/" + paginas + (filtro == null ? "" : "  filtro: " + filtro));
		vaciarContenido(inv);

		Team propio = Team.getTeam(jugador);
		int desde = actual * porPagina;
		for (int i = desde; i < clanes.size() && i - desde < porPagina; i++) {
			Team clan = clanes.get(i);
			int slot = CONTENIDO[i - desde];
			inv.setItem(slot, itemClan(clan, propio));
			String nombre = clan.getName();
			holder.asignar(slot, j -> abrirFicha(j, nombre, () -> abrirLista(j, actual, filtro)));
		}

		if (clanes.isEmpty()) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "No hay nada",
					filtro == null ? "Todavia no hay ningun clan." : "Ningun clan coincide con " + filtro));
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirLista(j, actual - 1, filtro));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirLista(j, actual + 1, filtro));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
				CUERPO + "Buscar por nombre: " + ETIQUETA + "/team menu <texto>"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		return inv;
	}

	private static List<Team> buscar(String filtro) {
		List<Team> encontrados = new ArrayList<>();
		String aguja = filtro == null ? null : filtro.toLowerCase(Locale.ROOT);
		for (String nombre : Team.getTeamManager().sortTeamsByMembers()) {
			if (aguja != null && !nombre.toLowerCase(Locale.ROOT).contains(aguja)) {
				continue;
			}
			Team clan = Team.getTeam(nombre);
			if (clan != null) {
				encontrados.add(clan);
			}
		}
		return encontrados;
	}

	/**
	 * El material dice de un vistazo si podes entrar, que es para lo que se abre
	 * este menu. No se usan cabezas: pedir una por clan obliga a resolver perfiles,
	 * y con online-mode apagado ademas salen mal.
	 */
	private static ItemStack itemClan(Team clan, Team propio) {
		Material material;
		if (propio != null && propio.getID().equals(clan.getID())) {
			material = Material.LIME_WOOL;
		} else if (clan.isOpen()) {
			material = Material.LIGHT_BLUE_WOOL;
		} else {
			material = Material.GRAY_WOOL;
		}
		List<String> lore = new ArrayList<>(Arrays.asList(datosClan(clan)));
		lore.add("");
		lore.add(ETIQUETA + "Clic para ver la ficha");
		return boton(material, limpiar(clan.getName()), lore.toArray(new String[0]));
	}

	private static String[] datosClan(Team clan) {
		return new String[]{
				CUERPO + "Miembros: " + MARCA + clan.getMembers().getOnlinePlayers().size()
						+ CUERPO + "/" + MARCA + clan.getMembers().size() + CUERPO + " conectados",
				CUERPO + "Puntaje: " + MARCA + clan.getScore(),
				clan.isOpen() ? MARCA + "Abierto para todos" : CUERPO + "Solo por invitacion"
		};
	}

	// -------------------------------------------------------------------- ficha

	public static void abrirFicha(Player jugador, String nombreClan, Runnable volver) {
		Team clan = Team.getTeam(nombreClan);
		if (clan == null) {
			mensaje(jugador, ERROR + "Ese clan ya no existe.");
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, limpiar(clan.getName()), "ficha del clan");

		List<String> datos = new ArrayList<>(Arrays.asList(datosClan(clan)));
		String tag = clan.getTag();
		if (tag != null && !tag.isEmpty()) {
			datos.add(0, CUERPO + "Etiqueta: " + MARCA + limpiar(tag));
		}
		String descripcion = clan.getDescription();
		if (descripcion != null && !descripcion.isEmpty()) {
			datos.add("");
			datos.add(CUERPO + limpiar(descripcion));
		}
		inv.setItem(20, boton(Material.BOOK, limpiar(clan.getName()), datos.toArray(new String[0])));

		inv.setItem(22, boton(Material.PLAYER_HEAD, "Miembros",
				CUERPO + "Conectados: " + MARCA + clan.getMembers().getOnlinePlayersString(),
				CUERPO + "Desconectados: " + CUERPO + clan.getMembers().getOfflinePlayersString()));

		Team propio = Team.getTeam(jugador);
		if (propio != null && propio.getID().equals(clan.getID())) {
			inv.setItem(24, boton(Material.LIME_DYE, "Este es tu clan"));
		} else if (propio != null) {
			// Con clan propio lo util no es unirse sino proponer alianza.
			if (tieneMando(propio, jugador) && !propio.isAlly(clan)) {
				inv.setItem(24, boton(Material.SHIELD, "Proponer alianza",
						CUERPO + "Le manda la propuesta a " + MARCA + limpiar(clan.getName()) + CUERPO + ".",
						CUERPO + "Se sella cuando ellos la piden de vuelta."));
				holder.asignar(24, j -> ejecutarYVolver(j, "team ally " + clan.getName(),
						() -> abrirFicha(j, nombreClan, volver)));
			} else if (propio.isAlly(clan)) {
				inv.setItem(24, boton(Material.SHIELD, "Ya son aliados"));
			} else {
				inv.setItem(24, boton(Material.BARRIER, "Ya estas en un clan",
						CUERPO + "Solo el mando puede proponer alianzas."));
			}
		} else if (clan.isOpen()) {
			inv.setItem(24, boton(Material.LIME_DYE, "Unirte a este clan",
					ETIQUETA + "Clic para entrar"));
			// Se delega en el comando: permisos, limites, baneos y cooldowns siguen valiendo.
			holder.asignar(24, j -> comando(j, "team join " + clan.getName()));
		} else {
			inv.setItem(24, boton(Material.GRAY_DYE, "Solo por invitacion",
					CUERPO + "Alguien del clan tiene que invitarte."));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, j -> volver.run());
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ----------------------------------------------------------------- miembros

	public static void abrirMiembros(Player jugador, int pagina) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		List<TeamPlayer> miembros = new ArrayList<>(clan.getMembers().getClone());
		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(miembros.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Miembros", (actual + 1) + "/" + paginas);
		vaciarContenido(inv);

		boolean mando = tieneMando(clan, jugador);
		int desde = actual * porPagina;
		for (int i = desde; i < miembros.size() && i - desde < porPagina; i++) {
			TeamPlayer miembro = miembros.get(i);
			boolean conectado = miembro.getPlayer() != null && miembro.getPlayer().isOnline();
			String nombre = miembro.getPlayer() == null ? "?" : miembro.getPlayer().getName();
			boolean yoMismo = nombre.equals(jugador.getName());
			inv.setItem(CONTENIDO[i - desde], boton(
					conectado ? Material.PLAYER_HEAD : Material.SKELETON_SKULL,
					(conectado ? MARCA : CUERPO) + nombre,
					CUERPO + "Rango: " + MARCA + rango(miembro.getRank()),
					conectado ? MARCA + "Conectado" : CUERPO + "Desconectado",
					"",
					mando && !yoMismo ? ETIQUETA + "Clic para administrarlo"
							: CUERPO + (yoMismo ? "Sos vos." : "")));
			if (mando && !yoMismo) {
				holder.asignar(CONTENIDO[i - desde], j -> abrirMiembro(j, nombre));
			}
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirMiembros(j, actual - 1));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirMiembros(j, actual + 1));
		}
		if (mando) {
			inv.setItem(SLOT_CERRAR - 3, boton(Material.WRITABLE_BOOK, "Invitar gente",
					CUERPO + "Elegi de los que estan conectados."));
			holder.asignar(SLOT_CERRAR - 3, j -> abrirInvitar(j, 0));
		}
		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------ aliados

	public static void abrirAliados(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Aliados", limpiar(clan.getName()));
		vaciarContenido(inv);

		int i = 0;
		for (UUID id : clan.getAllies().getClone()) {
			if (i >= CONTENIDO.length) {
				break;
			}
			Team aliado = Team.getTeam(id);
			if (aliado == null) {
				continue;
			}
			inv.setItem(CONTENIDO[i], boton(Material.SHIELD, limpiar(aliado.getName()), datosClan(aliado)));
			String nombre = aliado.getName();
			holder.asignar(CONTENIDO[i], j -> abrirFicha(j, nombre, () -> abrirAliados(j)));
			i++;
		}
		if (i == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Sin aliados",
					CUERPO + "Proponer alianza: " + ETIQUETA + "/team ally <clan>",
					CUERPO + "Se sella cuando el otro clan la pide de vuelta."));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
				CUERPO + "Proponer alianza: " + ETIQUETA + "/team ally <clan>"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------ ranking

	public static void abrirRanking(Player jugador) {
		Main.plugin.getFoliaLib().getScheduler().runAsync(t -> {
			String[] orden = Team.getTeamManager().sortTeamsByScore();
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador, t2 -> {
				MenuHolder holder = new MenuHolder();
				Inventory inv = crear(holder, "Ranking", "por puntaje");
				vaciarContenido(inv);

				for (int i = 0; i < orden.length && i < CONTENIDO.length; i++) {
					Team clan = Team.getTeam(orden[i]);
					if (clan == null) {
						continue;
					}
					Material material = i == 0 ? Material.NETHER_STAR
							: i < 3 ? Material.DIAMOND : Material.IRON_INGOT;
					inv.setItem(CONTENIDO[i], boton(material,
							"#" + (i + 1) + "  " + limpiar(clan.getName()), datosClan(clan)));
					String nombre = clan.getName();
					holder.asignar(CONTENIDO[i], j -> abrirFicha(j, nombre, () -> abrirRanking(j)));
				}
				if (orden.length == 0) {
					inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Todavia no hay clanes"));
				}

				inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
						CUERPO + "El puntaje se reinicia el 1 de cada mes."));
				holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
				cerrar(inv, holder);
				jugador.openInventory(inv);
			});
		});
	}

	// ------------------------------------------------------------------ ajustes

	public static void abrirAjustes(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Ajustes", limpiar(clan.getName()));

		inv.setItem(20, boton(clan.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
				clan.isOpen() ? "Clan abierto" : "Clan cerrado",
				clan.isOpen() ? CUERPO + "Cualquiera puede entrar solo."
						: CUERPO + "Solo se entra por invitacion.",
				"", ETIQUETA + "Clic para cambiarlo"));
		holder.asignar(20, j -> {
			j.performCommand("team open");
			abrirAjustes(j);
		});

		inv.setItem(22, boton(Material.MAGENTA_DYE, "Color del clan",
				CUERPO + "Elegilo de una paleta."));
		holder.asignar(22, Menus::abrirColores);

		inv.setItem(24, boton(Material.NAME_TAG, "Nombre, etiqueta y descripcion",
				CUERPO + "Como se llama el clan y con que",
				CUERPO + "etiqueta aparece en el chat."));
		holder.asignar(24, Menus::abrirIdentidad);

		inv.setItem(29, boton(Material.PAPER, "Chat del clan",
				CUERPO + "Prende o apaga el chat interno."));
		holder.asignar(29, j -> comando(j, "team chat"));

		inv.setItem(31, boton(Material.RED_BED, "Fijar la casa aca",
				CUERPO + "Deja la casa del clan donde estas parado."));
		holder.asignar(31, j -> ejecutarYVolver(j, "team sethome", () -> abrirAjustes(j)));

		inv.setItem(33, boton(Material.TNT, ERROR + "Disolver el clan",
				CUERPO + "Borra el clan para todos.",
				CUERPO + "Se pierde el cofre y el banco."));
		// "disband confirm" en un solo tiro: el comando pelado abre su propia
		// confirmacion por chat, que despues de una pantalla de confirmacion sobra.
		holder.asignar(33, j -> abrirConfirmacion(j, "Disolver el clan",
				new String[]{
						CUERPO + "Se borra " + MARCA + limpiar(clan.getName()) + CUERPO + " para todos.",
						ERROR + "Se pierden los items del cofre y la plata del banco."
				}, "team disband confirm", () -> abrirAjustes(j)));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ---------------------------------------------------------------- identidad

	/**
	 * Nombre, etiqueta y descripcion: lo unico del clan que es texto libre.
	 *
	 * <p>Antes esta pantalla se limitaba a dictar los tres comandos. Ahora los pide por
	 * chat y vuelve sola, que es la regla de la casa: se elige, y lo unico que se
	 * escribe es lo que no se puede ofrecer como opcion.
	 *
	 * <p>La <b>etiqueta</b> es lo unico del clan que entra al chat, y por eso esta
	 * acotada a {@code maxTagLength}: el nombre completo va al tab y al nombre flotante,
	 * donde sobra lugar. Un clan nuevo ya nace con una derivada del nombre, asi que esto
	 * es para cambiarla, no para ponerla.
	 */
	public static void abrirIdentidad(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Identidad", limpiar(clan.getName()));

		int max = Main.plugin.getConfig().getInt("maxTagLength", 4);

		inv.setItem(FILA_A[1], boton(Material.NAME_TAG, "Nombre",
				CUERPO + "Ahora: " + MARCA + limpiar(clan.getName()),
				CUERPO + "Se ve en el tab y sobre la cabeza.",
				"", ETIQUETA + "Clic para cambiarlo"));
		holder.asignar(FILA_A[1], j -> pedirTexto(j,
				"Escribi el nombre nuevo del clan.", "team name", true));

		inv.setItem(FILA_A[3], boton(Material.OAK_SIGN, "Etiqueta",
				CUERPO + "Ahora: " + MARCA + etiquetaActual(clan),
				CUERPO + "Es lo que sale en el chat, hasta " + MARCA + max + CUERPO + " letras.",
				"", ETIQUETA + "Clic para cambiarla"));
		holder.asignar(FILA_A[3], j -> pedirTexto(j,
				"Escribi la etiqueta nueva, hasta " + max + " caracteres.", "team tag", true));

		inv.setItem(FILA_A[5], boton(Material.BOOK, "Descripcion",
				CUERPO + "Ahora: " + MARCA + descripcionActual(clan),
				"", ETIQUETA + "Clic para cambiarla"));
		holder.asignar(FILA_A[5], j -> pedirTexto(j,
				"Escribi la descripcion del clan.", "team description", false));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirAjustes);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	/**
	 * Pide el texto por chat, ejecuta el comando y vuelve a la misma pantalla.
	 *
	 * <p>Con {@code unaPalabra} los espacios pasan a guion bajo: {@code /team name} y
	 * {@code /team tag} toman un solo argumento y si no se quedarian con la primera
	 * palabra sin avisar. La descripcion si acepta varias.
	 */
	private static void pedirTexto(Player jugador, String consigna, String comando, boolean unaPalabra) {
		EsperaTexto.pedir(jugador, consigna,
				texto -> {
					jugador.performCommand(comando + " " + (unaPalabra ? texto.replace(' ', '_') : texto));
					abrirIdentidad(jugador);
				},
				Menus::abrirIdentidad);
	}

	private static String etiquetaActual(Team clan) {
		String tag = clan.getOriginalTag();
		return tag == null || tag.isEmpty() ? "sin etiqueta" : limpiar(tag);
	}

	private static String descripcionActual(Team clan) {
		String desc = clan.getDescription();
		if (desc == null || desc.isEmpty()) {
			return "sin descripcion";
		}
		String limpio = limpiar(desc);
		return limpio.length() > 30 ? limpio.substring(0, 30) + "..." : limpio;
	}

	// --------------------------------------------------- acciones sobre un miembro

	public static void abrirMiembro(Player jugador, String nombreMiembro) {
		Team clan = Team.getTeam(jugador);
		if (clan == null || !tieneMando(clan, jugador)) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, limpiar(nombreMiembro), "miembro del clan");

		inv.setItem(20, boton(Material.EMERALD, "Ascender",
				CUERPO + "Le da un escalon mas de mando."));
		holder.asignar(20, j -> ejecutarYVolver(j, "team promote " + nombreMiembro,
				() -> abrirMiembro(j, nombreMiembro)));

		inv.setItem(22, boton(Material.REDSTONE, "Bajar de rango",
				CUERPO + "Le saca un escalon de mando."));
		holder.asignar(22, j -> ejecutarYVolver(j, "team demote " + nombreMiembro,
				() -> abrirMiembro(j, nombreMiembro)));

		inv.setItem(24, boton(Material.IRON_DOOR, ERROR + "Expulsar del clan",
				CUERPO + "Lo saca del clan."));
		holder.asignar(24, j -> abrirConfirmacion(j, "Expulsar a " + limpiar(nombreMiembro),
				new String[]{
						CUERPO + "Se va del clan y pierde el acceso",
						CUERPO + "al cofre y al banco."
				}, "team kick " + nombreMiembro, () -> abrirMiembros(j, 0)));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, j -> abrirMiembros(j, 0));
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ----------------------------------------------------------------- invitar

	public static void abrirInvitar(Player jugador, int pagina) {
		Team clan = Team.getTeam(jugador);
		if (clan == null || !tieneMando(clan, jugador)) {
			abrirPortada(jugador);
			return;
		}
		// Solo los conectados: invitar por lista evita tener que escribir el nombre,
		// que es lo unico que un menu no puede pedir bien desde Bedrock.
		List<Player> candidatos = new ArrayList<>();
		for (Player otro : Bukkit.getOnlinePlayers()) {
			if (Team.getTeam(otro) == null) {
				candidatos.add(otro);
			}
		}

		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(candidatos.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Invitar", (actual + 1) + "/" + paginas);
		vaciarContenido(inv);

		int desde = actual * porPagina;
		for (int i = desde; i < candidatos.size() && i - desde < porPagina; i++) {
			String nombre = candidatos.get(i).getName();
			inv.setItem(CONTENIDO[i - desde], boton(Material.PLAYER_HEAD, MARCA + nombre,
					CUERPO + "Sin clan",
					"", ETIQUETA + "Clic para invitarlo"));
			holder.asignar(CONTENIDO[i - desde], j -> ejecutarYVolver(j, "team invite " + nombre,
					() -> abrirInvitar(j, actual)));
		}
		if (candidatos.isEmpty()) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "No hay a quien invitar",
					CUERPO + "Todos los conectados ya tienen clan."));
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirInvitar(j, actual - 1));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirInvitar(j, actual + 1));
		}
		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
				CUERPO + "A alguien desconectado: " + ETIQUETA + "/team invite <jugador>"));
		holder.asignar(SLOT_VOLVER, j -> abrirMiembros(j, 0));
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------- banco

	private static final int[] MONTOS = {100, 1000, 10000};

	public static void abrirBanco(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Banco", "$" + clan.getBalance());

		inv.setItem(13, boton(Material.GOLD_BLOCK, "Saldo del clan",
				CUERPO + "Hay " + MARCA + "$" + clan.getBalance() + CUERPO + " en el banco."));

		// Montos fijos: es la unica forma de mover plata sin pedir que escriban.
		int slot = 19;
		for (int monto : MONTOS) {
			inv.setItem(slot, boton(Material.GOLD_NUGGET, "Poner $" + monto,
					CUERPO + "Saca " + MARCA + "$" + monto + CUERPO + " de lo tuyo",
					CUERPO + "y lo pone en el banco del clan."));
			int copia = monto;
			holder.asignar(slot, j -> ejecutarYVolver(j, "team deposit " + copia, () -> abrirBanco(j)));
			slot++;
		}

		slot = 25;
		for (int i = MONTOS.length - 1; i >= 0; i--) {
			int monto = MONTOS[i];
			inv.setItem(slot, boton(Material.GOLD_INGOT, "Sacar $" + monto,
					CUERPO + "Saca " + MARCA + "$" + monto + CUERPO + " del banco",
					CUERPO + "y te lo pone a vos."));
			holder.asignar(slot, j -> ejecutarYVolver(j, "team withdraw " + monto, () -> abrirBanco(j)));
			slot--;
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
				CUERPO + "Otro monto: " + ETIQUETA + "/team deposit <monto>"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------- warps

	public static void abrirWarps(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Warps", limpiar(clan.getName()));
		vaciarContenido(inv);

		int i = 0;
		for (com.booksaw.betterTeams.Warp warp : clan.getWarps().getClone()) {
			if (i >= CONTENIDO.length) {
				break;
			}
			inv.setItem(CONTENIDO[i], boton(Material.LODESTONE, limpiar(warp.getName()),
					CUERPO + "Mundo: " + MARCA + (warp.getLocation() == null || warp.getLocation().getWorld() == null
							? "?" : warp.getLocation().getWorld().getName()),
					"", ETIQUETA + "Clic para viajar"));
			String nombre = warp.getName();
			holder.asignar(CONTENIDO[i], j -> comando(j, "team warp " + nombre));
			i++;
		}
		if (i == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Sin warps",
					CUERPO + "Crear uno: " + ETIQUETA + "/team setwarp <nombre>"));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
				CUERPO + "Crear uno: " + ETIQUETA + "/team setwarp <nombre>"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ----------------------------------------------------------------- colores

	/** Los 16 de siempre, que es lo que acepta /team color. */
	private static final String[][] COLORES = {
			{"dark_red", "Rojo oscuro", "RED_WOOL"}, {"red", "Rojo", "PINK_WOOL"},
			{"gold", "Dorado", "ORANGE_WOOL"}, {"yellow", "Amarillo", "YELLOW_WOOL"},
			{"dark_green", "Verde oscuro", "GREEN_WOOL"}, {"green", "Verde", "LIME_WOOL"},
			{"aqua", "Celeste", "LIGHT_BLUE_WOOL"}, {"dark_aqua", "Turquesa", "CYAN_WOOL"},
			{"dark_blue", "Azul oscuro", "BLUE_WOOL"}, {"blue", "Azul", "LIGHT_BLUE_WOOL"},
			{"light_purple", "Rosa", "MAGENTA_WOOL"}, {"dark_purple", "Violeta", "PURPLE_WOOL"},
			{"white", "Blanco", "WHITE_WOOL"}, {"gray", "Gris", "LIGHT_GRAY_WOOL"},
			{"dark_gray", "Gris oscuro", "GRAY_WOOL"}, {"black", "Negro", "BLACK_WOOL"}
	};

	public static void abrirColores(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Color del clan", limpiar(clan.getName()));
		vaciarContenido(inv);

		for (int i = 0; i < COLORES.length && i < CONTENIDO.length; i++) {
			String codigo = COLORES[i][0];
			Material material = Material.matchMaterial(COLORES[i][2]);
			inv.setItem(CONTENIDO[i], boton(material == null ? Material.WHITE_WOOL : material,
					COLORES[i][1], "", ETIQUETA + "Clic para usarlo"));
			holder.asignar(CONTENIDO[i], j -> ejecutarYVolver(j, "team color " + codigo,
					() -> abrirColores(j)));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirAjustes);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------ confirmacion

	/**
	 * Pantalla de confirmacion para lo que no tiene vuelta atras.
	 *
	 * <p>Verde es SI y rojo es NO, que es como se lee en un menu de Minecraft. La
	 * version anterior los tenia al reves —verde para cancelar, rojo para la accion
	 * destructiva, que es la convencion de escritorio— y en el juego se leia
	 * invertido.
	 *
	 * <p>Y la posicion es siempre la misma: NO a la izquierda, SI a la derecha. Ver
	 * {@link #SLOT_NO} y {@link #SLOT_SI}.
	 */
	public static void abrirConfirmacion(Player jugador, String titulo, String[] aviso,
			String comando, Runnable volver) {
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, titulo, "confirmar");

		List<String> lore = new ArrayList<>(Arrays.asList(aviso));
		lore.add("");
		lore.add(ERROR + "Esto no se puede deshacer.");
		inv.setItem(13, boton(Material.PAPER, ERROR + titulo, lore.toArray(new String[0])));

		inv.setItem(SLOT_NO, boton(Material.RED_DYE, ERROR + "No, volver",
				CUERPO + "No pasa nada."));
		holder.asignar(SLOT_NO, j -> volver.run());

		inv.setItem(SLOT_SI, boton(Material.LIME_DYE, "Si, hacerlo",
				CUERPO + "Se hace ahora, de una."));
		holder.asignar(SLOT_SI, j -> comando(j, comando));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, j -> volver.run());
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------- comun

	private static Inventory crear(MenuHolder holder, String titulo, String sub) {
		String nombre = col("&#9235FF&l" + titulo + (sub == null ? "" : "  &#E4D9FF" + sub));
		Inventory inv = Bukkit.createInventory(holder, TAM, nombre);
		holder.setInventario(inv);

		// Dos tonos: el marco mas oscuro y el interior mas claro. Se lee donde
		// termina el marco y donde empieza el contenido.
		ItemStack borde = boton(Material.GRAY_STAINED_GLASS_PANE, " ");
		ItemStack fondo = boton(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
		for (int i = 0; i < TAM; i++) {
			inv.setItem(i, borde);
		}
		for (int slot : CONTENIDO) {
			inv.setItem(slot, fondo);
		}
		return inv;
	}

	private static void vaciarContenido(Inventory inv) {
		for (int slot : CONTENIDO) {
			inv.setItem(slot, null);
		}
	}

	/** Pone el boton de volver, su destino, y el de cerrar. */
	private static void volverA(Inventory inv, MenuHolder holder, Consumer<Player> destino) {
		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, destino);
		cerrar(inv, holder);
	}

	/**
	 * Sin decimales cuando el monto es redondo, y con {@link Locale#ROOT} siempre.
	 *
	 * <p>Este texto termina adentro de un comando, y el separador decimal del
	 * sistema —coma, en espaniol— no lo acepta el parser.
	 */
	private static String fmt(double monto) {
		return monto == Math.floor(monto) && !Double.isInfinite(monto)
				? String.valueOf((long) monto)
				: String.format(Locale.ROOT, "%.2f", monto);
	}

	private static void cerrar(Inventory inv, MenuHolder holder) {
		inv.setItem(SLOT_CERRAR, boton(Material.BARRIER, ERROR + "Cerrar"));
		holder.asignar(SLOT_CERRAR, Player::closeInventory);
	}

	private static ItemStack boton(Material material, String nombre, String... lore) {
		ItemStack pila = new ItemStack(material);
		ItemMeta meta = pila.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(col(nombre.startsWith("&") ? nombre : MARCA + nombre));
			if (lore.length > 0) {
				List<String> lineas = new ArrayList<>();
				for (String linea : lore) {
					lineas.add(col(linea.isEmpty() || linea.startsWith("&") ? linea : CUERPO + linea));
				}
				meta.setLore(lineas);
			}
			pila.setItemMeta(meta);
		}
		return pila;
	}

	/**
	 * Convierte a color de verdad. Son tres pasos y hacen falta los tres.
	 *
	 * <p>1) {@code toAdventure} pasa los codigos {@code &#RRGGBB} y {@code &c} a
	 * etiquetas MiniMessage. 2) el formateador las convierte en componente, que es
	 * lo que tambien resuelve el MiniMessage que ya traen los nombres de clan.
	 * 3) se serializa a legacy con {@code hexColors()}.
	 *
	 * <p>🔑 <b>El tercer paso no puede usar {@code LegacyTextUtils.parseAllAdventure}</b>:
	 * ese serializa sin hex, asi que aplasta cada color al legacy mas parecido y toda
	 * la paleta termina saliendo del mismo azul. Comprobado.
	 */
	static String col(String texto) {
		return Texto.col(texto);
	}

	/** Saca el color de un texto, para usarlo dentro de otro que ya tiene color. */
	private static String limpiar(String texto) {
		return Texto.limpiar(texto);
	}

	private static String rango(PlayerRank rango) {
		if (rango == PlayerRank.OWNER) {
			return "Duenio";
		}
		if (rango == PlayerRank.ADMIN) {
			return "Mando";
		}
		return "Miembro";
	}

	/** Cierra el menu y ejecuta. Para lo que saca al jugador de la pantalla. */
	private static void comando(Player jugador, String comando) {
		jugador.closeInventory();
		if (seguro(jugador, comando)) {
			jugador.performCommand(comando);
		}
	}

	/**
	 * Que el comando armado sea un solo comando.
	 *
	 * <p>Los botones interpolan nombres de clan y de jugador en una linea de
	 * comando. Hoy no hay riesgo porque {@code allowedChars} deja los nombres en
	 * alfanumerico puro, pero eso es una config que alguien puede vaciar —su propio
	 * comentario dice que en blanco permite todo—, y ahi un nombre con espacios
	 * correria los argumentos: un clan llamado "x 999999" convertiria una apuesta de
	 * 1 en otra cosa. Se valida al armar, no se confia en la config.
	 */
	private static boolean seguro(Player jugador, String comando) {
		if (comando.indexOf('\n') >= 0 || comando.indexOf('\r') >= 0) {
			mensaje(jugador, ERROR + "Ese nombre tiene caracteres que no se pueden usar.");
			return false;
		}
		return true;
	}

	/** Un argumento que se interpola en un comando: sin espacios ni saltos. */
	private static boolean argumentoSeguro(String valor) {
		if (valor == null || valor.isEmpty()) {
			return false;
		}
		for (int i = 0; i < valor.length(); i++) {
			if (Character.isWhitespace(valor.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Ejecuta sin cerrar y vuelve a dibujar la pantalla, para que se vea el efecto.
	 *
	 * <p>Se delega en el comando a proposito: permisos, limites, baneos, costos y
	 * cooldowns siguen valiendo, y el jugador recibe el mensaje de error del propio
	 * plugin si algo no se puede.
	 */
	private static void ejecutarYVolver(Player jugador, String comando, Runnable volverADibujar) {
		if (seguro(jugador, comando)) {
			jugador.performCommand(comando);
		}
		volverADibujar.run();
	}

	/** Si puede administrar el clan. La palabra final la tiene igual el comando. */
	private static boolean tieneMando(Team clan, Player jugador) {
		TeamPlayer yo = clan.getTeamPlayer(jugador);
		return yo != null && yo.getRank() != PlayerRank.DEFAULT;
	}

	private static void mensaje(Player jugador, String texto) {
		jugador.sendMessage(col(ETIQUETA + "[Clanes] " + CUERPO + texto));
	}
}
