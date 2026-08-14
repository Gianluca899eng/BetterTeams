package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Quien entra a los duelos y quien no.
 *
 * <p>Dos decisiones distintas, y las toma gente distinta:
 * <ul>
 * <li><b>Cada jugador</b> elige si participa en los duelos de su clan. Es lo que vuelve
 * justo el sistema: el duelo lo pacta el duenio del clan, asi que sin esto un miembro
 * queda expuesto por una decision que no tomo.
 * <li><b>Cada clan</b> —lider o colider— elige si entra automaticamente en los duelos de
 * sus aliados. Es el mismo problema un escalon mas arriba: hoy un aliado te arrastra sin
 * preguntarte.
 * </ul>
 *
 * <p>🔑 <b>Las dos vienen en SI por defecto.</b> Un sistema de duelo donde hay que
 * anotarse no lo usa nadie; lo que hace falta es poder salirse, no tener que entrar.
 *
 * <p><b>Se guardan solo las excepciones.</b> El archivo lista a los que dijeron que NO, asi
 * que un servidor donde nadie se baja tiene un archivo vacio y el default no depende de que
 * exista una entrada por jugador.
 *
 * <p>Cambiar la preferencia <b>no afecta a un duelo ya empezado</b>: los participantes se
 * congelan al arrancar (ver {@link Duelo}). Eso es a proposito — si se pudiera salir en
 * caliente, alcanzaria con bajarse cuando te estan por matar.
 */
public class DueloPreferencias {

	private static final String ARCHIVO = "duelo-preferencias.yml";

	private final Set<UUID> jugadoresAfuera = new HashSet<>();
	private final Set<UUID> clanesQueNoAyudan = new HashSet<>();

	public DueloPreferencias() {
		cargar();
	}

	/** Si ese jugador entra a los duelos de su clan. Por defecto si. */
	public boolean participa(UUID jugador) {
		return jugador != null && !jugadoresAfuera.contains(jugador);
	}

	public void setParticipa(UUID jugador, boolean participa) {
		if (jugador == null) {
			return;
		}
		boolean cambio = participa ? jugadoresAfuera.remove(jugador) : jugadoresAfuera.add(jugador);
		if (cambio) {
			guardar();
		}
	}

	/** Si ese clan entra automaticamente en los duelos de sus aliados. Por defecto si. */
	public boolean ayudaAliados(UUID clan) {
		return clan != null && !clanesQueNoAyudan.contains(clan);
	}

	public void setAyudaAliados(UUID clan, boolean ayuda) {
		if (clan == null) {
			return;
		}
		boolean cambio = ayuda ? clanesQueNoAyudan.remove(clan) : clanesQueNoAyudan.add(clan);
		if (cambio) {
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
		leer(yaml.getStringList("jugadores-afuera"), jugadoresAfuera);
		leer(yaml.getStringList("clanes-que-no-ayudan"), clanesQueNoAyudan);
	}

	/** Un UUID ilegible se saltea en vez de tumbar la carga entera. */
	private void leer(List<String> textos, Set<UUID> destino) {
		for (String texto : textos) {
			try {
				destino.add(UUID.fromString(texto));
			} catch (IllegalArgumentException ignorado) {
				Main.plugin.getLogger().warning("[duelo] UUID ilegible en " + ARCHIVO + ": " + texto);
			}
		}
	}

	/**
	 * Escribe el archivo.
	 *
	 * <p>Se llama solo cuando algo cambio de verdad —lo comprueban los dos setters— y eso
	 * pasa cuando alguien toca el menu, no por tick.
	 */
	private void guardar() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("jugadores-afuera", aTexto(jugadoresAfuera));
		yaml.set("clanes-que-no-ayudan", aTexto(clanesQueNoAyudan));
		try {
			yaml.save(archivo());
		} catch (IOException e) {
			Main.plugin.getLogger().severe("No pude guardar " + ARCHIVO + ": " + e.getMessage());
		}
	}

	private List<String> aTexto(Set<UUID> ids) {
		List<String> lista = new ArrayList<>(ids.size());
		for (UUID id : ids) {
			lista.add(id.toString());
		}
		return lista;
	}
}
