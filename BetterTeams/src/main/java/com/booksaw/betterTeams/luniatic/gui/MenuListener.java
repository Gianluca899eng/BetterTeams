package com.booksaw.betterTeams.luniatic.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Clics de los menus de clanes.
 *
 * <p>Cancela todo lo que pase adentro del menu antes de decidir que hacer: si un
 * menu deja sacar su propio item, cualquier boton se convierte en un generador de
 * items.
 */
public class MenuListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void alClickear(InventoryClickEvent evento) {
		if (!(evento.getInventory().getHolder() instanceof MenuHolder)) {
			return;
		}
		// Vale tambien para el inventario del jugador mientras el menu esta abierto:
		// asi no hay shift-click ni numero de hotbar que meta o saque nada.
		evento.setCancelled(true);

		if (!(evento.getWhoClicked() instanceof Player)) {
			return;
		}
		if (!evento.getInventory().equals(evento.getClickedInventory())) {
			return;
		}

		Player jugador = (Player) evento.getWhoClicked();
		MenuHolder holder = (MenuHolder) evento.getInventory().getHolder();
		MenuHolder.Ranura ranura = holder.getRanura(evento.getSlot());
		if (ranura == null) {
			return;
		}

		switch (ranura.accion) {
			case ABRIR_DETALLE:
				MenuClanes.abrirDetalle(jugador, ranura.dato);
				break;
			case PAGINA:
				MenuClanes.abrirLista(jugador, Integer.parseInt(ranura.dato), holder.getFiltro());
				break;
			case VOLVER:
				MenuClanes.abrirLista(jugador, holder.getPagina(), holder.getFiltro());
				break;
			case UNIRSE:
				// Se delega en el comando que ya existe, para que sigan valiendo
				// permisos, limites, baneos, costos y cooldowns.
				jugador.closeInventory();
				jugador.performCommand("team join " + ranura.dato);
				break;
			default:
				break;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void alArrastrar(InventoryDragEvent evento) {
		if (evento.getInventory().getHolder() instanceof MenuHolder) {
			evento.setCancelled(true);
		}
	}
}
