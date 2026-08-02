package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Menu para mirar y unirse a clanes.
 *
 * <p>Tres decisiones que conviene no revertir sin pensarlas:
 * <ul>
 * <li><b>No se refresca solo.</b> Se arma al abrirlo y muere al cerrarlo: cero
 * trabajo por tick, que es lo que P3 exige con dos nucleos.
 * <li><b>La busqueda entra por argumento del comando, no por yunque.</b> Los
 * menus de escritura tipo yunque son justo lo que peor anda por Geyser.
 * <li><b>Los botones ejecutan los comandos que ya existen</b> en vez de repetir
 * su logica, asi que permisos, limites, baneos y cooldowns siguen valiendo y el
 * merge con upstream sigue siendo barato.
 * </ul>
 */
public class MenuClanes {

	private static final int POR_PAGINA = 45;
	private static final int SLOT_ANTERIOR = 45;
	private static final int SLOT_INFO = 49;
	private static final int SLOT_SIGUIENTE = 53;
	private static final int SLOT_VOLVER = 22;
	private static final int SLOT_DETALLE_INFO = 11;
	private static final int SLOT_DETALLE_MIEMBROS = 13;
	private static final int SLOT_DETALLE_ACCION = 15;

	private MenuClanes() {
	}

	/** Arma la lista fuera del hilo del tick y la abre sobre el jugador. */
	public static void abrirLista(Player jugador, int pagina, String filtro) {
		Main.plugin.getFoliaLib().getScheduler().runAsync(tarea -> {
			List<Team> clanes = buscar(filtro);
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador,
					t -> jugador.openInventory(construirLista(jugador, clanes, pagina, filtro)));
		});
	}

	public static void abrirDetalle(Player jugador, String nombreClan) {
		Team clan = Team.getTeam(nombreClan);
		if (clan == null) {
			MessageManager.sendMessage(jugador, "noTeam");
			return;
		}
		jugador.openInventory(construirDetalle(jugador, clan));
	}

	private static List<Team> buscar(String filtro) {
		List<Team> encontrados = new ArrayList<>();
		String aguja = filtro == null ? null : filtro.toLowerCase(Locale.ROOT);
		// sortTeamsByMembers es el mismo orden que usa /team list.
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

	private static Inventory construirLista(Player jugador, List<Team> clanes, int pagina, String filtro) {
		int paginas = Math.max(1, (int) Math.ceil(clanes.size() / (double) POR_PAGINA));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder(MenuHolder.Tipo.LISTA, actual, filtro);
		Inventory inventario = Bukkit.createInventory(holder, 54,
				MessageManager.getMessage("menu.titulo", actual + 1, paginas));
		holder.setInventario(inventario);

		Team propio = Team.getTeam(jugador);

		int desde = actual * POR_PAGINA;
		for (int i = desde; i < clanes.size() && i - desde < POR_PAGINA; i++) {
			Team clan = clanes.get(i);
			int slot = i - desde;
			inventario.setItem(slot, itemClan(clan, propio));
			holder.asignar(slot, MenuHolder.Accion.ABRIR_DETALLE, clan.getName());
		}

		if (clanes.isEmpty()) {
			inventario.setItem(22, item(Material.BARRIER, MessageManager.getMessage("menu.vacio"), null));
		}

		if (actual > 0) {
			inventario.setItem(SLOT_ANTERIOR,
					item(Material.ARROW, MessageManager.getMessage("menu.anterior"), null));
			holder.asignar(SLOT_ANTERIOR, MenuHolder.Accion.PAGINA, String.valueOf(actual - 1));
		}
		if (actual < paginas - 1) {
			inventario.setItem(SLOT_SIGUIENTE,
					item(Material.ARROW, MessageManager.getMessage("menu.siguiente"), null));
			holder.asignar(SLOT_SIGUIENTE, MenuHolder.Accion.PAGINA, String.valueOf(actual + 1));
		}

		List<String> ayuda = new ArrayList<>();
		ayuda.add(MessageManager.getMessage("menu.ayuda_buscar"));
		if (filtro != null) {
			ayuda.add(MessageManager.getMessage("menu.filtro_activo", filtro));
		}
		ayuda.add(MessageManager.getMessage("menu.leyenda_propio"));
		ayuda.add(MessageManager.getMessage("menu.leyenda_abierto"));
		ayuda.add(MessageManager.getMessage("menu.leyenda_cerrado"));
		inventario.setItem(SLOT_INFO, item(Material.BOOK,
				MessageManager.getMessage("menu.info", clanes.size()), ayuda));

		return inventario;
	}

	/**
	 * El material dice de un vistazo si podes entrar, que es para lo que se abre
	 * este menu. No se usan cabezas: pedirlas por cada clan obliga a resolver
	 * perfiles, y con online-mode apagado ademas salen mal.
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

		List<String> descripcion = new ArrayList<>();
		descripcion.add(MessageManager.getMessage("menu.item.miembros",
				clan.getMembers().getOnlinePlayers().size(), clan.getMembers().size()));
		descripcion.add(MessageManager.getMessage("menu.item.puntaje", clan.getScore()));
		descripcion.add(clan.isOpen()
				? MessageManager.getMessage("menu.item.abierto")
				: MessageManager.getMessage("menu.item.cerrado"));
		descripcion.add("");
		descripcion.add(MessageManager.getMessage("menu.item.ver"));

		return item(material, clan.getDisplayName(), descripcion);
	}

	private static Inventory construirDetalle(Player jugador, Team clan) {
		MenuHolder holder = new MenuHolder(MenuHolder.Tipo.DETALLE, 0, null);
		Inventory inventario = Bukkit.createInventory(holder, 27,
				MessageManager.getMessage("menu.detalle.titulo", clan.getName()));
		holder.setInventario(inventario);

		List<String> datos = new ArrayList<>();
		String tag = clan.getTag();
		if (tag != null && !tag.isEmpty()) {
			datos.add(MessageManager.getMessage("menu.detalle.tag", tag));
		}
		String descripcion = clan.getDescription();
		if (descripcion != null && !descripcion.isEmpty()) {
			datos.add(MessageManager.getMessage("menu.detalle.descripcion", descripcion));
		}
		datos.add(MessageManager.getMessage("menu.item.puntaje", clan.getScore()));
		datos.add(clan.isOpen()
				? MessageManager.getMessage("menu.item.abierto")
				: MessageManager.getMessage("menu.item.cerrado"));
		inventario.setItem(SLOT_DETALLE_INFO,
				item(Material.BOOK, clan.getDisplayName(), datos));

		List<String> miembros = new ArrayList<>();
		miembros.add(MessageManager.getMessage("menu.detalle.conectados", clan.getMembers().getOnlinePlayersString()));
		miembros.add(MessageManager.getMessage("menu.detalle.desconectados", clan.getMembers().getOfflinePlayersString()));
		inventario.setItem(SLOT_DETALLE_MIEMBROS, item(Material.PLAYER_HEAD,
				MessageManager.getMessage("menu.detalle.miembros",
						clan.getMembers().getOnlinePlayers().size(), clan.getMembers().size()),
				miembros));

		Team propio = Team.getTeam(jugador);
		if (propio != null) {
			inventario.setItem(SLOT_DETALLE_ACCION, item(Material.BARRIER,
					MessageManager.getMessage("menu.detalle.ya_tenes_clan"), null));
		} else if (clan.isOpen()) {
			inventario.setItem(SLOT_DETALLE_ACCION, item(Material.LIME_DYE,
					MessageManager.getMessage("menu.detalle.unirse"), null));
			holder.asignar(SLOT_DETALLE_ACCION, MenuHolder.Accion.UNIRSE, clan.getName());
		} else {
			inventario.setItem(SLOT_DETALLE_ACCION, item(Material.GRAY_DYE,
					MessageManager.getMessage("menu.detalle.solo_invitacion"), null));
		}

		inventario.setItem(SLOT_VOLVER, item(Material.ARROW, MessageManager.getMessage("menu.volver"), null));
		holder.asignar(SLOT_VOLVER, MenuHolder.Accion.VOLVER, null);

		return inventario;
	}

	private static ItemStack item(Material material, String nombre, List<String> descripcion) {
		ItemStack pila = new ItemStack(material);
		ItemMeta meta = pila.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(nombre);
			if (descripcion != null) {
				meta.setLore(descripcion);
			}
			pila.setItemMeta(meta);
		}
		return pila;
	}
}
