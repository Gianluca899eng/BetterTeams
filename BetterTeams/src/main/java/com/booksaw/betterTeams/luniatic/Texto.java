package com.booksaw.betterTeams.luniatic;

import com.booksaw.betterTeams.text.Formatter;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Color para lo que no es chat: items de menu, titulos de inventario, barras.
 *
 * <p>El chat traduce el color solo; estas superficies no. Y no alcanza con una
 * conversion: son tres pasos y hacen falta los tres.
 */
public final class Texto {

	/**
	 * Con {@code hexColors()}, o la paleta se aplasta a los 16 colores viejos. Y con
	 * el formato "x repetida" ({@code §x§9§2§3§5§F§F}), que es el unico que entiende
	 * el cliente: el {@code §#9235ff} que sale por defecto se muestra como texto.
	 */
	private static final LegacyComponentSerializer LEGACY_HEX = LegacyComponentSerializer.builder()
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	private Texto() {
	}

	/**
	 * Convierte a color de verdad.
	 *
	 * <p>1) {@code toAdventure} pasa los codigos {@code &#RRGGBB} y {@code &c} a
	 * etiquetas MiniMessage. 2) el formateador las convierte en componente, que es
	 * lo que tambien resuelve el MiniMessage que ya traen los nombres de clan.
	 * 3) se serializa a legacy con hex.
	 *
	 * <p>🔑 <b>El tercer paso no puede usar {@code LegacyTextUtils.parseAllAdventure}</b>:
	 * ese serializa sin hex, asi que aplasta cada color al legacy mas parecido y toda
	 * la paleta termina saliendo del mismo azul. Comprobado.
	 */
	public static String col(String texto) {
		return LEGACY_HEX.serializeOr(Formatter.absolute().process(LegacyTextUtils.toAdventure(texto)), "");
	}

	/** Saca el color, para meter un texto adentro de otro que ya lo tiene. */
	public static String limpiar(String texto) {
		return org.bukkit.ChatColor.stripColor(col(texto));
	}
}
