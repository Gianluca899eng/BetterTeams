package com.booksaw.betterTeams.luniatic;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.customEvents.post.PostCreateTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostDisbandTeamEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le da a cada clan nuevo un icono y un color propios, sin que nadie los elija.
 *
 * <p>🔑 <b>El problema no es que el default sea feo: es que todos comparten el mismo.</b> Un
 * clan recien creado nacia con el primer icono de la lista y con {@code GOLD}, asi que los
 * primeros veinte clanes del servidor eran veinte filas identicas en el menu y veinte nombres
 * del mismo color sobre la cabeza. Justo la unica cosa que el icono y el color existen para
 * evitar. Y elegirlos es lo primero que nadie hace: hay que saber que la pantalla existe.
 *
 * <p>El reparto es <b>el menos usado gana</b>, no un sorteo. Un sorteo repite: con 14 colores,
 * a partir del quinto clan es mas probable que salga uno repetido que uno libre. Recorriendo
 * lo que ya hay, los primeros 14 clanes salen los 14 distintos, y recien despues empieza a
 * repetir — que es lo mejor que se puede hacer con una paleta finita.
 *
 * <p>⚠️ <b>Mira los clanes CARGADOS, no todos los que existen.</b> Enumerar todos obligaria a
 * levantar del disco cada clan del servidor para crear uno. O sea que la unicidad es <b>lo
 * mejor posible, no una garantia</b>, y esta bien que asi sea: dos clanes con el mismo color
 * es un detalle estetico, y cargar el servidor entero para evitarlo no lo vale.
 *
 * <p>Nada de esto le saca la eleccion a nadie: los dos se cambian desde Ajustes cuando quieran.
 */
public class ClanIdentidad implements Listener {

	/**
	 * Los colores que se reparten solos.
	 *
	 * <p>Son 14 de los 16 de {@code /clan color}: quedan afuera <b>negro y gris oscuro</b>,
	 * que sobre el fondo del nombre flotante no se leen. Elegirlos a mano se puede igual —
	 * la decision de que un clan sea ilegible es del clan.
	 */
	private static final ChatColor[] PALETA = {
			ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.AQUA,
			ChatColor.BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_RED, ChatColor.DARK_GREEN,
			ChatColor.DARK_AQUA, ChatColor.DARK_BLUE, ChatColor.DARK_PURPLE, ChatColor.WHITE,
			ChatColor.GRAY
	};

	private final ClanIconos iconos;

	public ClanIdentidad(ClanIconos iconos) {
		this.iconos = iconos;
	}

	/**
	 * Corre despues de que el clan quedo creado.
	 *
	 * <p>En {@code MONITOR} y sin tocar el evento: no decide nada sobre la creacion, solo le
	 * cuelga la identidad al clan que ya existe.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void alCrear(PostCreateTeamEvent evento) {
		Team clan = evento.getTeam();
		if (clan == null) {
			return;
		}
		try {
			iconos.asignarLibre(clan.getID());
			clan.setColor(colorLibre());
		} catch (Throwable t) {
			// Un clan sin icono ni color propios sigue siendo un clan: esto no puede
			// tumbar la creacion, que es lo unico que el jugador esta esperando.
			Main.plugin.getLogger().warning("[clanes] no pude darle identidad al clan "
					+ clan.getName() + ": " + t.getMessage());
		}
	}

	/**
	 * Corre despues de que el clan dejo de existir.
	 *
	 * <p>Suelta lo que quedaba colgado de su id: el icono —que si no queda ocupando un lugar
	 * de la lista para siempre, y agota los libres con clanes muertos— y el duelo, que sin
	 * esto seguia corriendo contra un fantasma hasta que venciera el reloj.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void alDesbandarse(PostDisbandTeamEvent evento) {
		Team clan = evento.getTeam();
		if (clan == null) {
			return;
		}
		UUID id = clan.getID();
		try {
			if (Main.plugin.getDueloManager() != null) {
				Main.plugin.getDueloManager().alDesbandarse(id);
			}
			iconos.olvidar(id);
		} catch (Throwable t) {
			Main.plugin.getLogger().warning("[clanes] no pude soltar lo del clan "
					+ clan.getName() + ": " + t.getMessage());
		}
	}

	/** El color de la paleta que menos clanes esten usando. */
	private ChatColor colorLibre() {
		Map<ChatColor, Integer> uso = new HashMap<>();
		for (ChatColor color : PALETA) {
			uso.put(color, 0);
		}
		for (Map.Entry<UUID, Team> entrada : Team.getTeamManager().getLoadedTeamListClone().entrySet()) {
			ChatColor color = entrada.getValue().getColor();
			// merge y no put: un clan pintado de negro a mano no tiene que entrar en el
			// reparto, pero tampoco puede romperlo.
			uso.computeIfPresent(color, (c, veces) -> veces + 1);
		}
		ChatColor elegido = PALETA[0];
		int menos = Integer.MAX_VALUE;
		for (ChatColor color : PALETA) {
			int veces = uso.get(color);
			if (veces < menos) {
				menos = veces;
				elegido = color;
			}
		}
		return elegido;
	}
}
