package com.booksaw.betterTeams.luniatic.hitos;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.commands.SubCommand;
import com.booksaw.betterTeams.luniatic.Texto;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/**
 * /clanadmin hitos &lt;clan&gt; [set|sumar] &lt;cantidad&gt; - mira y corrige el contador.
 *
 * <p>Existe por dos casos que van a pasar: probar el desbloqueo sin minar cien mil bloques, y
 * reponerle a un clan lo que se pierda si alguna vez hay que restaurar un backup viejo del
 * {@code clan-hitos.yml}.
 *
 * <p><b>El nodo es {@code admin}, no {@code admin.hitos}.</b> Un nodo nuevo no esta declarado
 * en el {@code plugin.yml} y para Bukkit eso es OP-only, que es el pozo en el que ya cayo
 * {@code /clan duelo}. Con {@code admin} usa el permiso que ya pide todo {@code /clanadmin},
 * que si esta declarado y se reparte por LuckPerms.
 */
public class HitosTeama extends SubCommand {

	private static final String MARCA = "&#9235FF";
	private static final String CUERPO = "&#E4D9FF";
	private static final String ETIQUETA = "&#7162FF";
	private static final String ERROR = "&#FF4554";

	private final HitosManager manager;

	public HitosTeama(HitosManager manager) {
		this.manager = manager;
	}

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		Team clan = Team.getTeam(args[0]);
		if (clan == null) {
			return new CommandResponse("noTeam");
		}

		if (args.length == 1) {
			for (String linea : HitosCommand.construirDetalle(manager, clan)) {
				sender.sendMessage(linea);
			}
			// Diagnostico de la guarda anti-abuso. Es lo unico que ocupa memoria de todo
			// esto, y sin un numero a mano la unica forma de saber si el tope quedo corto
			// —o exageradamente largo— seria adivinar.
			sender.sendMessage(Texto.col(ETIQUETA + "Posiciones colocadas en memoria: "
					+ MARCA + HitosCommand.numero(manager.getColocados().getAnotados())));
			return new CommandResponse(true);
		}

		if (args.length != 3) {
			return new CommandResponse("invalidArg");
		}

		long cantidad;
		try {
			cantidad = Long.parseLong(args[2].trim());
		} catch (NumberFormatException e) {
			return new CommandResponse("invalidArg");
		}
		if (cantidad < 0) {
			return new CommandResponse("invalidArg");
		}

		String accion = args[1].toLowerCase(Locale.ROOT);
		if (accion.equals("set")) {
			// Fijar hacia abajo puede "des-desbloquear" un hito. Se permite a proposito: es
			// justo lo que hace falta para probar el estado bloqueado despues de haberlo
			// desbloqueado, y es la unica forma de deshacer un sumar equivocado.
			manager.setBloquesMinados(clan.getID(), cantidad);
		} else if (accion.equals("sumar")) {
			// Pasa por sumar y no por set para que el cruce de un umbral dispare el aviso al
			// clan igual que si lo hubieran minado. Probar el desbloqueo sin ver el aviso
			// dejaria sin probar justamente la mitad que se rompe.
			manager.sumar(clan.getID(), cantidad);
		} else {
			return new CommandResponse("invalidArg");
		}

		sender.sendMessage(Texto.col(ETIQUETA + "Clanes » " + CUERPO + "El contador de "
				+ MARCA + clan.getName() + CUERPO + " queda en "
				+ MARCA + HitosCommand.numero(manager.getBloquesMinados(clan.getID()))
				+ CUERPO + " bloques."));

		if (!manager.isHabilitado()) {
			sender.sendMessage(Texto.col(ERROR
					+ "Ojo: los hitos estan apagados en el config, asi que esto no gatea nada."));
		}
		return new CommandResponse(true);
	}

	@Override
	public String getCommand() {
		return "hitos";
	}

	@Override
	public String getNode() {
		return "admin";
	}

	@Override
	public String getHelp() {
		return "Mira o corrige los bloques minados de un clan";
	}

	@Override
	public String getArguments() {
		return "<clan> [set|sumar] [cantidad]";
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
		if (args.length == 2) {
			options.add("set");
			options.add("sumar");
		}
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
