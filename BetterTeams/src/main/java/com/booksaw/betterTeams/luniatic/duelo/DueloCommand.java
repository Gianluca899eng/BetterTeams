package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.ReferencedFormatMessage;
import com.google.common.collect.ImmutableSet;
import org.bukkit.command.CommandSender;

import java.math.BigDecimal;
import java.util.List;

/**
 * /team duelo &lt;clan&gt; &lt;apuesta&gt; - desafia, o acepta si ese clan ya te desafio
 * /team duelo estado - muestra el duelo en curso
 * /team duelo rendirse - se rinde y le deja el pozo al rival
 */
public class DueloCommand extends TeamSubCommand {

	private final DueloManager manager;

	public DueloCommand(DueloManager manager) {
		this.manager = manager;
	}

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {

		if (args[0].equalsIgnoreCase("estado")) {
			Duelo duelo = manager.getDuelo(team);
			if (duelo == null) {
				return new CommandResponse(true, "duelo.sin_duelo");
			}
			Team rival = Team.getTeam(duelo.rivalDe(team.getID()));
			return new CommandResponse(true, new ReferencedFormatMessage("duelo.estado",
					rival == null ? "?" : rival.getName(),
					String.format("%.2f", duelo.getPozo()),
					String.valueOf(duelo.segundosRestantes(System.currentTimeMillis()) / 60),
					duelo.getBajas(team.getID()) + "/" + manager.getObjetivoBajas(),
					rival == null ? "?" : duelo.getBajas(rival.getID()) + "/" + manager.getObjetivoBajas()));
		}

		if (args[0].equalsIgnoreCase("rendirse")) {
			return respuesta(manager.rendirse(team));
		}

		if (args.length < 2) {
			return new CommandResponse("duelo.falta_monto");
		}

		Team rival = Team.getTeam(args[0]);
		if (rival == null) {
			return new CommandResponse("noTeam");
		}

		double apuesta;
		try {
			apuesta = new BigDecimal(args[1]).doubleValue();
		} catch (Exception e) {
			return new CommandResponse("duelo.monto_invalido");
		}
		if (apuesta < 0) {
			return new CommandResponse("duelo.monto_invalido");
		}

		return respuesta(manager.desafiar(team, rival, apuesta));
	}

	private CommandResponse respuesta(DueloManager.Resultado resultado) {
		if (resultado.argumentos.length == 0) {
			return resultado.exito
					? new CommandResponse(true, resultado.referencia)
					: new CommandResponse(resultado.referencia);
		}
		ReferencedFormatMessage mensaje = new ReferencedFormatMessage(resultado.referencia, resultado.argumentos);
		return new CommandResponse(resultado.exito, mensaje);
	}

	@Override
	public String getCommand() {
		return "duelo";
	}

	@Override
	public String getNode() {
		return "duelo";
	}

	@Override
	public String getHelp() {
		return "Pacta un duelo con otro clan, o acepta el que te enviaron";
	}

	@Override
	public String getArguments() {
		return "<clan> <apuesta> | estado | rendirse";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public int getMaximumArguments() {
		return 2;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			options.add("estado");
			options.add("rendirse");
			Team propio = getMyTeam(sender);
			addTeamStringList(options, args[0], propio != null ? ImmutableSet.of(propio.getID()) : null, null);
		} else if (args.length == 2) {
			options.add("<apuesta>");
		}
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.OWNER;
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
