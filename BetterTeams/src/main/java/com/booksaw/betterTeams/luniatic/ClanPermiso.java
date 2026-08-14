package com.booksaw.betterTeams.luniatic;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.post.PostCreateTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostDisbandTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostPlayerJoinTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostPlayerLeaveTeamEvent;
import com.booksaw.betterTeams.team.TeamManager;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Da el permiso luniatic.clan a quien esta en un clan y se lo saca al salir.
 *
 * Existe porque Quests solo sabe gatear una mision por permiso. Sin esto las misiones de clan
 * se pueden aceptar sin tener clan, y al completarlas el puntaje se pierde en silencio.
 *
 * Tiene que ser un permiso de verdad y no un control del menu: /quests esta en la lista blanca
 * de ProAntiTab, asi que una mision se puede tomar por comando salteando la GUI.
 *
 * Se usan attachments y no un grupo de LuckPerms para no meter la pertenencia a un clan en el
 * arbol de permisos, que es donde vive la escalera de rangos.
 */
public class ClanPermiso implements Listener {

	public static final String PERMISO = "luniatic.clan";

	private final Plugin plugin;
	private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

	public ClanPermiso(Plugin plugin) {
		this.plugin = plugin;
	}

	/** Recalcula el permiso de un jugador conectado. Idempotente: se puede llamar de mas. */
	public void actualizar(Player jugador) {
		if (jugador == null) {
			return;
		}
		UUID id = jugador.getUniqueId();
		TeamManager manager = Team.getTeamManager();
		boolean enClan = manager != null && manager.isInTeam(jugador);
		PermissionAttachment attachment = attachments.get(id);

		// Solo se recalcula si de verdad cambio algo. recalculatePermissions() invalida el arbol
		// entero del jugador, y el caso mas comun -- entrar al servidor sin clan -- no cambia nada.
		if (enClan && attachment == null) {
			attachment = jugador.addAttachment(plugin);
			attachment.setPermission(PERMISO, true);
			attachments.put(id, attachment);
			jugador.recalculatePermissions();
		} else if (!enClan && attachment != null) {
			jugador.removeAttachment(attachment);
			attachments.remove(id);
			jugador.recalculatePermissions();
		}
	}

	private void actualizar(OfflinePlayer jugador) {
		if (jugador != null && jugador.isOnline() && jugador.getPlayer() != null) {
			actualizar(jugador.getPlayer());
		}
	}

	/** Al habilitar el plugin ya puede haber gente conectada, por ejemplo tras un reload. */
	public void actualizarATodos() {
		for (Player jugador : plugin.getServer().getOnlinePlayers()) {
			actualizar(jugador);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		actualizar(event.getPlayer());
	}

	// El attachment muere con la sesion; solo hay que soltar la referencia para no acumular
	// una entrada por cada jugador que paso alguna vez.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		attachments.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntrarAlClan(PostPlayerJoinTeamEvent event) {
		actualizar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSalirDelClan(PostPlayerLeaveTeamEvent event) {
		actualizar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCrearClan(PostCreateTeamEvent event) {
		actualizar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDisolverClan(PostDisbandTeamEvent event) {
		if (event.getPrevMembers() == null) {
			return;
		}
		for (TeamPlayer miembro : event.getPrevMembers()) {
			if (miembro != null) {
				actualizar(miembro.getPlayer());
			}
		}
	}
}
