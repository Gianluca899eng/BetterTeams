package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.luniatic.Texto;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholders del duelo, para que TAB pueda avisarlo.
 *
 * <p>El aviso importa por una razon concreta: cuando el override esta encendido, el
 * {@code /pvp off} de un jugador deja de protegerlo mientras su clan este en duelo.
 * Eso no puede ser una sorpresa, y el unico lugar que se mira sin abrir nada es la
 * lista del tab.
 *
 * <ul>
 * <li>{@code %luniaticclanes_aviso_pvp%} - el aviso, o vacio si no corresponde.
 * <li>{@code %luniaticclanes_duelo%} - el marcador corto, o vacio.
 * <li>{@code %luniaticclanes_en_duelo%} - si o no.
 * </ul>
 */
public class DueloPlaceholders extends PlaceholderExpansion {

	private final DueloManager manager;

	public DueloPlaceholders(DueloManager manager) {
		this.manager = manager;
	}

	@Override
	public @NotNull String getIdentifier() {
		return "luniaticclanes";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Luniatic";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.0";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer jugador, @NotNull String parametro) {
		// TAB pide esto cada pocos segundos por jugador conectado, y resolver el clan
		// recorre todos los clanes: sin duelos en curso se corta antes de tocarlo.
		if (jugador == null || !manager.isHabilitado() || !manager.hayDuelos()) {
			return "";
		}
		Team clan = Team.getTeam(jugador);
		Duelo duelo = manager.getDuelo(clan);

		switch (parametro.toLowerCase()) {
			case "aviso_pvp":
				// Solo cuando el aviso significa algo: hay duelo y el override manda.
				if (duelo == null || !manager.isPisaPvpIndividual()) {
					return "";
				}
				return Texto.col(manager.getAvisoTab());
			case "en_duelo":
				return duelo == null ? "no" : "si";
			case "duelo":
				if (duelo == null) {
					return "";
				}
				Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
				return Texto.limpiar(rival == null ? "?" : rival.getName())
						+ " " + duelo.getBajas(clan.getID())
						+ "-" + (rival == null ? 0 : duelo.getBajas(rival.getID()));
			default:
				return null;
		}
	}
}
