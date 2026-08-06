package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Lee si un jugador tiene el PvP prendido en PvPManager.
 *
 * <p><b>Solo lee.</b> El duelo ya no le toca el toggle a nadie: destapar el dano entre
 * rivales alcanza, y el {@code /pvp} del jugador tiene que seguir diciendo lo que el
 * eligio. Esto existe unicamente para que el scoreboard sepa como pintar el estado —un
 * {@code ᴏꜰꜰ} amarillo cuando estas en guerra significa "lo tenes apagado, pero tu rival
 * igual te puede pegar".
 *
 * <p><b>Por reflexion y no por dependencia.</b> Compilar contra PvPManager lo ataria al
 * build; asi, si no esta instalado o cambia de version, el puente se apaga solo y el
 * cartel cae al estado normal. Se resuelve una vez, no en cada consulta.
 */
public final class EstadoPvp {

	private static Object gestorJugadores;
	private static Method obtenerJugador;
	private static Method tienePvpPrendido;
	private static boolean resuelto;

	private EstadoPvp() {
	}

	private static void resolver() {
		if (resuelto) {
			return;
		}
		resuelto = true;
		try {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("PvPManager");
			if (plugin == null) {
				return;
			}
			gestorJugadores = plugin.getClass().getMethod("getPlayerManager").invoke(plugin);
			obtenerJugador = gestorJugadores.getClass().getMethod("get", Player.class);
			tienePvpPrendido = obtenerJugador.getReturnType().getMethod("hasPvPEnabled");
		} catch (Throwable t) {
			gestorJugadores = null;
			Main.plugin.getLogger().info("PvPManager sin puente de lectura: el cartel de PvP del "
					+ "scoreboard no va a distinguir on de off (" + t.getClass().getSimpleName() + ")");
		}
	}

	/**
	 * True si ese jugador tiene el PvP prendido. Ante la duda, true.
	 *
	 * <p>Devolver true es lo conservador: el cartel muestra el estado normal en vez de
	 * inventar un "apagado" que podria no ser cierto.
	 */
	public static boolean tienePvp(Player jugador) {
		resolver();
		if (gestorJugadores == null || jugador == null) {
			return true;
		}
		try {
			Object combatiente = obtenerJugador.invoke(gestorJugadores, jugador);
			return combatiente == null || (Boolean) tienePvpPrendido.invoke(combatiente);
		} catch (Throwable t) {
			return true;
		}
	}
}
