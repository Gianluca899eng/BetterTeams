package com.booksaw.betterTeams.luniatic.hitos;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuerda las ultimas posiciones donde un jugador coloco un bloque, para no contarlas
 * cuando alguien las rompa.
 *
 * <p><b>Sin esto el sistema entero no vale nada.</b> El hito del cofre son 100.000 bloques;
 * poner y romper el mismo bloque en el mismo lugar los junta en un rato con un auto-clic, y
 * la lista blanca de materiales no lo impide: con pico de toque de seda una mena vuelve a
 * caer como mena, asi que un solo bloque alcanza para repetir el ciclo infinitas veces.
 *
 * <p><b>Por que no se resuelve con CoreProtect.</b> Su API sabe exactamente si un bloque lo
 * puso un jugador, pero cada consulta es un viaje a la base: en {@code BlockBreakEvent}, que
 * es de los eventos mas calientes que hay, eso es inaceptable con dos nucleos.
 *
 * <h2>Costo</h2>
 *
 * <p>Una entrada es una clave empaquetada en un {@code long}. El mapa esta <b>acotado</b>: al
 * pasarse de {@code capacidad} tira la entrada mas vieja, asi que la memoria tiene techo y no
 * crece con el tiempo de actividad del servidor. Con la capacidad por defecto son unas pocas
 * decenas de miles de entradas por mundo, del orden de un mega.
 *
 * <p>El olvido por antiguedad es <b>a favor del jugador honesto y en contra del que abusa</b>:
 * quien coloco un bloque hace meses y hoy lo rompe, lo cuenta; quien pone y rompe en el acto
 * cae siempre adentro de la ventana, que es justo el caso que hay que cortar.
 *
 * <h2>Concurrencia</h2>
 *
 * <p>El mapa de mundos es concurrente y cada mapa de posiciones se toma con su propio
 * candado. En Leaf los eventos de bloque son todos del hilo principal, asi que el candado
 * nunca se disputa y cuesta lo mismo que no tenerlo; en Folia, donde cada region tiene su
 * hilo, es lo que evita corromper la lista enlazada del {@code LinkedHashMap}.
 */
public class BloquesColocados {

	private final int capacidad;
	private final Map<UUID, Map<Long, Boolean>> porMundo = new ConcurrentHashMap<>();

	public BloquesColocados(int capacidad) {
		// Por debajo de mil la ventana es tan corta que un jugador construyendo normalmente
		// se saldria de ella, y romper lo que acaba de poner empezaria a contar.
		this.capacidad = Math.max(1000, capacidad);
	}

	/**
	 * Empaqueta la posicion en un {@code long}: 26 bits para X, 26 para Z y 12 para Y.
	 *
	 * <p>Es el mismo reparto que usa Minecraft para sus posiciones. El Y se corre 2048 antes
	 * de enmascarar porque el mundo llega a -64 y una altura negativa sin correr se recorta
	 * mal: quedaria pegada a otra altura y un bloque colocado en el cielo taparia a otro en
	 * la cueva de abajo.
	 */
	private static long clave(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF);
	}

	private Map<Long, Boolean> mapaDe(World mundo) {
		return porMundo.computeIfAbsent(mundo.getUID(), id -> {
			// accessOrder en false: la ventana es por antiguedad de colocado, no por uso. Con
			// true, mirar un bloque lo rejuveneceria y la ventana dejaria de tener sentido.
			LinkedHashMap<Long, Boolean> mapa = new LinkedHashMap<>(1024, 0.75f, false) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
					return size() > capacidad;
				}
			};
			return java.util.Collections.synchronizedMap(mapa);
		});
	}

	/** Anota que un jugador coloco este bloque. */
	public void anotar(Block bloque) {
		Map<Long, Boolean> mapa = mapaDe(bloque.getWorld());
		mapa.put(clave(bloque.getX(), bloque.getY(), bloque.getZ()), Boolean.TRUE);
	}

	/**
	 * Devuelve si el bloque lo habia colocado un jugador y, de paso, lo saca de la lista.
	 *
	 * <p><b>Sacarlo es parte del funcionamiento, no una limpieza.</b> Si quedara anotado, el
	 * bloque que la naturaleza vuelva a poner ahi —lava que se enfria, un aldeano que planta—
	 * tampoco contaria. Y sin sacarlo la lista se llenaria de posiciones que ya no existen,
	 * echando a codazos a las que si importan.
	 */
	public boolean consumir(Block bloque) {
		Map<Long, Boolean> mapa = porMundo.get(bloque.getWorld().getUID());
		if (mapa == null) {
			return false;
		}
		return mapa.remove(clave(bloque.getX(), bloque.getY(), bloque.getZ())) != null;
	}

	/** Suelta lo anotado de un mundo que se descarga. */
	public void olvidarMundo(World mundo) {
		porMundo.remove(mundo.getUID());
	}

	/** Cuantas posiciones hay anotadas en total. Para el comando de staff. */
	public int getAnotados() {
		int total = 0;
		for (Map<Long, Boolean> mapa : porMundo.values()) {
			total += mapa.size();
		}
		return total;
	}
}
