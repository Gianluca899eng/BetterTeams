package com.booksaw.betterTeams.luniatic.hitos;

import org.bukkit.Material;

/**
 * Un escalon de la escalera de hitos: un objetivo de bloques minados y lo que da al llegar.
 *
 * <p>Es inmutable y se arma una sola vez al cargar el config. No guarda nada del progreso de
 * ningun clan: el progreso vive en {@code HitosManager} y el estado "desbloqueado" se deriva
 * comparando el contador contra {@link #getUmbral()}. Guardar el estado aparte obligaria a
 * mantener dos fuentes de verdad sincronizadas, y bastaria un guardado perdido para que un
 * clan quedara con el cofre abierto y el contador en cero, o al reves.
 */
public final class Hito {

	private final String id;
	private final String nombre;
	private final String descripcion;
	private final long umbral;
	private final Material icono;
	private final Recompensa recompensa;
	private final int cantidad;

	public Hito(String id, String nombre, String descripcion, long umbral, Material icono,
			Recompensa recompensa, int cantidad) {
		this.id = id;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.umbral = umbral;
		this.icono = icono;
		this.recompensa = recompensa;
		this.cantidad = cantidad;
	}

	/** La clave del config. Se usa en el comando de staff y en los mensajes de consola. */
	public String getId() {
		return id;
	}

	/** El nombre que ve el jugador. */
	public String getNombre() {
		return nombre;
	}

	/** Una linea explicando que da. */
	public String getDescripcion() {
		return descripcion;
	}

	/** Bloques que el clan tiene que minar entre todos para alcanzarlo. */
	public long getUmbral() {
		return umbral;
	}

	/** El item con el que se muestra en el menu. */
	public Material getIcono() {
		return icono;
	}

	public Recompensa getRecompensa() {
		return recompensa;
	}

	/**
	 * Cuanto suma la recompensa, para las que son numericas ({@code WARPS}, {@code LUGARES}).
	 *
	 * <p>Para {@link Recompensa#COFRE} no se usa: es un interruptor, no una cantidad.
	 */
	public int getCantidad() {
		return cantidad;
	}

	/** Si un clan con ese total de bloques ya lo alcanzo. */
	public boolean alcanzado(long bloques) {
		return bloques >= umbral;
	}

	/**
	 * Que fraccion del hito lleva el clan, entre 0 y 1.
	 *
	 * <p>Acotado arriba en 1 para que la barra del menu no se pase de largo cuando el clan
	 * sigue minando despues de alcanzarlo.
	 */
	public double progreso(long bloques) {
		if (umbral <= 0) {
			return 1;
		}
		double fraccion = (double) bloques / (double) umbral;
		return fraccion > 1 ? 1 : fraccion;
	}
}
