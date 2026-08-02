package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * No deja apagar el PvP mientras tu clan esta en duelo.
 *
 * <p><b>Por que existe.</b> El duelo se hacia valer destapando el dano que otro
 * plugin cancelaba, y eso ataca el sintoma un paso tarde: el jugador escribia
 * {@code /pvp}, quedaba protegido, y el duelo se volvia decorativo. Un duelo que se
 * apaga con un comando no es un duelo.
 *
 * <p>Cortar el toggle es mas honesto que pelear el evento de dano: en vez de
 * deshacer la decision de PvPManager, se evita que el jugador entre en el estado que
 * la provoca, y el mensaje explica por que.
 *
 * <p>Se intercepta el comando y no un evento del otro plugin a proposito: asi no
 * hace falta compilar contra PvPManager, y si algun dia se cambia por otro plugin de
 * PvP esto sigue funcionando igual.
 *
 * <p>⚠️ <b>Solo cubre apagarlo, nunca prenderlo.</b> Prender el PvP siempre se puede.
 */
public class DueloToggleListener implements Listener {

	/** El comando y sus alias, del plugin.yml de PvPManager. */
	private static final Set<String> COMANDOS = new HashSet<>(Arrays.asList(
			"pvp", "togglepvp", "pvptoggle"));

	private final DueloManager manager;

	public DueloToggleListener(DueloManager manager) {
		this.manager = manager;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void alEscribirComando(PlayerCommandPreprocessEvent evento) {
		if (!manager.hayDuelos() || !manager.isPisaPvpIndividual()) {
			return;
		}

		String linea = evento.getMessage();
		int corte = linea.indexOf(' ');
		String comando = (corte == -1 ? linea : linea.substring(0, corte)).substring(1).toLowerCase(Locale.ROOT);
		// Un comando puede venir con el prefijo del plugin: /pvpmanager:pvp
		int dosPuntos = comando.indexOf(':');
		if (dosPuntos >= 0) {
			comando = comando.substring(dosPuntos + 1);
		}
		if (!COMANDOS.contains(comando)) {
			return;
		}

		// Si lo que escribio es "pvp on", que pase: prender nunca se bloquea.
		if (corte != -1) {
			String resto = linea.substring(corte + 1).trim().toLowerCase(Locale.ROOT);
			if (resto.startsWith("on") || resto.startsWith("true")) {
				return;
			}
		}

		Player jugador = evento.getPlayer();
		if (manager.getDuelo(Team.getTeam(jugador)) == null) {
			return;
		}

		evento.setCancelled(true);
		MessageManager.sendMessage(jugador, "duelo.no_podes_apagar_pvp");
	}
}
