package com.booksaw.betterTeams.luniatic.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Estado de un menu abierto.
 *
 * <p>Identificar el menu por su holder y no por el titulo es lo que hace que
 * renombrar o traducir un menu no rompa los clics.
 */
public class MenuHolder implements InventoryHolder {

	public enum Tipo {
		LISTA, DETALLE
	}

	public enum Accion {
		ABRIR_DETALLE, UNIRSE, VOLVER, PAGINA, NADA
	}

	/** Que hace un slot cuando lo clickean. */
	public static class Ranura {
		public final Accion accion;
		public final String dato;

		public Ranura(Accion accion, String dato) {
			this.accion = accion;
			this.dato = dato;
		}
	}

	private final Tipo tipo;
	private final int pagina;
	private final String filtro;
	private final Map<Integer, Ranura> ranuras = new HashMap<>();
	private Inventory inventario;

	public MenuHolder(Tipo tipo, int pagina, String filtro) {
		this.tipo = tipo;
		this.pagina = pagina;
		this.filtro = filtro;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public int getPagina() {
		return pagina;
	}

	public String getFiltro() {
		return filtro;
	}

	public void asignar(int slot, Accion accion, String dato) {
		ranuras.put(slot, new Ranura(accion, dato));
	}

	public Ranura getRanura(int slot) {
		return ranuras.get(slot);
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
