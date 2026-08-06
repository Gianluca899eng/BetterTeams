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
 * /team duelo &lt;clan&gt; &lt;apuesta&gt; [duracion] - desafia a otro clan
 * /team duelo aceptar &lt;clan&gt; - acepta el desafio que te mandaron, con sus condiciones
 * /team duelo rechazar &lt;clan&gt; - lo rechaza
 * /team duelo estado - muestra el duelo en curso
 * /team duelo rendirse - se rinde y le deja el pozo al rival
 *
 * <p>Repetir el comando de desafio con el mismo monto tambien acepta, y se deja andando
 * por si alguien lo tiene en la memoria. Pero la via buena es {@code aceptar}: el desafio
 * ya dice cuanto y cuanto dura, asi que hacerlo escribir de nuevo es friccion.
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
					duelo.getBajas(team.getID()) + "/" + duelo.getObjetivoBajas(),
					rival == null ? "?" : duelo.getBajas(rival.getID()) + "/" + duelo.getObjetivoBajas()));
		}

		if (args[0].equalsIgnoreCase("rendirse")) {
			return respuesta(manager.rendirse(team));
		}

		// Aceptar no repite el monto ni la duracion: el desafio ya los dice, y hacerlos
		// escribir de nuevo es friccion. El consentimiento es el acto de aceptar.
		if (args[0].equalsIgnoreCase("aceptar")) {
			if (args.length < 2) {
				return new CommandResponse("duelo.falta_clan");
			}
			Team quienDesafio = Team.getTeam(args[1]);
			if (quienDesafio == null) {
				return new CommandResponse("noTeam");
			}
			return respuesta(manager.aceptar(team, quienDesafio));
		}

		if (args[0].equalsIgnoreCase("rechazar")) {
			if (args.length < 2) {
				return new CommandResponse("duelo.falta_clan");
			}
			Team quienDesafio = Team.getTeam(args[1]);
			if (quienDesafio == null) {
				return new CommandResponse("noTeam");
			}
			return respuesta(manager.rechazar(team, quienDesafio));
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
			apuesta = new BigDecimal(normalizarMonto(args[1])).doubleValue();
		} catch (Exception e) {
			return new CommandResponse("duelo.monto_invalido");
		}
		if (apuesta < 0) {
			return new CommandResponse("duelo.monto_invalido");
		}

		// Se pasa null cuando no la eligio, y NO el preset por defecto: si esto termina
		// siendo una aceptacion, el manager tiene que poder tomar la que pacto el otro.
		// Con el default puesto aca, aceptar un duelo de dias era imposible.
		DueloManager.Duracion duracion = null;
		if (args.length >= 3) {
			duracion = manager.duracionPorId(args[2]);
			if (duracion == null) {
				return new CommandResponse(false,
						new ReferencedFormatMessage("duelo.duracion_invalida", idsDeDuracion()));
			}
		}

		return respuesta(manager.desafiar(team, rival, apuesta, duracion));
	}

	/** Los ids pactables, para el mensaje de error y el tab complete. */
	private String idsDeDuracion() {
		StringBuilder sb = new StringBuilder();
		for (DueloManager.Duracion d : manager.getDuraciones()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(d.id);
		}
		return sb.toString();
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

	/**
	 * Acepta el monto escrito como se escribe aca, con sufijo de escala si lo tiene.
	 *
	 * <p>La logica vive en {@link com.booksaw.betterTeams.luniatic.Montos} porque los
	 * comandos del banco toman montos escritos igual. Esto queda como delegacion para
	 * no cambiar el punto de entrada que ya usaban las pruebas.
	 */
	static String normalizarMonto(String texto) {
		return com.booksaw.betterTeams.luniatic.Montos.normalizar(texto);
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
		return "<clan> <apuesta> [duracion] | aceptar <clan> | estado | rendirse | rechazar <clan>";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public int getMaximumArguments() {
		return 3;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			options.add("aceptar");
			options.add("estado");
			options.add("rendirse");
			Team propio = getMyTeam(sender);
			addTeamStringList(options, args[0], propio != null ? ImmutableSet.of(propio.getID()) : null, null);
		} else if (args.length == 2) {
			// Con aceptar/rechazar el segundo argumento es un clan, no un monto.
			if (args[0].equalsIgnoreCase("aceptar") || args[0].equalsIgnoreCase("rechazar")) {
				Team propio = getMyTeam(sender);
				addTeamStringList(options, args[1], propio != null ? ImmutableSet.of(propio.getID()) : null, null);
			} else {
				options.add("<apuesta>");
			}
		} else if (args.length == 3) {
			for (DueloManager.Duracion duracion : manager.getDuraciones()) {
				options.add(duracion.id);
			}
		}
	}

	/**
	 * Lider y colider. En BetterTeams eso es OWNER y ADMIN: pedir ADMIN deja pasar a
	 * los dos, porque los rangos son escalonados. Un miembro comun no pacta duelos.
	 */
	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.ADMIN;
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
