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
import java.util.List;
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
	/** Separa las tres partes sin gastar el ancho que gastaban los espacios. */
	private static final String SEPARADOR = " &#7162FF| ";

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
		// Armar el titulo pasa por MiniMessage. Si no hay nadie de ese clan
		// conectado no hay a quien mostrarselo, asi que ni se arma.
		List<Player> conectados = clan.getMembers().getOnlinePlayers();
		if (conectados.isEmpty()) {
			return;
		}

		Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
		int propias = duelo.getBajas(clan.getID());
		int ajenas = rival == null ? 0 : duelo.getBajas(rival.getID());
		long minutos = duelo.segundosRestantes(System.currentTimeMillis()) / 60;
		long duracionMillis = manager.getDuracionMillis();
		String nombreRival = rival == null ? "?" : Texto.limpiar(rival.getName());

		// Corto a proposito: la barra se lee de reojo en medio de una pelea. El
		// marcador y el reloj van sin etiqueta —se entienden por la forma— y el
		// separador ocupa menos que los espacios que tenia antes.
		String encabezado;
		if (duelo.esPrincipal(clan.getID())) {
			encabezado = CUERPO + "Duelo vs " + MARCA + nombreRival;
		} else {
			// Al aliado hay que decirle de quien es el duelo: el no lo pacto.
			Team principal = Team.getTeam(duelo.getPrincipalDe(clan.getID()));
			encabezado = CUERPO + "Apoyo a " + MARCA
					+ (principal == null ? "?" : Texto.limpiar(principal.getName()))
					+ CUERPO + " vs " + MARCA + nombreRival;
		}

		String titulo = Texto.col(encabezado
				+ SEPARADOR + MARCA + propias + CUERPO + "-" + MARCA + ajenas
				+ SEPARADOR + MARCA + minutos + CUERPO + "m");

		// La barra es el reloj: arranca llena y se vacia. Antes se llenaba con las
		// caidas propias, y una barra que crece se lee como progreso hacia algo
		// bueno justo cuando significa lo contrario. El marcador ya esta en el
		// titulo; lo que la barra aporta es cuanto queda.
		double progreso = duracionMillis <= 0 ? 1.0
				: Math.max(0.0, Math.min(1.0,
						(duelo.getFinMillis() - System.currentTimeMillis()) / (double) duracionMillis));

		for (Player jugador : conectados) {
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
