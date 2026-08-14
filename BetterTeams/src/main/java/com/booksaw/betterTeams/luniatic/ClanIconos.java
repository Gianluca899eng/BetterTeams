package com.booksaw.betterTeams.luniatic;

import com.booksaw.betterTeams.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El icono con el que cada clan se muestra en el menu.
 *
 * <p>Existe porque explorar clanes era una lista de lana de tres colores: todos iguales
 * salvo por el nombre. El icono es lo unico que hace que un clan se reconozca de un vistazo
 * cuando hay veinte en pantalla.
 *
 * <p>🔑 <b>Se guarda en un archivo propio y no en el {@code teamInfo} del clan.</b> Meterlo
 * ahi obliga a tocar {@code StoredTeamValue} y sus tres implementaciones de storage, que son
 * de upstream: cada merge futuro costaria mas. Es la misma decision que ya se tomo con los
 * duelos.
 *
 * <p>La lista de iconos elegibles sale del config ({@code iconos}). Es una <b>lista blanca</b>
 * a proposito: sin ella alguien pone una barrera, un bloque de comandos o algo invisible, y
 * el menu de exploracion —que es una vidriera— queda roto.
 */
public class ClanIconos {

	private static final String ARCHIVO = "clan-iconos.yml";

	/** Lo que se usa si el config no trae la lista. Materiales comunes y reconocibles. */
	private static final String[] POR_DEFECTO = {
			"IRON_SWORD", "DIAMOND_SWORD", "NETHERITE_SWORD", "BOW", "SHIELD", "TRIDENT",
			"DIAMOND_PICKAXE", "IRON_PICKAXE", "GOLDEN_PICKAXE", "FISHING_ROD", "ELYTRA",
			"DIAMOND", "EMERALD", "GOLD_INGOT", "IRON_INGOT", "NETHERITE_INGOT", "AMETHYST_SHARD",
			"NETHER_STAR", "ENDER_EYE", "ENDER_PEARL", "BLAZE_ROD", "GHAST_TEAR", "HEART_OF_THE_SEA",
			"TOTEM_OF_UNDYING", "DRAGON_HEAD", "WITHER_SKELETON_SKULL", "CREEPER_HEAD", "ZOMBIE_HEAD",
			"BEACON", "CONDUIT", "END_CRYSTAL", "LODESTONE", "CAMPFIRE", "LANTERN", "SOUL_LANTERN",
			"OAK_SAPLING", "CHERRY_SAPLING", "SUNFLOWER", "POPPY", "CACTUS", "PUMPKIN", "HONEYCOMB",
			"BREAD", "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "CAKE", "COOKIE",
			"MUSIC_DISC_CAT", "PAINTING", "BOOK", "COMPASS", "CLOCK", "SPYGLASS", "BUNDLE",
			"WHITE_BANNER", "RED_BANNER", "BLUE_BANNER", "BLACK_BANNER", "YELLOW_BANNER"
	};

	private final Map<UUID, Material> iconos = new HashMap<>();
	private final List<Material> elegibles = new ArrayList<>();

	public ClanIconos() {
		leerElegibles();
		cargar();
	}

	/**
	 * El icono de ese clan.
	 *
	 * <p>🔑 <b>Sin icono guardado no devuelve el primero de la lista, sino uno derivado del
	 * id del clan.</b> El primero de la lista es el mismo para todos, o sea que la lista de
	 * exploracion volvia a ser lo que el icono vino a arreglar: veinte filas iguales. Asi,
	 * los clanes que ya existian desde antes de que esto se repartiera solo salen variados
	 * igual, sin escribir un archivo ni cargar un clan.
	 *
	 * <p>Es estable: el mismo clan cae siempre en el mismo icono.
	 */
	public Material getIcono(UUID clan) {
		Material elegido = clan == null ? null : iconos.get(clan);
		if (elegido != null) {
			return elegido;
		}
		if (elegibles.isEmpty()) {
			return Material.WHITE_BANNER;
		}
		if (clan == null) {
			return elegibles.get(0);
		}
		// El signo se saca con >>> y no con Math.abs: abs(Integer.MIN_VALUE) es negativo,
		// y ahi el indice revienta la lista.
		return elegibles.get((clan.hashCode() >>> 1) % elegibles.size());
	}

	/**
	 * Le da al clan un icono que no este usando ningun otro, y lo guarda.
	 *
	 * <p>Recorre lo repartido y no sortea: un sorteo repite mucho antes de agotar la lista.
	 * Si ya no queda ninguno libre —mas clanes que iconos— cae en el derivado del id, que es
	 * lo mismo que veia antes de tener uno propio.
	 *
	 * <p>Solo mira <b>este</b> archivo, que ya esta en memoria: no carga un solo clan.
	 */
	public void asignarLibre(UUID clan) {
		if (clan == null || elegibles.isEmpty() || iconos.containsKey(clan)) {
			return;
		}
		Set<Material> usados = new java.util.HashSet<>(iconos.values());
		// Arranca a buscar desde el icono que le tocaba por id y da la vuelta. Empezar
		// siempre por el principio de la lista repartiria las espadas a los primeros cinco
		// clanes del servidor y las flores a nadie.
		int desde = (clan.hashCode() >>> 1) % elegibles.size();
		for (int i = 0; i < elegibles.size(); i++) {
			Material material = elegibles.get((desde + i) % elegibles.size());
			if (!usados.contains(material)) {
				setIcono(clan, material);
				return;
			}
		}
		setIcono(clan, getIcono(clan));
	}

	/** Si ese clan eligio icono. Sirve para no mostrar "cambiar" donde no hay nada puesto. */
	public boolean tieneIcono(UUID clan) {
		return clan != null && iconos.containsKey(clan);
	}

	/** Los materiales que se pueden elegir, en el orden del config. */
	public List<Material> getElegibles() {
		return java.util.Collections.unmodifiableList(elegibles);
	}

	public boolean esElegible(Material material) {
		return material != null && elegibles.contains(material);
	}

	/** Guarda el icono. Devuelve false si ese material no esta en la lista blanca. */
	public boolean setIcono(UUID clan, Material material) {
		if (clan == null || !esElegible(material)) {
			return false;
		}
		iconos.put(clan, material);
		guardar();
		return true;
	}

	/** Lo borra cuando el clan deja de existir, para que el archivo no acumule basura. */
	public void olvidar(UUID clan) {
		if (clan != null && iconos.remove(clan) != null) {
			guardar();
		}
	}

	private void leerElegibles() {
		List<String> nombres = Main.plugin.getConfig().getStringList("iconos");
		if (nombres.isEmpty()) {
			nombres = java.util.Arrays.asList(POR_DEFECTO);
		}
		// LinkedHashSet: respeta el orden del config y descarta repetidos, que en una
		// grilla se verian como dos casilleros iguales.
		Set<Material> unicos = new LinkedHashSet<>();
		for (String nombre : nombres) {
			Material material = Material.matchMaterial(nombre.trim().toUpperCase(java.util.Locale.ROOT));
			// Un material que no existe en esta version se saltea con aviso, en vez de
			// tumbar el menu entero al abrirlo.
			if (material == null || material.isAir()) {
				Main.plugin.getLogger().warning("[clanes] icono desconocido en el config: " + nombre);
				continue;
			}
			unicos.add(material);
		}
		elegibles.addAll(unicos);
	}

	private File archivo() {
		return new File(Main.plugin.getDataFolder(), ARCHIVO);
	}

	private void cargar() {
		File origen = archivo();
		if (!origen.exists()) {
			return;
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(origen);
		for (String clave : yaml.getKeys(false)) {
			UUID clan;
			try {
				clan = UUID.fromString(clave);
			} catch (IllegalArgumentException e) {
				Main.plugin.getLogger().warning("[clanes] UUID ilegible en " + ARCHIVO + ": " + clave);
				continue;
			}
			Material material = Material.matchMaterial(yaml.getString(clave, ""));
			if (material != null && !material.isAir()) {
				iconos.put(clan, material);
			}
		}
	}

	private void guardar() {
		YamlConfiguration yaml = new YamlConfiguration();
		for (Map.Entry<UUID, Material> entrada : iconos.entrySet()) {
			yaml.set(entrada.getKey().toString(), entrada.getValue().name());
		}
		try {
			yaml.save(archivo());
		} catch (IOException e) {
			Main.plugin.getLogger().severe("No pude guardar " + ARCHIVO + ": " + e.getMessage());
		}
	}
}
