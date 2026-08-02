package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.luniatic.Texto;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Barra de progreso del duelo en curso.
 *
 * <p>Existe por lo mismo que la de misiones: sin ella el jugador no sabe como va
 * hasta que el duelo termina. Muestra el rival, el marcador y el tiempo que queda.
 *
 * <p><b>La barra mide cuanto te falta para PERDER</b>, no para ganar: se llena con
 * tus propias caidas. Es la informacion que cambia lo que hacer — cuando esta por
 * llenarse conviene replegarse o rendirse.
 *
 * <p><b>No hay polling por jugador.</b> Se redibuja cuando pasa algo (arranca el
 * duelo, cae alguien, termina) y de arrastre en el chequeo de vencimientos que ya
 * corre cada diez segundos, que es lo unico que necesita el reloj. Con dos nucleos
 * (P3) una tarea por jugador para redibujar una barra seria trabajo por tick a
 * cambio de nada.
 */
public class DueloBossBar implements Listener {

	private static final String CUERPO = "&#E4D9FF";
	private static final String MARCA = "&#9235FF";

	private final DueloManager manager;
	private final Map<UUID, BossBar> barras = new HashMap<>();

	public DueloBossBar(DueloManager manager) {
		this.manager = manager;
	}

	/** Redibuja la barra de todos los conectados de ese clan. */
	public void refrescar(Team clan) {
		Duelo duelo = manager.getDuelo(clan);
		if (duelo == null) {
			quitar(clan);
			return;
		}
		Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
		int propias = duelo.getBajas(clan.getID());
		int ajenas = rival == null ? 0 : duelo.getBajas(rival.getID());
		int objetivo = Math.max(1, manager.getObjetivoBajas());
		long minutos = duelo.segundosRestantes(System.currentTimeMillis()) / 60;
		String nombreRival = rival == null ? "?" : Texto.limpiar(rival.getName());

		// Al aliado hay que decirle de quien es el duelo: el no lo pacto, y su barra
		// tiene que explicarle por que de golpe lo pueden matar.
		String encabezado;
		if (duelo.esPrincipal(clan.getID())) {
			encabezado = CUERPO + "Duelo contra " + MARCA + nombreRival;
		} else {
			Team principal = Team.getTeam(duelo.getPrincipalDe(clan.getID()));
			encabezado = CUERPO + "Apoyando a " + MARCA
					+ (principal == null ? "?" : Texto.limpiar(principal.getName()))
					+ CUERPO + " contra " + MARCA + nombreRival;
		}

		String titulo = Texto.col(encabezado
				+ CUERPO + "   caidas " + MARCA + propias + CUERPO + " - " + MARCA + ajenas
				+ CUERPO + "   quedan " + MARCA + minutos + CUERPO + " min");
		double progreso = Math.min(1.0, propias / (double) objetivo);

		for (Player jugador : clan.getMembers().getOnlinePlayers()) {
			BossBar barra = barras.computeIfAbsent(jugador.getUniqueId(),
					id -> Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_10));
			barra.setTitle(titulo);
			barra.setProgress(progreso);
			if (!barra.getPlayers().contains(jugador)) {
				barra.addPlayer(jugador);
			}
			barra.setVisible(true);
		}
	}

	/** Saca la barra a todos los del clan. */
	public void quitar(Team clan) {
		if (clan == null) {
			return;
		}
		for (Player jugador : clan.getMembers().getOnlinePlayers()) {
			quitar(jugador);
		}
	}

	public void quitar(Player jugador) {
		BossBar barra = barras.remove(jugador.getUniqueId());
		if (barra != null) {
			barra.removeAll();
		}
	}

	/** La llama el chequeo de vencimientos: es lo unico que mueve el reloj. */
	public void refrescarTodos() {
		for (Team clan : manager.getClanesEnDuelo()) {
			refrescar(clan);
		}
	}

	public void quitarTodas() {
		for (BossBar barra : barras.values()) {
			barra.removeAll();
		}
		barras.clear();
	}

	/**
	 * La barra vive del lado del cliente y se pierde al desconectar: sin esto, un
	 * jugador que vuelve en medio de un duelo se queda sin barra hasta la proxima
	 * caida.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void alEntrar(PlayerJoinEvent evento) {
		Team clan = Team.getTeam(evento.getPlayer());
		if (clan != null && manager.getDuelo(clan) != null) {
			refrescar(clan);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void alSalir(PlayerQuitEvent evento) {
		quitar(evento.getPlayer());
	}
}
