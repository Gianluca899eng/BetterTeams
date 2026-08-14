package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
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
		// La guarda la crea Main y vive en el manager: la usan tambien el corte de vuelo,
		// la regla de aparicion y el cartel del scoreboard. Crearla aca ataba todo eso a
		// que 'pisa-pvp-individual' estuviera en true, que es de lo que ninguno depende.
		this.guardia = manager.getGuardia();
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
		// Los dos tienen que estar adentro del duelo. Alcanza con que uno se haya
		// bajado para que esto siga siendo un ataque normal, sujeto al /pvp de cada uno:
		// el que no participa no puede pegar amparado en el duelo ni recibir por el.
		//
		// Se pregunta contra el duelo ya resuelto y no con manager.esParticipante(...),
		// que volveria a recorrer todos los clanes por cada uno de los dos.
		Duelo duelo = manager.getDuelo(clanVictima);
		if (duelo == null || !duelo.esParticipante(victima.getUniqueId())
				|| !duelo.esParticipante(atacante.getUniqueId())) {
			depurar("alguno no participa del duelo: " + atacante.getName() + " -> " + victima.getName());
			return;
		}
		if (guardia != null && !guardia.permitePvp(victima.getLocation())) {
			depurar("WorldGuard prohibe PvP en " + resumen(victima) + ", no se destapa");
			return;
		}

		evento.setCancelled(false);
		// Solo se anota con debug: sin el vigilante registrado nadie limpia el campo, y
		// quedaria reteniendo el ultimo evento de dano para siempre.
		if (manager.isDebug()) {
			ultimoDestapado = evento;
		}
		depurar("destapado: " + atacante.getName() + " -> " + victima.getName());
	}

	/**
	 * El vigilante de la ultima palabra, para que {@code Main} lo registre aparte.
	 *
	 * <p>Va en un listener propio y no aca porque se cuelga de
	 * {@code EntityDamageByEntityEvent} <b>sin</b> {@code ignoreCancelled}: registrado
	 * siempre, se despacharia en cada golpe de cada mob del servidor —granjas incluidas—
	 * para comparar una referencia y no hacer nada. Solo tiene sentido con
	 * {@code duelo.debug} encendido.
	 */
	public Listener vigilante() {
		return new Vigilante();
	}

	/**
	 * Ultimo en la fila: si aca el evento volvio a estar cancelado, otro plugin lo
	 * re-cancelo despues nuestro y el problema es de orden de prioridades, no de
	 * este listener.
	 */
	private final class Vigilante implements Listener {

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
		public void alFinal(EntityDamageByEntityEvent evento) {
			if (evento != ultimoDestapado) {
				return;
			}
			ultimoDestapado = null;
			if (evento.isCancelled()) {
				Main.plugin.getLogger().warning("[duelo] el dano se destapo y OTRO PLUGIN lo volvio a cancelar; "
						+ "el override no puede ganar en esta cadena de prioridades");
			}
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
