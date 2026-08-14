package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

/**
 * Cabezas cuya textura es el numero del puesto, para el ranking de clanes.
 *
 * <p>La textura la sirve Mojang desde {@code textures.minecraft.net}: aca solo va el hash.
 * Son de la tipografia "Purple" de minecraft-heads, que ya viene en los dos colores de la
 * paleta (violeta de fondo, lavanda el digito), asi que no hay nada que retocar.
 *
 * <p>El juego de digitos llega hasta el 25. Del 26 en adelante se usa el simbolo de numero
 * de la misma tipografia: el puesto exacto igual va escrito en el nombre del boton.
 *
 * <p>El numero dibujado no se ve desde Bedrock: Geyser muestra las cabezas con textura
 * propia como una cabeza generica. Por eso el puesto va tambien en el nombre del boton, que
 * es lo unico que se lee en las dos plataformas.
 */
public final class CabezasNumero {

	private static final String HOST = "https://textures.minecraft.net/texture/";

	/** Hash de la textura de cada puesto. El indice 0 es el puesto 1. */
	private static final String[] DIGITOS = {
			"78a42df06fc916de110f61bd76eddbf58ed4249fce5ee51c219ec75a37b414",
			"1ef134f0efa88351b837f7c087afe1b3fb36435ab7d746fa37c0ef155e4f29",
			"1965e9c57c14c95c84e622e5306e1cf23bc5f1e47ac791f3d357b5ae8cded24",
			"6e31837d89c264c3b54f8214ae24f89a368a92bc46df9225333ad7cd449f856",
			"49357cb4664426a9ae0f9c7725ea4351dc69f15a8062c03591e26ac11bbc5a",
			"b8d550717d26ae8dda55e9a0a9edc92f04a774b501a36e24c8c4e682e32c59",
			"89141952aabca4987bddf208c5e15ca6a5bfce239af39584aeb5ee876d87",
			"a112a392ef19ee393a14505bbdad9c1e9293a04ec3ab337e374880222e708244",
			"b7277b841ef2f06e6b7631d9cfac275498f889fad94687668b251fc339a51f0",
			"61cae1ab0fce04ebf5391c65f65347497cc92be73a5b8f93a16373ae438ccf5",
			"6acdf8938c1b25ca1638b1896ba9dbc2ede7976c12258dd7d1267418fceeb36e",
			"ad13993a1fe1f984a78bc33163198bcfb6536e74d65e42653878daa6d768184",
			"395c0f482e4f5db7249e32537b57a927abef191f376fbf779502ebbbd2ac1",
			"50814a67aad55b705028fd27a716ea17da23f46d329d176e36c26c8f98",
			"41c5e7fc387913fc0d73dcf5a34777ea31033955d2d79623dfdcec2320f8",
			"af50871d5aad448f3b24966c0f7e97a8336dfd743ad13d1e51246fbf57355",
			"16c634d51c84a72ec3764c46e66022e01af5583eab58e562c514d7fcac56",
			"896d1b8db9f24efe9aa5ca9ba7caba3b66edaaff738dd210e2ad6e1aa2c3f57",
			"acecc87f4ac813664bf6b182d069834b6c542175ad2493367a4a3cc37d39f",
			"6ff188454f13ae3a5e21e75b102231161266a86315ef8fae27e9655b3559",
			"4a6e9cbfd9434eed1e97f61497b0ce9c953f87a535d11b7d2a1a021552df75",
			"d0dbdcc4939fa25aa3efca6bbdbf3cc42aef4b7a5a3888ba1c8f425aa11fd",
			"6718c0a054f9511c5de7d128205254cfe51e9749cdb1704870238798b599e8f9",
			"ebd82b3a6217e364dc51253f3aa3cd85e2a1a396fc6c771c73f0daaf2d358a2",
			"69a43ded7747b64a24b335fe6eafffa2997c92faaf5033117c15fa90c98e56"
	};

	/** Simbolo de numero, para los puestos que se pasan de la tabla. */
	private static final String OCTOTHORPE = "8c9227b36ab3403e85324a57e8d9b0e8c7ba342f5bda4fcef831d4da8d3ebdce";

	/** Los perfiles se arman una sola vez: no consultan a Mojang, pero tampoco son gratis. */
	private static final PlayerProfile[] CACHE = new PlayerProfile[DIGITOS.length + 1];

	private CabezasNumero() {
	}

	/**
	 * La cabeza de ese puesto, ya con la textura puesta.
	 *
	 * <p>Si la textura no se puede aplicar devuelve una cabeza sin skin, nunca null: el menu
	 * tiene que abrirse igual aunque falle esto.
	 *
	 * @param puesto posicion en el ranking, empezando en 1
	 */
	public static ItemStack cabeza(int puesto) {
		ItemStack pila = new ItemStack(Material.PLAYER_HEAD);
		if (puesto < 1) {
			return pila;
		}

		int indice = Math.min(puesto, DIGITOS.length + 1) - 1;
		PlayerProfile perfil = CACHE[indice];
		if (perfil == null) {
			perfil = crearPerfil(indice);
			if (perfil == null) {
				return pila;
			}
			CACHE[indice] = perfil;
		}

		if (pila.getItemMeta() instanceof SkullMeta meta) {
			meta.setOwnerProfile(perfil);
			pila.setItemMeta(meta);
		}
		return pila;
	}

	private static PlayerProfile crearPerfil(int indice) {
		String hash = indice < DIGITOS.length ? DIGITOS[indice] : OCTOTHORPE;
		// El id se deriva del indice para que sea siempre el mismo: el cliente cachea la
		// skin por perfil, y un id nuevo en cada arranque la haria descargar de nuevo.
		UUID id = UUID.nameUUIDFromBytes(("luniatic-puesto-" + indice).getBytes());
		try {
			PlayerProfile perfil = Bukkit.createPlayerProfile(id, null);
			URL url = URI.create(HOST + hash).toURL();
			// getTextures devuelve las del perfil, no una copia: alcanza con tocarlas.
			perfil.getTextures().setSkin(url);
			return perfil;
		} catch (Exception e) {
			Main.plugin.getLogger().warning("[clanes] no se pudo armar la cabeza del puesto "
					+ (indice + 1) + ": " + e.getMessage());
			return null;
		}
	}
}
