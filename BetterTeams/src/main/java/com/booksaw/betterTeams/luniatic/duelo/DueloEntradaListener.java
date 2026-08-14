package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Entrega lo que quedo pendiente para el que estaba desconectado.
 *
 * <p>Un duelo la pactan dos lideres, no el clan entero conectado. El que no estaba se
 * enteraria al volver, sin aviso y sin el libro — y es el que menos sabe en que lo metieron.
 *
 * <p>La entrega va <b>un tick despues</b> del evento: durante {@code PlayerJoinEvent} el
 * inventario todavia se esta armando del lado del cliente, asi que un item agregado ahi
 * puede no verse hasta reabrir el inventario.
 */
public class DueloEntradaListener implements Listener {

	private final DueloManager manager;

	public DueloEntradaListener(DueloManager manager) {
		this.manager = manager;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void alEntrar(PlayerJoinEvent evento) {
		Player jugador = evento.getPlayer();
		// Lo primero es una consulta a un mapa: la enorme mayoria de las entradas no tiene
		// nada pendiente y no tiene que costar ni programar una tarea.
		if (!manager.tienePendientes(jugador.getUniqueId())) {
			return;
		}
		Main.plugin.getFoliaLib().getScheduler().runAtEntityLater(jugador,
				tarea -> {
					if (jugador.isOnline()) {
						manager.entregarPendientes(jugador);
					}
				}, 20L);
	}
}
