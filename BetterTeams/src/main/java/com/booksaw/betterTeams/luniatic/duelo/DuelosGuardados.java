package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda y levanta los duelos en curso.
 *
 * <p><b>Existe porque los duelos pasaron a durar dias.</b> Con 30 minutos, que vivieran
 * solo en memoria casi no se notaba: el duelo empezaba y terminaba dentro de la misma
 * sesion del servidor. Con un preset de 1, 3 o 7 dias eso deja de ser cierto —cualquier
 * reinicio cae en el medio— y sin esto el duelo se cancelaria solo, devolviendo el pozo
 * y borrando el marcador.
 *
 * <p>Va a un archivo propio y no al storage de los clanes a proposito: el estado de un
 * duelo no le pertenece a ninguno de los dos clanes, y meterlo ahi obligaria a tocar
 * {@code StoredTeamValue} y sus tres implementaciones de storage, que son de upstream.
 *
 * <p>Se escribe poco: al empezar un duelo, al sumar una baja, al cerrarlo y al apagar.
 * No hay ninguna ruta que lo escriba por tick.
 */
public final class DuelosGuardados {

	private static final String ARCHIVO = "duelos.yml";

	private DuelosGuardados() {
	}

	private static File archivo() {
		return new File(Main.plugin.getDataFolder(), ARCHIVO);
	}

	public static void guardar(Collection<Duelo> duelos) {
		YamlConfiguration yaml = new YamlConfiguration();

		int i = 0;
		for (Duelo duelo : duelos) {
			String base = "duelos." + i++;
			yaml.set(base + ".clan-a", duelo.getClanA().toString());
			yaml.set(base + ".clan-b", duelo.getClanB().toString());
			yaml.set(base + ".bando-a", aTexto(duelo.getBando(duelo.getClanA())));
			yaml.set(base + ".bando-b", aTexto(duelo.getBando(duelo.getClanB())));
			yaml.set(base + ".apuesta", duelo.getApuesta());
			yaml.set(base + ".fin-millis", duelo.getFinMillis());
			yaml.set(base + ".duracion-millis", duelo.getDuracionMillis());
			yaml.set(base + ".objetivo-bajas", duelo.getObjetivoBajas());
			yaml.set(base + ".bajas-a", duelo.getBajasA());
			yaml.set(base + ".bajas-b", duelo.getBajasB());
		}

		try {
			File destino = archivo();
			if (duelos.isEmpty()) {
				// Sin nada que guardar, se borra: un archivo con una lista vacia hace
				// dudar de si el sistema guardo o no.
				if (destino.exists() && !destino.delete()) {
					Main.plugin.getLogger().warning("No pude borrar " + ARCHIVO);
				}
				return;
			}
			yaml.save(destino);
		} catch (IOException e) {
			// Que no se pueda guardar no puede tumbar el servidor, pero tiene que
			// gritar: el pozo de los duelos abiertos depende de esto.
			Main.plugin.getLogger().severe("No pude guardar los duelos en curso: " + e.getMessage());
		}
	}

	public static List<Duelo> cargar() {
		List<Duelo> duelos = new ArrayList<>();

		File origen = archivo();
		if (!origen.exists()) {
			return duelos;
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(origen);

		ConfigurationSection seccion = yaml.getConfigurationSection("duelos");
		if (seccion != null) {
			for (String clave : seccion.getKeys(false)) {
				Duelo duelo = leerUno(seccion.getConfigurationSection(clave));
				if (duelo != null) {
					duelos.add(duelo);
				}
			}
		}
		return duelos;
	}

	private static Duelo leerUno(ConfigurationSection s) {
		if (s == null) {
			return null;
		}
		UUID clanA = aUuid(s.getString("clan-a"));
		UUID clanB = aUuid(s.getString("clan-b"));
		if (clanA == null || clanB == null) {
			return null;
		}
		Duelo duelo = new Duelo(clanA, aUuids(s.getStringList("bando-a")),
				clanB, aUuids(s.getStringList("bando-b")),
				s.getDouble("apuesta"), s.getLong("fin-millis"),
				s.getLong("duracion-millis"), s.getInt("objetivo-bajas"));
		duelo.reponerBajas(s.getInt("bajas-a"), s.getInt("bajas-b"));
		return duelo;
	}

	private static List<String> aTexto(Set<UUID> ids) {
		List<String> lista = new ArrayList<>();
		for (UUID id : ids) {
			lista.add(id.toString());
		}
		return lista;
	}

	private static Set<UUID> aUuids(List<String> textos) {
		Set<UUID> ids = new LinkedHashSet<>();
		for (String texto : textos) {
			UUID id = aUuid(texto);
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	/** Un UUID ilegible se saltea en vez de tumbar la carga entera. */
	private static UUID aUuid(String texto) {
		if (texto == null || texto.isEmpty()) {
			return null;
		}
		try {
			return UUID.fromString(texto);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
