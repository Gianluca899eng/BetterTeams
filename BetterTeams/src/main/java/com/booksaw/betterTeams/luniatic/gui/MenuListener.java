package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.function.Consumer;

/**
 * Clics de los menus de clanes.
 *
 * <p>Cancela todo lo que pase adentro del menu antes de decidir que hacer, y
 * tambien el inventario del jugador mientras el menu esta abierto: si un menu
 * deja sacar su propio item, cualquier boton es un generador de items.
 */
public class MenuListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void alClickear(InventoryClickEvent evento) {
		if (!(evento.getInventory().getHolder() instanceof MenuHolder)) {
			return;
		}
		evento.setCancelled(true);

		if (!(evento.getWhoClicked() instanceof Player)) {
			return;
		}
		if (!evento.getInventory().equals(evento.getClickedInventory())) {
			return;
		}

		MenuHolder holder = (MenuHolder) evento.getInventory().getHolder();
		Consumer<Player> accion = holder.getAccion(evento.getSlot());
		if (accion == null) {
			return;
		}

		// Un tick despues, no dentro del evento: abrir un inventario mientras Bukkit
		// todavia esta procesando el clic deja items fantasma del lado del cliente.
		Player jugador = (Player) evento.getWhoClicked();
		Main.plugin.getFoliaLib().getScheduler().runAtEntityLater(jugador,
				() -> accion.accept(jugador), 1L);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void alArrastrar(InventoryDragEvent evento) {
		if (evento.getInventory().getHolder() instanceof MenuHolder) {
			evento.setCancelled(true);
		}
	}
}
