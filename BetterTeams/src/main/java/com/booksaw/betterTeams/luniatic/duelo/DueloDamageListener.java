package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
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
 * <p>Solo corre si <code>duelo.pisa-pvp-individual</code> esta encendido. Lo que
 * hace es destapar un dano que otro plugin cancelo, y eso tiene una consecuencia
 * que hay que entender antes de encenderlo: el consentimiento lo da el duenio del
 * clan, asi que un miembro con el PvP apagado queda alcanzado por una decision que
 * no tomo el.
 *
 * <p>Nunca destapa dano dentro de una region donde WorldGuard prohibe PvP: sin esa
 * guarda, esto tambien romperia la zona segura del spawn.
 *
 * <p>🔑 <b>Por que hay un segundo handler en MONITOR.</b> Destapar el dano no
 * alcanza si otro plugin lo vuelve a cancelar despues: el evento pasa por varias
 * prioridades y la ultima gana. Sin ese chequeo, "no puedo pegar" no se distingue
 * de "el listener no corrio", y las dos causas se arreglan distinto. Con
 * <code>duelo.debug</code> encendido, el log dice cual de las dos es.
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
			depurar("no son rivales de duelo: " + atacante.getName() + " -> " + victima.getName());
			return;
		}
		if (guardia != null && !guardia.permitePvp(victima.getLocation())) {
			depurar("WorldGuard prohibe PvP en " + resumen(victima) + ", no se destapa");
			return;
		}

		evento.setCancelled(false);
		ultimoDestapado = evento;
		depurar("destapado: " + atacante.getName() + " -> " + victima.getName());
	}

	/**
	 * Ultimo en la fila: si acá el evento volvio a estar cancelado, otro plugin lo
	 * re-cancelo despues nuestro y el problema es de orden de prioridades, no de
	 * este listener.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void alFinal(EntityDamageByEntityEvent evento) {
		if (evento != ultimoDestapado) {
			return;
		}
		ultimoDestapado = null;
		if (evento.isCancelled() && manager.isDebug()) {
			Main.plugin.getLogger().warning("[duelo] el dano se destapo y OTRO PLUGIN lo volvio a cancelar; "
					+ "el override no puede ganar en esta cadena de prioridades");
		}
	}

	/**
	 * El ultimo evento que destapamos, para reconocerlo en MONITOR. Alcanza con un
	 * campo porque los eventos se procesan de a uno en el hilo del servidor.
	 */
	private EntityDamageByEntityEvent ultimoDestapado;

	private void depurar(String texto) {
		if (manager.isDebug()) {
			Main.plugin.getLogger().info("[duelo] " + texto);
		}
	}

	private String resumen(Player jugador) {
		return jugador.getWorld().getName() + " "
				+ jugador.getLocation().getBlockX() + "," + jugador.getLocation().getBlockZ();
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
