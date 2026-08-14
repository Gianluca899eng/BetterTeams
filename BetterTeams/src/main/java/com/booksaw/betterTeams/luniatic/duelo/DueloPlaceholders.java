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
 * {@code ᴏɴ}, {@code ᴏꜰꜰ}, o {@code ᴏꜰꜰ} amarillo si estas en duelo y aca te pueden
 * pegar. <b>Responde siempre</b>, haya duelos o no.
 * <li>{@code %luniaticclanes_etiqueta%} - la etiqueta del clan sin corchetes, con su color,
 * para el nombre flotante. <b>Responde siempre</b> tambien.
 * <li>{@code %luniaticclanes_duelo%} - el clan rival, o vacio.
 * <li>{@code %luniaticclanes_aviso_pvp%} - el aviso, o vacio si no corresponde.
 * <li>{@code %luniaticclanes_duelo%} - el marcador corto, o vacio.
 * <li>{@code %luniaticclanes_en_duelo%} - si o no.
 * </ul>
 */
public class DueloPlaceholders extends PlaceholderExpansion {

	// Constantes ya coloreadas: TAB las pide cada pocos segundos por jugador conectado, y
	// pasarlas por MiniMessage en cada pedido era rearmar siempre el mismo texto.
	private static final String PVP_ON = Texto.col("&#56FF3Bᴏɴ");
	private static final String PVP_OFF = Texto.col("&#FF4554ᴏꜰꜰ");
	private static final String PVP_OFF_CONDICION = Texto.col("&eᴏꜰꜰ*");
	private static final String SEPARADOR = Texto.col("&#7162FF| ");

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
		// vacia apenas terminaba un duelo —y para todo el servidor, que nunca tuvo
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
			// El asterisco del estado de duelo no es decoracion: el color solo es la
			// senial mas debil —es lo que hizo fracasar la paleta original— y ademas no
			// se puede confiar en como lo pinta Bedrock. Con el asterisco, "off con
			// condicion" se distingue de "off" aunque el color no llegue.
			if (EstadoPvp.tienePvp(jugador.getPlayer())) {
				return PVP_ON;
			}
			return manager.isHabilitado() && manager.pvpForzadoAca(jugador.getPlayer())
					? PVP_OFF_CONDICION : PVP_OFF;
		}

		// La etiqueta del clan SIN CORCHETES, con su color y con su separador adelante. Se
		// diferencia de %betterTeams_tag% en los corchetes: ese los trae porque esta pensado
		// para el chat, donde hay texto al lado y hacen falta.
		//
		// 🔑 <b>El separador viaja adentro y no en el formato de TAB.</b> Puesto alla, el
		// jugador sin clan se queda con la barra colgando —"❤ 20 |"—, porque el formato es
		// uno solo para todos. Adentro, o salen los dos o no sale ninguno.
		//
		// Va antes del corte por duelos: se dibuja sobre la cabeza de todos, haya duelos o no.
		if ("etiqueta".equalsIgnoreCase(parametro)) {
			Team suClan = Team.getTeam(jugador);
			if (suClan == null) {
				return "";
			}
			String etiqueta = suClan.getOriginalTag();
			if (etiqueta.isEmpty()) {
				return "";
			}
			// La barra en el violeta de estructura, la etiqueta en el color de su clan.
			return SEPARADOR
					+ (suClan.getColor() == null ? "" : suClan.getColor().toString()) + etiqueta;
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
			case "rival":
				// El nombre del clan rival, vacio si no hay duelo. TAB lo usa con una
				// display-condition, asi que quien no esta en duelo no ve el renglon.
				// Se llamaba "guerra" hasta que el termino se estandarizo en duelo; no
				// puede llamarse "duelo" porque esa clave ya es el marcador corto.
				if (duelo == null) {
					return "";
				}
				Team otro = Team.getTeam(duelo.rivalDe(clan.getID()));
				return Texto.limpiar(otro == null ? "?" : otro.getName());
			case "en_duelo":
				return duelo == null ? "no" : "si";
			case "base_rival":
				// Lo lee la regla de wFly para cortar el vuelo: "si" cuando estas adentro
				// de una base rival, o cuando un rival esta adentro de una tuya.
				return manager.enZonaDeBaseDelDuelo(jugador.getPlayer()) ? "si" : "no";
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
