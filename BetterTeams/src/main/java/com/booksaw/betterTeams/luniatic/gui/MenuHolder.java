package com.booksaw.betterTeams.luniatic.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Estado de un menu abierto: que hace cada slot.
 *
 * <p>Identificar el menu por su holder y no por el titulo es lo que permite
 * renombrarlo o traducirlo sin romper los clics, y evita que otro plugin con el
 * mismo titulo se coma los eventos.
 *
 * <p>Cada accion es un {@link Consumer} armado al construir la pantalla. No hace
 * falta serializar nada porque el menu vive lo que dura abierto.
 */
public class MenuHolder implements InventoryHolder {

	private final Map<Integer, Consumer<Player>> acciones = new HashMap<>();
	private final Map<Integer, Consumer<Player>> accionesDerecha = new HashMap<>();
	private Inventory inventario;

	public void asignar(int slot, Consumer<Player> accion) {
		acciones.put(slot, accion);
	}

	/** Dos acciones en el mismo item: izquierdo acepta, derecho rechaza. */
	public void asignarConClic(int slot, Consumer<Player> izquierdo, Consumer<Player> derecho) {
		acciones.put(slot, izquierdo);
		accionesDerecha.put(slot, derecho);
	}

	public Consumer<Player> getAccion(int slot) {
		return acciones.get(slot);
	}

	/** La accion del clic derecho, o la normal si ese slot no distingue. */
	public Consumer<Player> getAccion(int slot, boolean derecho) {
		if (derecho && accionesDerecha.containsKey(slot)) {
			return accionesDerecha.get(slot);
		}
		return acciones.get(slot);
	}

	public void setInventario(Inventory inventario) {
		this.inventario = inventario;
	}

	@NotNull
	@Override
	public Inventory getInventory() {
		return inventario;
	}
}
