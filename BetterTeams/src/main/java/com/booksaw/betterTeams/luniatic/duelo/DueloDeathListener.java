package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Lleva el marcador del duelo.
 *
 * <p>Solo cuenta las caidas <b>a manos del clan rival</b>. Ahogarse, caerse de un
 * risco o morir en lava no suma: si contara cualquier muerte, la forma de ganar
 * seria mandar a un companero a saltar al vacio del lado del rival.
 *
 * <p>El contador no se guarda en ningun lado y no toca el puntaje del clan. Se
 * borra con el duelo.
 */
public class DueloDeathListener implements Listener {

	private final DueloManager manager;

	public DueloDeathListener(DueloManager manager) {
		this.manager = manager;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void alMorir(PlayerDeathEvent evento) {
		Player caido = evento.getEntity();
		Player asesino = caido.getKiller();
		if (asesino == null || asesino.equals(caido)) {
			return;
		}

		Team clanCaido = Team.getTeam(caido);
		Team clanAsesino = Team.getTeam(asesino);
		if (clanCaido == null || clanAsesino == null) {
			return;
		}

		manager.registrarBaja(clanCaido, clanAsesino);
	}
}
