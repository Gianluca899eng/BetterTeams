package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

/**
 * Corta los comandos de regreso mientras el clan esta en duelo.
 *
 * <p>El problema que resuelve: {@code /dback} devuelve al lugar donde moriste. El
 * tag de combate de PvPManager ya bloquea todos los comandos mientras peleas, pero
 * <b>al morir el tag se va con vos</b>: revivis sin tag y podes volver al punto
 * exacto de la pelea al instante, sin caminar. Con el duelo resolviendose por
 * caidas, eso abarata morir.
 *
 * <p><b>El corte es quirurgico, no general.</b> No alcanza con estar en duelo: hace
 * falta que tu <b>ultima caida haya sido a manos del clan rival</b>. Si te ahogaste
 * o te caiste de un risco, volver ahi no te devuelve a ninguna pelea y el comando
 * anda. Asi el que compro vuelo o rango no pierde el comando por media hora sin
 * motivo.
 *
 * <p>Tambien entra {@code /back} por defecto, que no devuelve a la muerte pero
 * habilita el mismo patron un paso antes: teletransportarse fuera de la pelea y
 * volver. La lista es configurable en {@code duelo.comandos-bloqueados}.
 *
 * <p>Alcanza a los dos bandos y a los aliados arrastrados: la pregunta es si el
 * clan del jugador esta en un duelo, no de que lado.
 */
public class DueloComandoListener implements Listener {

	private final DueloManager manager;

	public DueloComandoListener(DueloManager manager) {
		this.manager = manager;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void alEscribirComando(PlayerCommandPreprocessEvent evento) {
		// Esto corre en cada comando de cada jugador, asi que lo primero tiene que ser
		// lo mas barato: sin duelos en curso no se parsea ni se aloca nada.
		if (!manager.hayDuelos()) {
			return;
		}

		String comando = normalizar(evento.getMessage());
		if (comando.isEmpty() || !manager.esComandoBloqueado(comando)) {
			return;
		}

		Player jugador = evento.getPlayer();
		// La marca es una consulta a un hash y se resuelve sin recorrer clanes, asi que
		// va antes que Team.getTeam. Sin caida a manos del rival no hay nada que cortar.
		if (!manager.cayoPorRival(jugador.getUniqueId())) {
			return;
		}

		Team clan = Team.getTeam(jugador);
		if (clan == null || manager.getDuelo(clan) == null) {
			return;
		}

		evento.setCancelled(true);
		MessageManager.sendMessage(jugador, "duelo.comando_bloqueado", comando);
	}

	/**
	 * Deja el nombre del comando en minusculas, sin barra, sin argumentos y sin
	 * namespace.
	 *
	 * <p>El namespace importa: {@code /cmi:dback} es el mismo comando y esquivaria
	 * una comparacion contra el nombre pelado.
	 */
	private static String normalizar(String mensaje) {
		if (mensaje == null) {
			return "";
		}
		String texto = mensaje.trim();
		if (texto.startsWith("/")) {
			texto = texto.substring(1);
		}
		int espacio = texto.indexOf(' ');
		if (espacio >= 0) {
			texto = texto.substring(0, espacio);
		}
		int namespace = texto.indexOf(':');
		if (namespace >= 0) {
			texto = texto.substring(namespace + 1);
		}
		return texto.toLowerCase(Locale.ROOT);
	}
}
