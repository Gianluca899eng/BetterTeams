package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que hay que entregarle a alguien la proxima vez que entre.
 *
 * <p><b>Existe porque un duelo lo pactan dos lideres, no el clan entero conectado.</b> El
 * que estaba desconectado se enteraria al volver, sin aviso y sin el libro — y es justo el
 * que menos sabe en que lo metieron.
 *
 * <p>Guarda <b>texto</b>, no items serializados. Un {@link ItemStack} en YAML ata el archivo
 * a la version del servidor y se rompe en la proxima actualizacion de formato; las paginas
 * son cadenas y el libro se arma en el momento de entregarlo.
 *
 * <p>🔑 <b>Los libros se guardan como una LISTA, no como uno solo.</b> La primera version
 * acumulaba paginas y titulo sueltos: dos libros pendientes —el desafio y el de arranque—
 * se fusionaban en un unico libro con las paginas de los dos y el titulo del ultimo.
 *
 * <p>Se escribe al encolar y al entregar. Nada por tick.
 */
public class DueloBuzon {

	private static final String ARCHIVO = "duelo-buzon.yml";

	/**
	 * Cuanto se guarda algo sin entregar.
	 *
	 * <p>Sin vencimiento esto crece para siempre: cada duelo deja una entrada por cada
	 * participante desconectado, y la del que no vuelve nunca no la borra nadie. Un mes
	 * despues el aviso ya no informa de nada — el duelo termino hace semanas.
	 */
	private static final long VENCE_MILLIS = 30L * 24 * 60 * 60 * 1000;

	/** Lo pendiente de cada jugador. Se vacia al entregarlo. */
	private final Map<UUID, Pendiente> pendientes = new HashMap<>();

	public DueloBuzon() {
		cargar();
	}

	/** Un libro pendiente, con su propio vencimiento. */
	private static final class Libro {
		private String titulo = "";
		private final List<String> paginas = new ArrayList<>();
		/** Fecha propia; 0 usa la general. La necesita el desafio, que dura un dia. */
		private long venceMillis;
	}

	/** Avisos de chat y libros que le esperan a un jugador. */
	private static final class Pendiente {
		private final List<String> mensajes = new ArrayList<>();
		private final List<Libro> libros = new ArrayList<>();
		private long guardadoMillis;

		private boolean vacio() {
			return mensajes.isEmpty() && libros.isEmpty();
		}
	}

	/** Deja pendiente un aviso y un libro para cuando el jugador entre. */
	public void encolar(UUID jugador, List<String> mensajes, String tituloLibro, List<String> paginas) {
		encolar(jugador, mensajes, tituloLibro, paginas, 0L);
	}

	/**
	 * Igual, pero con fecha de vencimiento propia para el libro.
	 *
	 * <p>La necesita el libro de desafio: un desafio dura un dia, asi que entregarlo tres
	 * dias despues seria darle a alguien un libro que invita a algo que ya no existe.
	 */
	public void encolar(UUID jugador, List<String> mensajes, String tituloLibro, List<String> paginas,
			long venceMillis) {
		if (jugador == null) {
			return;
		}
		Pendiente pendiente = pendientes.computeIfAbsent(jugador, id -> new Pendiente());
		pendiente.guardadoMillis = System.currentTimeMillis();
		if (mensajes != null) {
			pendiente.mensajes.addAll(mensajes);
		}
		if (paginas != null && !paginas.isEmpty()) {
			Libro libro = new Libro();
			libro.titulo = tituloLibro == null ? "" : tituloLibro;
			libro.paginas.addAll(paginas);
			libro.venceMillis = venceMillis;
			pendiente.libros.add(libro);
		}
		if (pendiente.vacio()) {
			pendientes.remove(jugador);
			return;
		}
		guardar();
	}

	/** Si hay algo para ese jugador. Es lo primero que mira el listener de entrada. */
	public boolean tienePendiente(UUID jugador) {
		return jugador != null && pendientes.containsKey(jugador);
	}

	/**
	 * Entrega lo pendiente y lo borra.
	 *
	 * <p>Los libros van al inventario, y <b>lo que no entra cae al piso</b>: perderlos por
	 * tener el inventario lleno seria perder justo lo que explica en que te metieron.
	 *
	 * <p>🔑 Se entrega PRIMERO y se borra despues. Al reves, si el jugador se va en ese tick
	 * lo pendiente ya estaba tachado y se pierde para siempre. Entregar dos veces es un libro
	 * repetido; no entregar es no enterarse.
	 */
	public void entregar(Player jugador) {
		if (jugador == null) {
			return;
		}
		Pendiente pendiente = pendientes.get(jugador.getUniqueId());
		if (pendiente == null) {
			return;
		}

		for (String mensaje : pendiente.mensajes) {
			jugador.sendMessage(mensaje);
		}
		long ahora = System.currentTimeMillis();
		for (Libro libro : pendiente.libros) {
			// Un libro con fecha propia vencida no se entrega: el desafio que anunciaba ya
			// no existe.
			if (libro.venceMillis > 0 && ahora > libro.venceMillis) {
				continue;
			}
			ItemStack item = LibroDelDuelo.armar(libro.titulo, libro.paginas);
			for (ItemStack sobrante : jugador.getInventory().addItem(item).values()) {
				jugador.getWorld().dropItem(jugador.getLocation(), sobrante);
			}
		}

		pendientes.remove(jugador.getUniqueId());
		guardar();
	}

	private boolean vencio(Pendiente pendiente) {
		return System.currentTimeMillis() - pendiente.guardadoMillis > VENCE_MILLIS;
	}

	/**
	 * Saca lo vencido sin esperar al proximo arranque.
	 *
	 * <p>El vencimiento solo se aplicaba al cargar, asi que con el servidor prendido semanas
	 * lo de quien no vuelve se quedaba en memoria y en el archivo hasta reiniciar.
	 */
	public void purgarVencidos() {
		if (pendientes.isEmpty()) {
			return;
		}
		if (pendientes.values().removeIf(this::vencio)) {
			guardar();
		}
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
		ConfigurationSection raiz = yaml.getConfigurationSection("pendientes");
		if (raiz == null) {
			return;
		}
		for (String clave : raiz.getKeys(false)) {
			UUID id;
			try {
				id = UUID.fromString(clave);
			} catch (IllegalArgumentException e) {
				Main.plugin.getLogger().warning("[duelo] UUID ilegible en " + ARCHIVO + ": " + clave);
				continue;
			}
			ConfigurationSection s = raiz.getConfigurationSection(clave);
			if (s == null) {
				continue;
			}
			Pendiente pendiente = new Pendiente();
			pendiente.mensajes.addAll(s.getStringList("mensajes"));
			// Sin fecha se toma por recien guardado, para no tirar lo que dejo una version
			// anterior al vencimiento.
			pendiente.guardadoMillis = s.getLong("guardado-millis", System.currentTimeMillis());

			ConfigurationSection libros = s.getConfigurationSection("libros");
			if (libros != null) {
				for (String indice : libros.getKeys(false)) {
					ConfigurationSection l = libros.getConfigurationSection(indice);
					if (l == null) {
						continue;
					}
					Libro libro = new Libro();
					libro.titulo = l.getString("titulo", "");
					libro.paginas.addAll(l.getStringList("paginas"));
					libro.venceMillis = l.getLong("vence-millis", 0L);
					if (!libro.paginas.isEmpty()) {
						pendiente.libros.add(libro);
					}
				}
			}

			if (pendiente.vacio() || vencio(pendiente)) {
				continue;
			}
			pendientes.put(id, pendiente);
		}
	}

	private void guardar() {
		YamlConfiguration yaml = new YamlConfiguration();
		for (Map.Entry<UUID, Pendiente> entrada : pendientes.entrySet()) {
			String base = "pendientes." + entrada.getKey();
			yaml.set(base + ".mensajes", entrada.getValue().mensajes);
			yaml.set(base + ".guardado-millis", entrada.getValue().guardadoMillis);
			int i = 0;
			for (Libro libro : entrada.getValue().libros) {
				String ruta = base + ".libros." + i++;
				yaml.set(ruta + ".titulo", libro.titulo);
				yaml.set(ruta + ".paginas", libro.paginas);
				yaml.set(ruta + ".vence-millis", libro.venceMillis);
			}
		}
		try {
			File destino = archivo();
			if (pendientes.isEmpty()) {
				// Sin nada pendiente se borra: un archivo con una lista vacia hace dudar
				// de si el sistema guardo o no.
				if (destino.exists() && !destino.delete()) {
					Main.plugin.getLogger().warning("No pude borrar " + ARCHIVO);
				}
				return;
			}
			yaml.save(destino);
		} catch (IOException e) {
			Main.plugin.getLogger().severe("No pude guardar " + ARCHIVO + ": " + e.getMessage());
		}
	}
}
