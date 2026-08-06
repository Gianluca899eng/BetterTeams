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
 * <li>{@code %luniaticclanes_pvp_estado%} - la linea de PvP del scoreboard, ya armada:
 * {@code ᴏɴ}, {@code ᴏꜰꜰ}, o {@code ᴏꜰꜰ} amarillo si estas en guerra y aca te pueden
 * pegar. <b>Es el unico que responde siempre</b>, haya duelos o no.
 * <li>{@code %luniaticclanes_guerra%} - el clan rival, o vacio.
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
		if (jugador == null) {
			return "";
		}

		// 🔑 pvp_estado va ANTES del corte por "no hay duelos": es la linea normal de PvP
		// del scoreboard, no un dato del duelo. Con el corte adelante, la linea quedaba
		// vacia apenas terminaba una guerra —y para todo el servidor, que nunca tuvo
		// una— asi que el PvP desaparecia del cartel por completo.
		//
		// No cuesta lo que costaba el resto: no resuelve el clan. EstadoPvp es una
		// llamada reflexiva a PvPManager, y pvpForzadoAca corta en un booleano cuando
		// no hay duelos en curso.
		if ("pvp_estado".equalsIgnoreCase(parametro)) {
			// Semaforo, y en ese orden: verde peleas, rojo no, amarillo "no, pero tu
			// rival si". El verde es el mismo #56FF3B que ya usa la linea de dinero de
			// ese scoreboard, para no inventar un color: la paleta de Fotz no tiene uno
			// de exito, y el amarillo es el que ya usaba esta misma linea.
			//
			// El asterisco del estado de guerra no es decoracion: el color solo es la
			// senial mas debil —es lo que hizo fracasar la paleta original— y ademas no
			// se puede confiar en como lo pinta Bedrock. Con el asterisco, "off con
			// condicion" se distingue de "off" aunque el color no llegue.
			if (EstadoPvp.tienePvp(jugador.getPlayer())) {
				return Texto.col("&#56FF3Bᴏɴ");
			}
			return Texto.col(manager.isHabilitado() && manager.pvpForzadoAca(jugador.getPlayer())
					? "&eᴏꜰꜰ*" : "&#FF4554ᴏꜰꜰ");
		}

		// El resto si son datos del duelo. TAB los pide cada pocos segundos por jugador
		// conectado y resolver el clan recorre todos los clanes: sin duelos en curso se
		// corta antes de tocarlo.
		if (!manager.isHabilitado() || !manager.hayDuelos()) {
			return "";
		}
		Team clan = Team.getTeam(jugador);
		Duelo duelo = manager.getDuelo(clan);

		switch (parametro.toLowerCase()) {
			case "pvp_forzado":
				// Para la condicion de TAB: cuando esto es "si", la linea de PvP del
				// scoreboard muestra el estado forzado en vez del toggle personal.
				// Mira tambien DONDE esta parado: adentro de un claim el duelo no
				// aplica, y decir "forzado" ahi seria mentir sobre si te pueden matar.
				return manager.pvpForzadoAca(jugador.getPlayer()) ? "si" : "no";
			case "aviso_pvp":
				// Solo cuando el aviso significa algo: hay duelo y el override manda.
				if (duelo == null || !manager.isPisaPvpIndividual()) {
					return "";
				}
				return Texto.col(manager.getAvisoTab());
			case "guerra":
				// La linea "Guerra con:", vacia si no hay duelo. TAB la usa con una
				// display-condition, asi que quien no esta en guerra no ve el renglon.
				if (duelo == null) {
					return "";
				}
				Team otro = Team.getTeam(duelo.rivalDe(clan.getID()));
				return Texto.limpiar(otro == null ? "?" : otro.getName());
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
