package me.pikamug.unite.api.interfaces;

import java.util.Set;
import java.util.UUID;

/**
 * Redeclaracion minima de la API de Unite (LGPL-2.1, PikaMug/Unite), necesaria solo para
 * compatibilidad binaria: Quests resuelve PartyProvider por este nombre exacto de paquete.
 *
 * Son declaraciones, no codigo de Unite. No se copio ninguna implementacion suya, y este
 * plugin no depende del plugin Unite.
 *
 * NO INSTALAR Unite junto con este plugin: quedarian dos copias de estas clases y la que
 * cargue segunda falla con LinkageError.
 */
public interface PartyPlugin {

    boolean isPluginEnabled();

    String getPluginName();

    boolean createParty(String partyName, UUID playerId);

    boolean isPlayerInParty(UUID playerId);

    boolean areInSameParty(UUID playerId1, UUID playerId2);

    String getPartyName(UUID playerId);

    String getPartyId(UUID playerId);

    UUID getLeader(String partyId);

    Set<UUID> getMembers(String partyId);

    Set<UUID> getOnlineMembers(String partyId);
}
