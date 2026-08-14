package me.pikamug.unite.api.objects;

import me.pikamug.unite.api.interfaces.PartyPlugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Redeclaracion minima de la API de Unite (LGPL-2.1, PikaMug/Unite). Ver PartyPlugin.
 *
 * Quests busca este tipo exacto en el ServicesManager y le llama getPartyId, getMembers y
 * getOnlineMembers por invokevirtual, asi que tiene que ser una clase abstracta con este
 * nombre de paquete. El resto de los metodos existen para respetar la forma de la API.
 */
public abstract class PartyProvider implements PartyPlugin {

    protected Plugin plugin = null;

    public boolean createParty(Player player) {
        return createParty(String.valueOf(System.currentTimeMillis()), player.getUniqueId());
    }

    public boolean isPlayerInParty(Player player) {
        return isPlayerInParty(player.getUniqueId());
    }

    public boolean areInSameParty(Player player1, Player player2) {
        return areInSameParty(player1.getUniqueId(), player2.getUniqueId());
    }
}
