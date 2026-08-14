package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.ReferencedFormatMessage;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * /clan preferencias - muestra como estas anotado
 * /clan preferencias duelo si|no - si entras a los duelos de tu clan
 * /clan preferencias aliados si|no - si tu clan entra a los duelos de sus aliados
 *
 * <p>🔑 <b>Es un comando aparte de {@code /clan duelo} por el rango.</b> Pactar un duelo es
 * cosa de lider y colider, pero <b>elegir si peleas es de cada uno</b>: si esto viviera
 * adentro de {@code /clan duelo}, un miembro comun no podria ni consultarlo.
 *
 * <p>🔑 <b>Y reusa el nodo de permiso {@code duelo}</b> en vez de estrenar uno. Un
 * subcomando propio nace con un permiso que no esta declarado en el {@code plugin.yml}, y
 * para Bukkit eso es OP-only: exactamente lo que dejo {@code /clan duelo} inservible para
 * todo el mundo hasta que se descubrio. Con el nodo ya repartido, esto anda desde el
 * primer arranque.
 */
public class PreferenciasCommand extends TeamSubCommand {

	private final DueloManager manager;

	public PreferenciasCommand(DueloManager manager) {
		this.manager = manager;
	}

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {
		DueloPreferencias preferencias = manager.getPreferencias();

		if (args.length == 0) {
			return new CommandResponse(true, new ReferencedFormatMessage("duelo.preferencias_estado",
					texto(preferencias.participa(player.getPlayerUUID())),
					texto(preferencias.ayudaAliados(team.getID()))));
		}

		if (args[0].equalsIgnoreCase("duelo")) {
			if (args.length < 2) {
				return new CommandResponse("duelo.preferencias_falta_valor");
			}
			Boolean valor = leerSiNo(args[1]);
			if (valor == null) {
				return new CommandResponse("duelo.preferencias_falta_valor");
			}
			// El cambio NO toca un duelo ya empezada: los participantes se congelan al
			// arrancar. Si no, alcanzaria con bajarse cuando te estan por matar.
			preferencias.setParticipa(player.getPlayerUUID(), valor);
			boolean enDuelo = manager.getDuelo(team) != null;
			return new CommandResponse(true, new ReferencedFormatMessage(
					enDuelo ? "duelo.preferencias_duelo_en_curso" : "duelo.preferencias_duelo",
					texto(valor)));
		}

		if (args[0].equalsIgnoreCase("aliados")) {
			// Esto habla por el clan entero, asi que lo decide quien manda. El comando
			// sigue siendo de rango DEFAULT para que un miembro pueda consultarlo.
			if (player.getRank().value < PlayerRank.ADMIN.value) {
				return new CommandResponse("duelo.preferencias_solo_mando");
			}
			if (args.length < 2) {
				return new CommandResponse("duelo.preferencias_falta_valor");
			}
			Boolean valor = leerSiNo(args[1]);
			if (valor == null) {
				return new CommandResponse("duelo.preferencias_falta_valor");
			}
			preferencias.setAyudaAliados(team.getID(), valor);
			return new CommandResponse(true,
					new ReferencedFormatMessage("duelo.preferencias_aliados", texto(valor)));
		}

		return new CommandResponse("duelo.preferencias_falta_valor");
	}

	/** Acepta las dos formas que la gente escribe, y nada mas. */
	private Boolean leerSiNo(String texto) {
		if (texto.equalsIgnoreCase("si") || texto.equalsIgnoreCase("sí") || texto.equalsIgnoreCase("true")) {
			return Boolean.TRUE;
		}
		if (texto.equalsIgnoreCase("no") || texto.equalsIgnoreCase("false")) {
			return Boolean.FALSE;
		}
		return null;
	}

	private String texto(boolean valor) {
		return valor ? "si" : "no";
	}

	@Override
	public String getCommand() {
		return "preferencias";
	}

	@Override
	public String getNode() {
		return "duelo";
	}

	@Override
	public String getHelp() {
		return "Elige si entras a los duelos de tu clan";
	}

	@Override
	public String getArguments() {
		return "[duelo si|no] [aliados si|no]";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 2;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			options.add("duelo");
			options.add("aliados");
		} else if (args.length == 2) {
			options.add("si");
			options.add("no");
		}
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.DEFAULT;
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
