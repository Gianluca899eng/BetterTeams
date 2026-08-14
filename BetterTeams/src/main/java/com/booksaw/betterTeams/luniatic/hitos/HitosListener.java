package com.booksaw.betterTeams.luniatic.hitos;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.customEvents.post.PostDisbandTeamEvent;
import com.booksaw.betterTeams.team.TeamManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.UUID;

/**
 * Cuenta los bloques que mina cada clan.
 *
 * <h2>El orden de las comprobaciones no es cosmetico</h2>
 *
 * <p>{@code BlockBreakEvent} es de los eventos mas calientes del servidor: con una sola
 * persona minando son varios por segundo, y con veinte son cientos. Las comprobaciones van de
 * la mas barata y selectiva a la mas cara:
 *
 * <ol>
 * <li><b>El sistema esta encendido</b> — leer un campo.</li>
 * <li><b>El material cuenta</b> — un {@code EnumSet}, que es mirar un bit por el ordinal. Va
 *     primero de las de verdad porque descarta casi todo: tierra, madera, hojas, cultivos y
 *     cualquier cosa que se rompa construyendo salen por aca sin tocar nada mas.</li>
 * <li><b>Modo creativo</b> — leer un campo. El staff construyendo no infla el contador.</li>
 * <li><b>El bloque no lo acababa de poner un jugador</b> — un {@code remove} de mapa.</li>
 * <li><b>El jugador tiene clan</b> — lo ultimo, y aun asi es un solo {@code get} de mapa
 *     porque se usa {@code getTeamUUID} y no {@code getTeam}: hace falta el id para sumar un
 *     contador, no el objeto entero del clan. {@code getTeam} carga el clan desde el storage
 *     si no estaba en memoria, y eso en este evento seria un disparate.</li>
 * </ol>
 *
 * <p>No hay ni una escritura a disco en este camino: {@code sumar} toca un mapa en memoria y
 * el volcado lo hace una tarea aparte cada tantos segundos.
 */
public class HitosListener implements Listener {

	private final HitosManager manager;

	public HitosListener(HitosManager manager) {
		this.manager = manager;
	}

	/**
	 * MONITOR y sin eventos cancelados: solo se cuenta lo que de verdad se rompio.
	 *
	 * <p>Correr en MONITOR es lo que hace que un bloque protegido por ProtectionStones o por
	 * el spawn no sume, porque para cuando llega aca esos plugins ya cancelaron el evento.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onRomper(BlockBreakEvent event) {
		if (!manager.isHabilitado() || !manager.cuenta(event.getBlock().getType())) {
			return;
		}

		Player jugador = event.getPlayer();
		if (jugador.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		// Se consume siempre, tenga clan o no: la anotacion corresponde a una posicion, no a
		// una persona, y dejarla puesta ensuciaria la lista con posiciones ya rotas.
		if (manager.getColocados().consumir(event.getBlock())) {
			return;
		}

		TeamManager equipos = Team.getTeamManager();
		if (equipos == null) {
			return;
		}
		UUID clan = equipos.getTeamUUID(jugador);
		if (clan == null) {
			return;
		}

		manager.sumar(clan, 1);
	}

	/**
	 * Anota lo que un jugador coloca, para que romperlo despues no cuente.
	 *
	 * <p>Solo se anotan los materiales de la lista blanca. Es lo que mantiene la lista chica y
	 * util: al romper solo se consulta por esos materiales, asi que anotar un tablon o una
	 * antorcha seria gastar un lugar de la lista en algo que nunca se va a consultar, y de
	 * paso echar a codazos a la piedra que si importa.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onColocar(BlockPlaceEvent event) {
		if (!manager.isHabilitado() || !manager.cuenta(event.getBlockPlaced().getType())) {
			return;
		}
		manager.getColocados().anotar(event.getBlockPlaced());
	}

	/** Un mundo descargado no vuelve a recibir eventos: lo anotado ahi es memoria tirada. */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDescargarMundo(WorldUnloadEvent event) {
		manager.getColocados().olvidarMundo(event.getWorld());
	}

	/**
	 * Un clan disuelto suelta su contador.
	 *
	 * <p>Va en MONITOR y despues del hecho: el evento previo se puede cancelar, y borrar el
	 * contador de un clan que al final no se disolvio seria irreversible.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onDisolver(PostDisbandTeamEvent event) {
		if (event.getTeam() != null) {
			manager.olvidar(event.getTeam().getID());
		}
	}
}
