package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Hace valer el duelo por encima del toggle individual de PvP.
 *
 * <p>Solo corre si <code>duelo.pisa-pvp-individual</code> esta encendido, y viene
 * apagado de fabrica. Lo que hace es destapar un dano que otro plugin cancelo,
 * y eso tiene una consecuencia que hay que entender antes de encenderlo: el
 * consentimiento lo da el duenio del clan, asi que un miembro con el PvP apagado
 * queda alcanzado por una decision que no tomo el.
 *
 * <p>Nunca destapa dano dentro de una region donde WorldGuard prohibe PvP: sin esa
 * guarda, esto tambien romperia la zona segura del spawn.
 */
public class DueloDamageListener implements Listener {

	private final DueloManager manager;
	private final GuardiaRegion guardia;

	public DueloDamageListener(DueloManager manager) {
		this.manager = manager;
		this.guardia = Bukkit.getPluginManager().getPlugin("WorldGuard") != null ? new GuardiaRegion() : null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void alDaniar(EntityDamageByEntityEvent evento) {
		// El orden importa: este listener corre en CADA evento de dano del servidor,
		// mobs y granjas incluidos. Primero los chequeos que son una comparacion, y
		// recien al final resolver el clan, que recorre todos los clanes.
		if (!evento.isCancelled() || !manager.hayDuelos() || !manager.isPisaPvpIndividual()) {
			return;
		}
		if (!(evento.getEntity() instanceof Player)) {
			return;
		}
		Player victima = (Player) evento.getEntity();
		Player atacante = resolverAtacante(evento);
		if (atacante == null || atacante.equals(victima)) {
			return;
		}

		Team clanVictima = Team.getTeam(victima);
		Team clanAtacante = Team.getTeam(atacante);
		if (!manager.sonRivales(clanVictima, clanAtacante)) {
			return;
		}
		if (guardia != null && !guardia.permitePvp(victima.getLocation())) {
			return;
		}

		evento.setCancelled(false);
	}

	private Player resolverAtacante(EntityDamageByEntityEvent evento) {
		if (evento.getDamager() instanceof Player) {
			return (Player) evento.getDamager();
		}
		if (evento.getDamager() instanceof Projectile) {
			ProjectileSource fuente = ((Projectile) evento.getDamager()).getShooter();
			if (fuente instanceof Player) {
				return (Player) fuente;
			}
		}
		return null;
	}
}
