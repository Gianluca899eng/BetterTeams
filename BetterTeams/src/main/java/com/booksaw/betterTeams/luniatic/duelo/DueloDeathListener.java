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
		// Primero lo barato: sin duelos en curso no hay nada que contar, y resolver
		// el clan de un jugador recorre todos los clanes.
		if (!manager.hayDuelos()) {
			return;
		}

		Player caido = evento.getEntity();
		Player asesino = caido.getKiller();
		Team clanCaido = Team.getTeam(caido);
		Team clanAsesino = asesino == null ? null : Team.getTeam(asesino);

		boolean porRival = asesino != null && !asesino.equals(caido)
				&& clanCaido != null && clanAsesino != null
				&& manager.sonRivales(clanCaido, clanAsesino);

		// Se anota SIEMPRE, tambien cuando no fue el rival: la marca describe la ultima
		// caida, asi que ahogarse o caer a la lava despues la levanta. Y se anota antes
		// del filtro de registrarBaja, que solo cuenta a los clanes principales: un
		// aliado arrastrado tambien esta peleando y no tiene que poder volver.
		manager.marcarCaida(caido.getUniqueId(), porRival);

		if (!porRival) {
			return;
		}

		manager.registrarBaja(clanCaido, clanAsesino);
	}
}
