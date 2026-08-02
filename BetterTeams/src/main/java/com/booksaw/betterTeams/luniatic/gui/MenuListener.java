package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

/**
 * Clics de los menus de clanes.
 *
 * <p>Cancela todo lo que pase adentro del menu antes de decidir que hacer, y
 * tambien el inventario del jugador mientras el menu esta abierto: si un menu
 * deja sacar su propio item, cualquier boton es un generador de items.
 *
 * <p>🔑 <b>El inventario clickeado se reconoce por su holder, nunca comparando
 * inventarios.</b> {@code CraftInventory} no implementa {@code equals}, asi que
 * {@code getInventory().equals(getClickedInventory())} devuelve falso aunque sean
 * el mismo inventario: son dos envoltorios distintos. Con esa comparacion el menu
 * cancelaba el clic pero no ejecutaba nunca la accion, y parecia roto sin tirar un
 * solo error.
 *
 * <p>Tampoco lleva {@code ignoreCancelled}: el clic ya viene cancelado por este
 * mismo listener, y si otro plugin lo cancelara antes, el menu dejaria de responder.
 */
public class MenuListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL)
	public void alClickear(InventoryClickEvent evento) {
		if (!(evento.getInventory().getHolder() instanceof MenuHolder)) {
			return;
		}
		evento.setCancelled(true);

		if (!(evento.getWhoClicked() instanceof Player)) {
			return;
		}

		Inventory clickeado = evento.getClickedInventory();
		if (clickeado == null || !(clickeado.getHolder() instanceof MenuHolder)) {
			// Clickeo su propio inventario con el menu abierto: ya quedo cancelado.
			return;
		}

		MenuHolder holder = (MenuHolder) clickeado.getHolder();
		Consumer<Player> accion = holder.getAccion(evento.getSlot(), evento.isRightClick());
		if (accion == null) {
			return;
		}

		// Un tick despues, no dentro del evento: abrir un inventario mientras Bukkit
		// todavia esta procesando el clic deja items fantasma del lado del cliente.
		Player jugador = (Player) evento.getWhoClicked();
		Main.plugin.getFoliaLib().getScheduler().runAtEntityLater(jugador,
				() -> accion.accept(jugador), 1L);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void alArrastrar(InventoryDragEvent evento) {
		if (evento.getInventory().getHolder() instanceof MenuHolder) {
			evento.setCancelled(true);
		}
	}
}
