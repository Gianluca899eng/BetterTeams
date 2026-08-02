package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.text.Formatter;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

	// Paleta de Luniatic.
	private static final String MARCA = "&#9235FF";
	private static final String CUERPO = "&#E4D9FF";
	private static final String ETIQUETA = "&#7162FF";
	private static final String ERROR = "&#FF4554";

	/**
	 * Con hexColors(), o la paleta se aplasta a los 16 colores viejos. Y con el
	 * formato "x repetida" ({@code §x§9§2§3§5§F§F}), que es el unico que entiende
	 * el cliente: el {@code §#9235ff} que sale por defecto se muestra como texto.
	 */
	private static final LegacyComponentSerializer LEGACY_HEX = LegacyComponentSerializer.builder()
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	private Menus() {
	}

	// ------------------------------------------------------------------ portada

	public static void abrirPortada(Player jugador) {
		Team clan = Team.getTeam(jugador);
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Clanes", clan == null ? "sin clan" : limpiar(clan.getName()));

		if (clan == null) {
			inv.setItem(20, boton(Material.COMPASS, "Explorar clanes",
					"Mira todos los clanes del servidor", "y sumate a los que esten abiertos."));
			holder.asignar(20, j -> abrirLista(j, 0, null));

			inv.setItem(22, boton(Material.WRITABLE_BOOK, "Crear tu clan",
					"Escribi " + ETIQUETA + "/team create <nombre>",
					"", CUERPO + "El nombre no se puede repetir."));
			holder.asignar(22, j -> {
				j.closeInventory();
				mensaje(j, "Para crear tu clan escribi " + ETIQUETA + "/team create <nombre>");
			});

			inv.setItem(24, boton(Material.NETHER_STAR, "Ranking",
					"Los clanes con mas puntaje."));
			holder.asignar(24, j -> abrirRanking(j));

			inv.setItem(31, boton(Material.PAPER, "Como funciona",
					"Un clan te da chat propio, cofre compartido,",
					"casa del clan y banco en comun.",
					"",
					CUERPO + "Si te invitaron, entra con " + ETIQUETA + "/team join <clan>"));
		} else {
			TeamPlayer yo = clan.getTeamPlayer(jugador);
			boolean mando = yo != null && yo.getRank() != PlayerRank.DEFAULT;

			inv.setItem(19, boton(Material.BOOK, "Mi clan", datosClan(clan)));
			holder.asignar(19, j -> abrirFicha(j, clan.getName(), () -> abrirPortada(j)));

			inv.setItem(20, boton(Material.PLAYER_HEAD, "Miembros",
					CUERPO + "Conectados: " + MARCA + clan.getMembers().getOnlinePlayers().size()
							+ CUERPO + " de " + MARCA + clan.getMembers().size(),
					"", ETIQUETA + "Clic para ver la lista"));
			holder.asignar(20, j -> abrirMiembros(j, 0));

			inv.setItem(21, boton(Material.ENDER_CHEST, "Cofre del clan",
					"El cofre compartido de todo el clan."));
			holder.asignar(21, j -> comando(j, "team echest"));

			inv.setItem(22, boton(Material.GOLD_INGOT, "Banco del clan",
					CUERPO + "Saldo: " + MARCA + "$" + clan.getBalance(),
					"",
					ETIQUETA + "/team deposit <monto>" + CUERPO + " para poner",
					ETIQUETA + "/team withdraw <monto>" + CUERPO + " para sacar"));
			holder.asignar(22, j -> {
				j.closeInventory();
				mensaje(j, "Saldo del clan: " + MARCA + "$" + clan.getBalance());
				mensaje(j, "Para mover plata: " + ETIQUETA + "/team deposit <monto>"
						+ CUERPO + " o " + ETIQUETA + "/team withdraw <monto>");
			});

			inv.setItem(23, boton(Material.RED_BED, "Casa del clan",
					"Te lleva a la casa del clan.",
					"",
					mando ? ETIQUETA + "/team sethome" + CUERPO + " la fija donde estas parado"
							: CUERPO + "Solo el mando puede moverla."));
			holder.asignar(23, j -> comando(j, "team home"));

			inv.setItem(24, boton(Material.OAK_SIGN, "Warps del clan",
					"Los puntos guardados del clan."));
			holder.asignar(24, j -> comando(j, "team warps"));

			inv.setItem(29, boton(Material.SHIELD, "Aliados",
					"Los clanes con los que no te pegas."));
			holder.asignar(29, j -> abrirAliados(j));

			inv.setItem(30, boton(Material.COMPASS, "Explorar clanes",
					"Mira todos los clanes del servidor."));
			holder.asignar(30, j -> abrirLista(j, 0, null));

			inv.setItem(31, boton(Material.NETHER_STAR, "Ranking",
					"Los clanes con mas puntaje."));
			holder.asignar(31, j -> abrirRanking(j));

			inv.setItem(32, boton(Material.COMPARATOR, "Ajustes del clan",
					mando ? "Abrir o cerrar el clan, chat, color y mas."
							: ERROR + "Solo el mando puede tocar esto."));
			holder.asignar(32, j -> abrirAjustes(j));

			inv.setItem(33, boton(Material.BARRIER, "Salir del clan",
					ERROR + "Te vas del clan.",
					"", ETIQUETA + "Clic para ver como"));
			holder.asignar(33, j -> {
				j.closeInventory();
				mensaje(j, ERROR + "Para salir del clan escribi " + ETIQUETA + "/team leave");
			});
		}

		cerrar(inv, holder);
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
			inv.setItem(24, boton(Material.BARRIER, "Ya estas en un clan",
					CUERPO + "Sali del tuyo antes de entrar a otro."));
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

		int desde = actual * porPagina;
		for (int i = desde; i < miembros.size() && i - desde < porPagina; i++) {
			TeamPlayer miembro = miembros.get(i);
			boolean conectado = miembro.getPlayer() != null && miembro.getPlayer().isOnline();
			String nombre = miembro.getPlayer() == null ? "?" : miembro.getPlayer().getName();
			inv.setItem(CONTENIDO[i - desde], boton(
					conectado ? Material.PLAYER_HEAD : Material.SKELETON_SKULL,
					(conectado ? MARCA : CUERPO) + nombre,
					CUERPO + "Rango: " + MARCA + rango(miembro.getRank()),
					conectado ? MARCA + "Conectado" : CUERPO + "Desconectado"));
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirMiembros(j, actual - 1));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirMiembros(j, actual + 1));
		}
		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver",
				CUERPO + "Invitar: " + ETIQUETA + "/team invite <jugador>",
				CUERPO + "Expulsar: " + ETIQUETA + "/team kick <jugador>"));
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

		inv.setItem(22, boton(Material.NAME_TAG, "Nombre, etiqueta y color",
				ETIQUETA + "/team name <nombre>",
				ETIQUETA + "/team tag <etiqueta>",
				ETIQUETA + "/team color <color>"));
		holder.asignar(22, j -> {
			j.closeInventory();
			mensaje(j, "Nombre: " + ETIQUETA + "/team name <nombre>");
			mensaje(j, "Etiqueta: " + ETIQUETA + "/team tag <etiqueta>");
			mensaje(j, "Color: " + ETIQUETA + "/team color <color>");
		});

		inv.setItem(24, boton(Material.WRITABLE_BOOK, "Descripcion",
				ETIQUETA + "/team description <texto>"));
		holder.asignar(24, j -> {
			j.closeInventory();
			mensaje(j, "Descripcion: " + ETIQUETA + "/team description <texto>");
		});

		inv.setItem(30, boton(Material.PAPER, "Chat del clan",
				CUERPO + "Prende o apaga el chat interno."));
		holder.asignar(30, j -> comando(j, "team chat"));

		inv.setItem(32, boton(Material.RED_BED, "Fijar la casa aca",
				CUERPO + "Deja la casa del clan donde estas parado."));
		holder.asignar(32, j -> comando(j, "team sethome"));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
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
		return LEGACY_HEX.serializeOr(Formatter.absolute().process(LegacyTextUtils.toAdventure(texto)), "");
	}

	/** Saca el color de un texto, para usarlo dentro de otro que ya tiene color. */
	private static String limpiar(String texto) {
		return org.bukkit.ChatColor.stripColor(col(texto));
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

	private static void comando(Player jugador, String comando) {
		jugador.closeInventory();
		jugador.performCommand(comando);
	}

	private static void mensaje(Player jugador, String texto) {
		jugador.sendMessage(col(ETIQUETA + "[Clanes] " + CUERPO + texto));
	}
}
