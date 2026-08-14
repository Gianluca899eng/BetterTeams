package com.booksaw.betterTeams.luniatic.hitos;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.luniatic.Texto;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * La escalera de hitos del clan: un contador de bloques minados entre todos los miembros y
 * las cosas que se habilitan al llegar a cada escalon.
 *
 * <h2>Que problema resuelve, y por que no alcanzaba con el puntaje</h2>
 *
 * <p>El clan ya tenia una magnitud —el puntaje— pero no servia para esto: el puntaje se
 * <b>gasta</b> al subir de nivel y se <b>resetea</b> todos los meses con la purga. Colgar el
 * cofre del puntaje significaria que un clan lo pierde por comprar un nivel, o el primero de
 * cada mes. El contador de hitos es lo contrario en las dos cosas: <b>no se gasta y no se
 * resetea nunca</b>, asi que sirve de escalera permanente sin pisar al puntaje, que se queda
 * siendo la vitrina mensual.
 *
 * <h2>Donde vive el dato</h2>
 *
 * <p>En un archivo propio, {@code clan-hitos.yml}, y no en el {@code teamInfo} del clan. Es la
 * misma decision que ya se tomo con los duelos y con los iconos: meterlo ahi obliga a tocar
 * {@code StoredTeamValue} y sus tres implementaciones de storage, que son de upstream, y cada
 * merge futuro costaria mas.
 *
 * <h2>Por que el estado no se guarda</h2>
 *
 * <p>Lo unico persistido es el contador. Si un hito esta alcanzado se <b>deriva</b> comparando
 * el contador con su umbral. Guardar aparte un "desbloqueado: si" seria una segunda fuente de
 * verdad, y bastaria un guardado perdido para dejar a un clan con el cofre abierto y el
 * contador en cero. Ademas, asi bajar un umbral en el config desbloquea solo a quien ya lo
 * habia superado, sin migracion.
 */
public class HitosManager {

	private static final String ARCHIVO = "clan-hitos.yml";

	/**
	 * Lista blanca que se usa si el config no trae una.
	 *
	 * <p>Son bloques de <b>terreno y menas</b>. Quedan afuera a proposito la tierra, la arena,
	 * la madera y la grava, que se juntan por camiones sin que eso sea minar, y sobre todo el
	 * <b>adoquin</b>: es lo que tira un generador de lava y agua, o sea la fuente renovable
	 * mas facil de automatizar con un auto-clic.
	 */
	private static final String[] BLOQUES_POR_DEFECTO = {
			"STONE", "GRANITE", "DIORITE", "ANDESITE", "TUFF", "CALCITE", "DRIPSTONE_BLOCK",
			"DEEPSLATE", "NETHERRACK", "BASALT", "SMOOTH_BASALT", "BLACKSTONE", "END_STONE",
			"COAL_ORE", "DEEPSLATE_COAL_ORE", "IRON_ORE", "DEEPSLATE_IRON_ORE",
			"COPPER_ORE", "DEEPSLATE_COPPER_ORE", "GOLD_ORE", "DEEPSLATE_GOLD_ORE",
			"REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE", "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE",
			"DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE",
			"NETHER_GOLD_ORE", "NETHER_QUARTZ_ORE", "ANCIENT_DEBRIS", "AMETHYST_BLOCK"
	};

	private final boolean habilitado;
	private final Set<Material> bloques;
	private final List<Hito> hitos = new ArrayList<>();
	private final BloquesColocados colocados;
	private final int guardadoSegundos;

	/** Bloques minados por cada clan, para siempre. Es lo unico que se persiste. */
	private final Map<UUID, Long> contador = new ConcurrentHashMap<>();
	private final AtomicBoolean sucio = new AtomicBoolean(false);
	private final File archivo;

	public HitosManager(ConfigurationSection config) {
		this.archivo = new File(Main.plugin.getDataFolder(), ARCHIVO);

		if (config == null) {
			// Sin seccion en el config el sistema queda apagado, y apagado significa que el
			// cofre NO se gatea. Es la falla segura: un config incompleto no le puede sacar
			// a un clan algo que ya usaba.
			this.habilitado = false;
			this.bloques = EnumSet.noneOf(Material.class);
			this.colocados = new BloquesColocados(1000);
			this.guardadoSegundos = 120;
			return;
		}

		this.habilitado = config.getBoolean("enabled", false);
		this.bloques = leerBloques(config);
		this.guardadoSegundos = Math.max(15, config.getInt("guardado", 120));
		this.colocados = new BloquesColocados(config.getInt("memoria-colocados", 20000));
		leerHitos(config.getConfigurationSection("lista"));
		cargar();
	}

	// ------------------------------------------------------------------ config

	private Set<Material> leerBloques(ConfigurationSection config) {
		List<String> nombres = config.getStringList("bloques");
		if (nombres.isEmpty()) {
			nombres = new ArrayList<>();
			Collections.addAll(nombres, BLOQUES_POR_DEFECTO);
		}
		// EnumSet y no HashSet: la comprobacion por bloque roto es lo mas caliente de todo
		// esto, y en un EnumSet es mirar un bit por el ordinal, sin hash ni equals.
		Set<Material> set = EnumSet.noneOf(Material.class);
		for (String nombre : nombres) {
			Material material = Material.matchMaterial(nombre.trim().toUpperCase());
			if (material == null || !material.isBlock()) {
				aviso("bloque desconocido o que no es bloque en hitos.bloques: " + nombre);
				continue;
			}
			set.add(material);
		}
		return set;
	}

	private void leerHitos(ConfigurationSection lista) {
		if (lista == null) {
			return;
		}
		for (String id : lista.getKeys(false)) {
			ConfigurationSection seccion = lista.getConfigurationSection(id);
			if (seccion == null) {
				continue;
			}

			long umbral = seccion.getLong("umbral", 0);
			if (umbral <= 0) {
				aviso("el hito '" + id + "' no tiene umbral y se descarta.");
				continue;
			}

			Recompensa recompensa;
			try {
				recompensa = Recompensa.valueOf(seccion.getString("recompensa", "").trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				// Se descarta en vez de cargarlo igual: un hito que se alcanza y no da nada es
				// peor que no tenerlo, porque el clan cumple el objetivo y no recibe lo
				// prometido, y nadie se entera de que fue un error de config.
				aviso("el hito '" + id + "' tiene una recompensa desconocida ("
						+ seccion.getString("recompensa") + ") y se descarta.");
				continue;
			}

			Material icono = Material.matchMaterial(
					seccion.getString("icono", "CHEST").trim().toUpperCase());
			if (icono == null) {
				icono = Material.CHEST;
			}

			hitos.add(new Hito(
					id,
					seccion.getString("nombre", id),
					seccion.getString("descripcion", ""),
					umbral,
					icono,
					recompensa,
					Math.max(0, seccion.getInt("cantidad", 1))));
		}

		// De menor a mayor. El menu, el comando y "el proximo hito" recorren la lista en
		// orden y dan por hecho que esta ordenada; el config es un mapa y no garantiza nada.
		hitos.sort(Comparator.comparingLong(Hito::getUmbral));
	}

	private void aviso(String texto) {
		Main.plugin.getLogger().warning("[hitos] " + texto);
	}

	// ------------------------------------------------------------------ estado

	public boolean isHabilitado() {
		return habilitado && !hitos.isEmpty();
	}

	/** Los hitos, de menor a mayor umbral. Solo lectura. */
	public List<Hito> getHitos() {
		return Collections.unmodifiableList(hitos);
	}

	public Set<Material> getBloquesQueCuentan() {
		return Collections.unmodifiableSet(bloques);
	}

	public BloquesColocados getColocados() {
		return colocados;
	}

	public int getGuardadoSegundos() {
		return guardadoSegundos;
	}

	/** Si ese material suma al contador cuando se rompe. */
	public boolean cuenta(Material material) {
		return bloques.contains(material);
	}

	public long getBloquesMinados(UUID clan) {
		if (clan == null) {
			return 0;
		}
		Long valor = contador.get(clan);
		return valor == null ? 0 : valor;
	}

	/**
	 * Si el clan tiene desbloqueada esa recompensa.
	 *
	 * <p>Con el sistema apagado devuelve {@code true} para todo: apagar los hitos tiene que
	 * devolver el plugin a como estaba antes de que existieran, no dejar a todo el mundo sin
	 * cofre.
	 */
	public boolean tiene(UUID clan, Recompensa recompensa) {
		if (!isHabilitado()) {
			return true;
		}
		long minados = getBloquesMinados(clan);
		for (Hito hito : hitos) {
			if (hito.getRecompensa() == recompensa && hito.alcanzado(minados)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Cuanto suman los hitos alcanzados de una recompensa numerica.
	 *
	 * <p>Suma todos los que apliquen, asi que dos hitos de {@code WARPS} se acumulan. Con el
	 * sistema apagado devuelve 0: el bonus no existe, y el limite vuelve a ser el del nivel.
	 */
	public int bonus(UUID clan, Recompensa recompensa) {
		if (!isHabilitado() || clan == null) {
			return 0;
		}
		long minados = getBloquesMinados(clan);
		int total = 0;
		for (Hito hito : hitos) {
			if (hito.getRecompensa() == recompensa && hito.alcanzado(minados)) {
				total += hito.getCantidad();
			}
		}
		return total;
	}

	/** El primer hito que el clan todavia no alcanzo, o null si ya los tiene todos. */
	public Hito getProximo(UUID clan) {
		long minados = getBloquesMinados(clan);
		for (Hito hito : hitos) {
			if (!hito.alcanzado(minados)) {
				return hito;
			}
		}
		return null;
	}

	/** El hito que da esa recompensa, para poder nombrarlo en un mensaje. */
	public Hito hitoDe(Recompensa recompensa) {
		for (Hito hito : hitos) {
			if (hito.getRecompensa() == recompensa) {
				return hito;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ contar

	/**
	 * Suma bloques al clan y avisa si con eso cruzo algun hito.
	 *
	 * <p>El cruce se detecta comparando el total de antes con el de despues, y no consultando
	 * un "ya avisado" guardado: como el contador nunca baja, cada umbral se cruza una sola vez
	 * en la vida del clan y el aviso no se puede repetir. Eso ahorra persistir un estado mas.
	 */
	public void sumar(UUID clan, long cantidad) {
		if (clan == null || cantidad <= 0 || !isHabilitado()) {
			return;
		}

		// merge es atomico sobre el ConcurrentHashMap: dos regiones de Folia minando a la vez
		// no se pisan el contador.
		long ahora = contador.merge(clan, cantidad, Long::sum);
		sucio.set(true);

		// El "antes" se deduce del resultado y no se lee aparte. Leerlo antes del merge seria
		// una segunda operacion no atomica: dos hilos podrian leer el mismo valor previo,
		// creerse los dos que cruzaron el umbral, y anunciar el hito dos veces.
		long antes = ahora - cantidad;
		for (Hito hito : hitos) {
			if (hito.getUmbral() > antes && hito.getUmbral() <= ahora) {
				anunciar(clan, hito);
			}
		}
	}

	/**
	 * Le avisa al clan que alcanzo un hito.
	 *
	 * <p>Solo a los miembros conectados: es la unica superficie donde el aviso significa algo,
	 * y evita cargar de disco a los que no estan. Cargar el clan aca no cuesta nada porque
	 * esto pasa, como mucho, tres veces en la vida de un clan.
	 */
	private void anunciar(UUID clan, Hito hito) {
		Team equipo = Team.getTeam(clan);
		if (equipo == null) {
			return;
		}
		String cabecera = "&#7162FF» &#9235FF&lHito del clan alcanzado";
		String cuerpo = "&#7162FF» &#E4D9FF" + hito.getNombre();
		String detalle = hito.getDescripcion().isEmpty()
				? null
				: "&#7162FF» &#E4D9FF" + hito.getDescripcion();

		for (Player miembro : equipo.getOnlineMembers()) {
			miembro.sendMessage("");
			miembro.sendMessage(Texto.col(cabecera));
			miembro.sendMessage(Texto.col(cuerpo));
			if (detalle != null) {
				miembro.sendMessage(Texto.col(detalle));
			}
			miembro.sendMessage("");
		}
	}

	/**
	 * Le explica al jugador por que todavia no puede usar algo, y cuanto le falta.
	 *
	 * <p>Un "no tienes permiso" pelado seria el peor resultado posible: la funcion existe, el
	 * boton esta a la vista, y el jugador no tendria forma de saber que hay que hacer para
	 * conseguirla. Por eso el mensaje dice el objetivo y el numero que falta.
	 */
	public void avisarBloqueado(Player jugador, UUID clan, Recompensa recompensa) {
		Hito hito = hitoDe(recompensa);
		if (jugador == null || hito == null) {
			return;
		}
		long minados = getBloquesMinados(clan);
		long faltan = Math.max(0, hito.getUmbral() - minados);

		jugador.sendMessage("");
		jugador.sendMessage(Texto.col("&#7162FF» &#E4D9FFTu clan todavía no desbloqueó "
				+ "&#9235FF" + hito.getNombre() + "&#E4D9FF."));
		jugador.sendMessage(Texto.col("&#7162FF» &#E4D9FFFaltan &#9235FF"
				+ HitosCommand.numero(faltan) + "&#E4D9FF bloques minados entre todo el clan."));
		jugador.sendMessage(Texto.col("&#7162FF» &#E4D9FFMira cómo van con &#7162FF/clan hitos&#E4D9FF."));
		jugador.sendMessage("");
	}

	// ------------------------------------------------------------------ staff

	/** Fija el contador de un clan. Para el comando de staff. */
	public void setBloquesMinados(UUID clan, long valor) {
		if (clan == null) {
			return;
		}
		contador.put(clan, Math.max(0, valor));
		sucio.set(true);
	}

	/**
	 * Suelta el contador de un clan disuelto.
	 *
	 * <p>Sin esto el archivo acumula una entrada por cada clan que existio alguna vez. Es
	 * exactamente el resto que quedo en {@code chestClaims} apuntando a un clan borrado.
	 */
	public void olvidar(UUID clan) {
		if (clan != null && contador.remove(clan) != null) {
			sucio.set(true);
		}
	}

	// ------------------------------------------------------------------ disco

	private void cargar() {
		if (!archivo.exists()) {
			return;
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
		for (String clave : yaml.getKeys(false)) {
			try {
				contador.put(UUID.fromString(clave), Math.max(0, yaml.getLong(clave)));
			} catch (IllegalArgumentException e) {
				aviso("clave que no es un id de clan en " + ARCHIVO + ": " + clave);
			}
		}
	}

	/** Vuelca a disco solo si algo cambio desde el ultimo volcado. */
	public void guardarSiCambio() {
		if (sucio.compareAndSet(true, false)) {
			guardar();
		}
	}

	public void guardar() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.options().setHeader(List.of(
				"Bloques minados por cada clan, acumulados para siempre.",
				"Lo escribe el plugin: editarlo con el servidor corriendo no tiene efecto,",
				"porque el contador vive en memoria y el proximo volcado pisa el archivo.",
				"Para cambiarlo en caliente esta /clanadmin hitos <clan> set <cantidad>."));
		for (Map.Entry<UUID, Long> entrada : contador.entrySet()) {
			yaml.set(entrada.getKey().toString(), entrada.getValue());
		}
		// Temporal y despues mover: yaml.save() trunca antes de llenar, asi que un corte en el
		// medio dejaba el archivo vacio. Aca eso no pierde el ultimo volcado, pierde el
		// contador acumulado de todos los clanes desde que arranco el servidor.
		File temporal = new File(archivo.getParentFile(), ARCHIVO + ".tmp");
		try {
			yaml.save(temporal);
			try {
				Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException noAtomico) {
				Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			Main.plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + ARCHIVO, e);
			// Se vuelve a marcar sucio para que el proximo volcado lo reintente en vez de
			// dar por guardado algo que no llego al disco.
			sucio.set(true);
			try {
				Files.deleteIfExists(temporal.toPath());
			} catch (IOException ignorado) {
				// el temporal huerfano lo pisa el proximo volcado
			}
		}
	}
}
