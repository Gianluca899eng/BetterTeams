package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class TeamPlayerSetComponent extends SetTeamComponent<TeamPlayer> {

	/**
	 * @return A list of players which are currently online and on this team
	 */
	/**
	 * Resuelve por UUID contra los conectados.
	 *
	 * <p>La version anterior clonaba el set y pasaba por
	 * {@code TeamPlayer#getPlayer()}, que llama a {@code Bukkit.getOfflinePlayer()}
	 * <b>por cada miembro</b> — una llamada cara para despues preguntar solo si esta
	 * conectado. {@code Bukkit.getPlayer(UUID)} es una busqueda directa y devuelve
	 * null si no esta.
	 */
	public List<Player> getOnlinePlayers() {
		List<Player> conectados = new ArrayList<>();
		for (TeamPlayer teamPlayer : set) {
			Player jugador = Bukkit.getPlayer(teamPlayer.getPlayerUUID());
			if (jugador != null) {
				conectados.add(jugador);
			}
		}
		return conectados;
	}

	/**
	 * @return A list of the members of this team which are currently OFFLINE
	 * <p>
	 * The name says offline and so does the filter; the doc used to say "online", and code that
	 * trusted it went looking for connected players in a list that excludes them by construction.
	 */
	public List<OfflinePlayer> getOfflinePlayers() {
		return getClone().stream()
				.map(TeamPlayer::getPlayer)
				.filter(p -> !p.isOnline())
				.collect(Collectors.toList());
	}

	/**
	 * @return A list of all teamPlayers which are currently online
	 */
	public List<TeamPlayer> getOnlineTeamPlayers() {
		return getClone().stream()
				.filter(TeamPlayer::isOnline)
				.collect(Collectors.toList());
	}

	/**
	 * Used to get the team player instance of that offline player
	 *
	 * @param p The player to get the team player for
	 * @return The team player instance, or null if not found
	 */
	/**
	 * 🔑 <b>El camino mas caliente del plugin.</b> Lo llama {@code contains()}, que a
	 * su vez lo llama {@code getTeamUUID(OfflinePlayer)} <b>por cada clan</b> cada
	 * vez que se resuelve el clan de un jugador — y eso pasa dos veces por cada
	 * evento de dano.
	 *
	 * <p>La version anterior clonaba el set y comparaba
	 * {@code teamPlayer.getPlayer().getUniqueId()}, o sea que hacia un
	 * {@code Bukkit.getOfflinePlayer()} <b>por miembro de cada clan</b> para sacar un
	 * UUID que el propio {@link TeamPlayer} ya tiene guardado al lado. Con 50 clanes
	 * de 10 miembros eran 500 de esas llamadas por lookup.
	 *
	 * <p>Iterar el set directo no es menos seguro que clonarlo: clonar tambien lo
	 * itera, asi que la exposicion a una modificacion concurrente es la misma. Lo
	 * unico que cambia es que ya no aloca un set por clan.
	 */
	@Nullable
	public TeamPlayer getTeamPlayer(@NotNull OfflinePlayer p) {
		UUID buscado = p.getUniqueId();
		for (TeamPlayer teamPlayer : set) {
			if (buscado.equals(teamPlayer.getPlayerUUID())) {
				return teamPlayer;
			}
		}
		return null;
	}

	/**
	 * Get all team players with the specified rank
	 *
	 * @param rank The rank to filter by
	 * @return List of team players with the specified rank
	 */
	public List<TeamPlayer> getRank(PlayerRank rank) {
		return getClone().stream()
				.filter(player -> player.getRank() == rank)
				.collect(Collectors.toList());
	}

	/**
	 * Sends the specified message to all team players stored in the list
	 *
	 * @param message The message to send to all online players
	 */
	public void broadcastMessage(@NotNull Message message) {
		message.sendMessage(getOnlinePlayers());
	}

	/**
	 * Sends the specified title to all team players stored in the list
	 *
	 * @param message The message to send to all online players
	 */
	public void broadcastTitle(@NotNull Message message) {
		message.sendTitle(getOnlinePlayers());
	}

	@Override
	public TeamPlayer fromString(String str) {
		return new TeamPlayer(str);
	}

	@Override
	public String toString(@NotNull TeamPlayer component) {
		return component.toString();
	}

	@Override
	public boolean contains(@NotNull TeamPlayer component) {
		return contains(component.getPlayer());
	}

	/**
	 * Checks if the given player is in this team
	 *
	 * @param player The player to check for
	 * @return true if the player is in this team, false otherwise
	 */
	public boolean contains(OfflinePlayer player) {
		return getTeamPlayer(player) != null;
	}

	private String getPlayersString(@NotNull List<? extends OfflinePlayer> players) {
		return players.stream().map(p -> p.getName()).collect(Collectors.joining(", "));
	}

	/**
	 * @return A comma-separated string of all online player names
	 */
	public String getOnlinePlayersString() {
		return getPlayersString(getOnlinePlayers());
	}

	/**
	 * @return A comma-separated string of all offline player names
	 */
	public String getOfflinePlayersString() {
		return getPlayersString(getOfflinePlayers());
	}
}