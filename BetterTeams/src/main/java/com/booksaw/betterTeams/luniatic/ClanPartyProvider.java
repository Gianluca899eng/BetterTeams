package com.booksaw.betterTeams.luniatic;

import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.team.TeamManager;

import me.pikamug.unite.api.objects.PartyProvider;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Presenta cada clan como un "party" para Quests, que asi comparte el progreso de una mision
 * entre los miembros. Quests lo descubre por el ServicesManager y solo le llama getPartyId,
 * getMembers y getOnlineMembers.
 *
 * Se escribio propio en vez de usar el proveedor que trae Unite por dos motivos: aquel no
 * chequea null -- para un jugador sin clan hace Team.getTeam(p).getID() y tira NPE, y Quests
 * no la atrapa en share-progress-level 2 -- y su ultima version es de 2025, anterior a 26.x.
 *
 * El identificador de party es el UUID del clan, no su nombre: renombrar un clan no tiene que
 * perder el progreso en curso.
 */
public class ClanPartyProvider extends PartyProvider {

    private static final String NOMBRE = "BetterTeams";

    public ClanPartyProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Resuelve el clan por su id de party, tolerando un id invalido o un clan ya disuelto. */
    private Team porId(String partyId) {
        if (partyId == null) {
            return null;
        }
        try {
            return Team.getTeam(UUID.fromString(partyId));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Corre por cada evento de progreso de una mision compartida, y Quests lo llama dos veces
     * seguidas, asi que los dos atajos de aca importan:
     *
     * 1. El actor siempre esta conectado en este camino, asi que se prueba getPlayer antes de
     *    construir un OfflinePlayer nuevo.
     * 2. TeamManager.getTeam(OfflinePlayer) usa un lookup O(1), pero si el jugador no aparece
     *    ahi cae a un stream sobre TODOS los clanes cargados. Ese es justo el caso de quien no
     *    tiene clan, o sea el mas comun. El isInTeam corta antes de llegar al stream.
     */
    private Team porJugador(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        OfflinePlayer jugador = Bukkit.getPlayer(playerId);
        if (jugador == null) {
            jugador = Bukkit.getOfflinePlayer(playerId);
        }
        TeamManager manager = Team.getTeamManager();
        if (manager == null || !manager.isInTeam(jugador)) {
            return null;
        }
        return manager.getTeam(jugador);
    }

    @Override
    public boolean isPluginEnabled() {
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public String getPluginName() {
        return NOMBRE;
    }

    /**
     * No se crean clanes desde aca. Un clan se arma con /clan create, que cobra, valida el
     * nombre y avisa; crearlo por un camino lateral saltearia todo eso.
     */
    @Override
    public boolean createParty(String partyName, UUID playerId) {
        return false;
    }

    @Override
    public boolean isPlayerInParty(UUID playerId) {
        return porJugador(playerId) != null;
    }

    @Override
    public boolean areInSameParty(UUID playerId1, UUID playerId2) {
        Team uno = porJugador(playerId1);
        Team otro = porJugador(playerId2);
        return uno != null && otro != null && uno.getID().equals(otro.getID());
    }

    @Override
    public String getPartyName(UUID playerId) {
        Team clan = porJugador(playerId);
        return clan == null ? null : clan.getName();
    }

    /** Devuelve null para quien no tenga clan: es la señal que Quests espera para no compartir. */
    @Override
    public String getPartyId(UUID playerId) {
        Team clan = porJugador(playerId);
        return clan == null ? null : clan.getID().toString();
    }

    @Override
    public UUID getLeader(String partyId) {
        Team clan = porId(partyId);
        if (clan == null) {
            return null;
        }
        List<TeamPlayer> duenos = clan.getMembers().getRank(PlayerRank.OWNER);
        if (duenos == null || duenos.isEmpty()) {
            return null;
        }
        TeamPlayer dueno = duenos.get(0);
        return dueno == null || dueno.getPlayer() == null ? null : dueno.getPlayer().getUniqueId();
    }

    @Override
    public Set<UUID> getMembers(String partyId) {
        Team clan = porId(partyId);
        if (clan == null) {
            return Collections.emptySet();
        }
        Set<UUID> out = new HashSet<>();
        for (OfflinePlayer miembro : clan.getMembers().getOfflinePlayers()) {
            if (miembro != null) {
                out.add(miembro.getUniqueId());
            }
        }
        return out;
    }

    @Override
    public Set<UUID> getOnlineMembers(String partyId) {
        Team clan = porId(partyId);
        if (clan == null) {
            return Collections.emptySet();
        }
        Set<UUID> out = new HashSet<>();
        for (Player miembro : clan.getOnlineMembers()) {
            if (miembro != null) {
                out.add(miembro.getUniqueId());
            }
        }
        return out;
    }
}
