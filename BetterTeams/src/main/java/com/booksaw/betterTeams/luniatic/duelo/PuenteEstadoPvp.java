package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Lee y escribe el estado de PvP de un jugador en PvPManager.
 *
 * <p>Hace falta para poder <b>devolverle el PvP como lo tenia</b> cuando el duelo
 * termina: el duelo lo prende a la fuerza, y si al final quedara prendido le
 * cambiariamos una preferencia que el no toco.
 *
 * <p><b>Por reflexion y no por dependencia.</b> Compilar contra PvPManager obligaria
 * a sumarlo al build y ataria el fork a ese plugin; asi, si no esta instalado o
 * cambia de version, el puente se apaga solo y el duelo sigue funcionando sin
 * restaurar. Se resuelve una vez al arrancar, no en cada uso.
 *
 * <p>Escribir se hace igual por comando de consola, que es estable y no depende de
 * la firma de ningun metodo interno.
 */
public class PuenteEstadoPvp {

	private Object gestorJugadores;
	private Method obtenerJugador;
	private Method tienePvpPrendido;

	public PuenteEstadoPvp() {
		try {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("PvPManager");
			if (plugin == null) {
				return;
			}
			gestorJugadores = plugin.getClass().getMethod("getPlayerManager").invoke(plugin);
			obtenerJugador = gestorJugadores.getClass().getMethod("get", Player.class);
			Class<?> claseJugador = obtenerJugador.getReturnType();
			tienePvpPrendido = claseJugador.getMethod("hasPvPEnabled");
		} catch (Throwable t) {
			// Sin puente el duelo funciona igual: no restaura, nada mas.
			gestorJugadores = null;
			Main.plugin.getLogger().info("PvPManager sin puente de estado: el duelo no va a "
					+ "devolver el PvP como estaba (" + t.getClass().getSimpleName() + ")");
		}
	}

	public boolean disponible() {
		return gestorJugadores != null && obtenerJugador != null && tienePvpPrendido != null;
	}

	/** True si ese jugador tiene el PvP prendido. Ante la duda, true. */
	public boolean tienePvp(Player jugador) {
		if (!disponible()) {
			return true;
		}
		try {
			Object combatiente = obtenerJugador.invoke(gestorJugadores, jugador);
			return combatiente != null && (Boolean) tienePvpPrendido.invoke(combatiente);
		} catch (Throwable t) {
			// Devolver true es lo conservador: no le apagamos el PvP a nadie por un
			// error de lectura.
			return true;
		}
	}

	public void prender(Player jugador) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pvp " + jugador.getName() + " on");
	}

	public void apagar(Player jugador) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pvp " + jugador.getName() + " off");
	}
}
