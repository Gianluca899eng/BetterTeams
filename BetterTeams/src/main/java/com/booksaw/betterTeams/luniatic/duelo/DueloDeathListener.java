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

		// Los dos tienen que estar adentro del duelo: una caida donde alguno se bajo no
		// es una baja del duelo, asi que no mueve el marcador ni corta el regreso. Se
		// resuelve sobre el duelo del caido para no volver a recorrer los clanes.
		Duelo duelo = manager.getDuelo(clanCaido);
		boolean porRival = asesino != null && !asesino.equals(caido)
				&& clanCaido != null && clanAsesino != null
				&& manager.sonRivales(clanCaido, clanAsesino)
				&& duelo != null
				&& duelo.esParticipante(caido.getUniqueId())
				&& duelo.esParticipante(asesino.getUniqueId());

		// El regreso se bloquea por dos motivos, no uno: que te haya matado el rival, o
		// que hayas caido pegado a su base. Sin lo segundo el atajo era obvio: tirarse a
		// la lava adentro de la base enemiga para conservar el /dback.
		boolean bloquearRegreso = porRival
				|| (duelo != null && duelo.esParticipante(caido.getUniqueId())
						&& manager.cercaDeBaseRival(caido.getLocation(), clanCaido));

		// Se anota SIEMPRE, tambien cuando no aplica: la marca describe la ultima caida,
		// asi que morir despues lejos y de otra forma la levanta. Y se anota antes del
		// filtro de registrarBaja, que solo cuenta a los clanes principales: un aliado
		// arrastrado tambien esta peleando y no tiene que poder volver.
		manager.marcarCaida(caido.getUniqueId(), bloquearRegreso);

		if (!porRival) {
			return;
		}

		manager.registrarBaja(clanCaido, clanAsesino);
	}
}
