package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.luniatic.Comandos;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Impide reaparecer al lado de una base rival mientras dura el duelo.
 *
 * <p>Es la regla de <b>destino</b>, y es distinta de la de {@link DueloComandoListener},
 * que mira como moriste. Aquella no puede cubrir el regreso por {@code /home}, por cama o
 * por un warp, porque ninguno depende de la muerte. Esta si: se pregunta a donde vas.
 *
 * <p>Cubre tres caminos:
 * <ul>
 * <li><b>Teletransporte</b> por comando o por plugin — {@code /home}, {@code /dback},
 * {@code /back}, {@code /tpa}, warps. Se cancela.
 * <li><b>Respawn de cama o de ancla</b> — se desvia al spawn del mundo en vez de
 * cancelarse, porque un respawn no se puede negar: hay que mandarlo a algun lado.
 * <li><b>{@code /sethome}</b> — se corta antes, para que no se pueda plantar la casa
 * adentro de la zona y despues discutir si el teletransporte valia.
 * </ul>
 *
 * <p>🔑 <b>Las perlas de ender y los portales NO entran.</b> Eso es movilidad de combate:
 * cortarla cambia como se pelea, y lo que esta regla ataca es el regreso gratis. Por eso
 * el filtro es por causa del teletransporte y no por distancia recorrida.
 */
public class DueloAparicionListener implements Listener {

	private final DueloManager manager;

	public DueloAparicionListener(DueloManager manager) {
		this.manager = manager;
	}

	/**
	 * Corta el teletransporte cuyo destino cae en zona de base rival.
	 *
	 * <p>Solo causas de comando y de plugin. CMI mueve al jugador como {@code PLUGIN} y
	 * los comandos vanilla como {@code COMMAND}, asi que entre las dos queda cubierto todo
	 * lo que sea "volver", y afuera lo que sea moverse peleando.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void alTeletransportarse(PlayerTeleportEvent evento) {
		if (!manager.isBloquearTeletransporte()) {
			return;
		}
		PlayerTeleportEvent.TeleportCause causa = evento.getCause();
		if (causa != PlayerTeleportEvent.TeleportCause.COMMAND
				&& causa != PlayerTeleportEvent.TeleportCause.PLUGIN) {
			return;
		}
		Location destino = evento.getTo();
		if (destino == null || !manager.aparicionProhibida(evento.getPlayer(), destino)) {
			return;
		}
		evento.setCancelled(true);
		MessageManager.sendMessage(evento.getPlayer(), "duelo.aparicion_bloqueada",
				String.valueOf(manager.getRadioBaseRival()));
	}

	/**
	 * Desvia el respawn cuando la cama quedo adentro de la zona.
	 *
	 * <p>No se cancela: un respawn siempre termina en algun lado, asi que la unica salida
	 * es mandarlo al spawn del mundo. Se avisa, o el jugador cree que perdio la cama.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void alReaparecer(PlayerRespawnEvent evento) {
		if (!manager.isBloquearRespawnCama() || (!evento.isBedSpawn() && !evento.isAnchorSpawn())) {
			return;
		}
		Player jugador = evento.getPlayer();
		Location cama = evento.getRespawnLocation();
		if (!manager.aparicionProhibida(jugador, cama)) {
			return;
		}
		// 🔑 El spawn es el del mundo DE LA CAMA, no el del mundo del jugador: al morir en
		// el nether con la cama en el overworld, jugador.getWorld() es el nether y lo
		// mandaria a reaparecer ahi.
		if (cama == null || cama.getWorld() == null) {
			return;
		}
		evento.setRespawnLocation(cama.getWorld().getSpawnLocation());
		MessageManager.sendMessage(jugador, "duelo.respawn_desviado",
				String.valueOf(manager.getRadioBaseRival()));
	}

	/**
	 * Corta {@code /sethome} parado adentro de la zona.
	 *
	 * <p>Lo primero es {@code hayDuelos()} adentro de {@link DueloManager}, pero antes esta
	 * el filtro del nombre, que es una consulta a un hash: este listener corre en cada
	 * comando de cada jugador.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void alEscribirComando(PlayerCommandPreprocessEvent evento) {
		// Esto corre en CADA comando de cada jugador, asi que lo primero es lo mas barato:
		// sin duelos en curso no se parsea el mensaje ni se aloca una sola cadena. Es la
		// misma guarda que ya tiene DueloComandoListener, por el mismo motivo.
		if (!manager.hayDuelos()) {
			return;
		}
		String comando = normalizar(evento.getMessage());
		if (!manager.esComandoDeCasa(comando)) {
			return;
		}
		Player jugador = evento.getPlayer();
		if (!manager.aparicionProhibida(jugador, jugador.getLocation())) {
			return;
		}
		evento.setCancelled(true);
		MessageManager.sendMessage(jugador, "duelo.sethome_bloqueado",
				String.valueOf(manager.getRadioBaseRival()));
	}

	/**
	 * Igual que en {@link DueloComandoListener}. Ver {@link Comandos}: tambien desenvuelve
	 * {@code /cmi sethome}, que es la forma canonica del alias {@code /sethome}.
	 */
	private static String normalizar(String mensaje) {
		return Comandos.nombre(mensaje);
	}
}
