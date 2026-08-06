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
 * Hace que el duelo alcance a los rivales y a nadie mas.
 *
 * <p><b>Hace una sola cosa: deja pasar un dano que otro plugin cancelo, y solo entre
 * los dos bandos del duelo.</b> Requiere <code>duelo.pisa-pvp-individual</code>.
 *
 * <p>🔑 <b>El duelo NO toca el toggle de PvP del jugador, y por eso alcanza con esto.</b>
 * Antes se lo prendia a la fuerza para que el scoreboard no mintiera, y como el toggle de
 * PvPManager es global —no sabe contra quien esta prendido— hacia falta una segunda mitad
 * que volviera a tapar el dano de los terceros. Ahora el toggle queda como el jugador lo
 * dejo, el cartel lo explica aparte, y toda esa maquinaria sobra.
 *
 * <p>La consecuencia que hay que entender antes de encender el override sigue en pie:
 * el consentimiento lo da el duenio del clan, asi que un miembro con el PvP apagado
 * queda alcanzado por una decision que no tomo el. Lo que esto acota es a quienes:
 * al rival pactado, no al servidor entero.
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
		this.guardia = Bukkit.getPluginManager().getPlugin("WorldGuard") != null
				? new GuardiaRegion(manager.isPisaClaims())
				: null;
		// El cartel del scoreboard usa la misma guarda: tiene que decir lo mismo que
		// hace el listener, o avisa "forzado" donde no se puede pegar.
		manager.setGuardia(guardia);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void alDaniar(EntityDamageByEntityEvent evento) {
		// El orden importa: este listener corre en CADA evento de dano del servidor,
		// mobs y granjas incluidos. Primero los chequeos que son una comparacion, y
		// recien al final resolver el clan, que recorre todos los clanes.
		if (!manager.hayDuelos()) {
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

		// Solo se destapa. Ya no hace falta la mitad que volvia a tapar: existia para
		// deshacer el forzado del toggle de PvPManager, y el duelo dejo de tocarlo.
		if (evento.isCancelled()) {
			destapar(evento, victima, atacante);
		}
	}

	/**
	 * Deja pasar un dano que otro plugin cancelo, solo entre los dos bandos del duelo.
	 */
	private void destapar(EntityDamageByEntityEvent evento, Player victima, Player atacante) {
		if (!manager.isPisaPvpIndividual()) {
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
