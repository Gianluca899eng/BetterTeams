package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.commands.SubCommand;
import com.booksaw.betterTeams.message.ReferencedFormatMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /clanadmin duelo agregar &lt;jugador&gt; - lo suma al duelo que pelea su clan.
 *
 * <p>Existe por un caso real: alguien se olvido de anotarse y su clan quedo peleando sin
 * el. Sin esto la unica salida seria esperar a la proxima duelo, porque los participantes
 * se congelan al arrancar — y eso esta bien, porque si no alcanzaria con bajarse cuando te
 * estan por matar.
 *
 * <p><b>No le cambia la preferencia para las proximas.</b> Esa la elige el jugador, no el
 * staff; el mensaje se lo recuerda.
 *
 * <p>🔑 <b>El nodo es {@code admin} a proposito, no {@code admin.duelo}.</b> Un nodo nuevo
 * no esta declarado en el {@code plugin.yml} y para Bukkit eso es OP-only —el mismo pozo en
 * el que cayo {@code /clan duelo}—. Con {@code admin} usa el permiso que ya pide todo
 * {@code /clanadmin}, que si esta declarado y se puede repartir por LuckPerms.
 */
public class DueloTeama extends SubCommand {

	private final DueloManager manager;

	public DueloTeama(DueloManager manager) {
		this.manager = manager;
	}

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		if (!args[0].equalsIgnoreCase("agregar")) {
			return new CommandResponse("invalidArg");
		}

		OfflinePlayer objetivo = buscar(args[1]);
		if (objetivo == null) {
			return new CommandResponse("noPlayer");
		}
		return respuesta(manager.agregarADuelo(objetivo));
	}

	/**
	 * El jugador, conectado o no.
	 *
	 * <p>Con {@code online-mode=false} un nombre nunca visto igual devuelve un
	 * OfflinePlayer, con un UUID inventado. Por eso se exige que haya entrado alguna vez:
	 * si no, se sumaria al duelo un fantasma.
	 */
	private OfflinePlayer buscar(String nombre) {
		Player conectado = Bukkit.getPlayerExact(nombre);
		if (conectado != null) {
			return conectado;
		}
		@SuppressWarnings("deprecation")
		OfflinePlayer offline = Bukkit.getOfflinePlayer(nombre);
		return offline.hasPlayedBefore() ? offline : null;
	}

	private CommandResponse respuesta(DueloManager.Resultado resultado) {
		if (resultado.argumentos.length == 0) {
			return resultado.exito
					? new CommandResponse(true, resultado.referencia)
					: new CommandResponse(resultado.referencia);
		}
		return new CommandResponse(resultado.exito,
				new ReferencedFormatMessage(resultado.referencia, resultado.argumentos));
	}

	@Override
	public String getCommand() {
		return "duelo";
	}

	@Override
	public String getNode() {
		return "admin";
	}

	@Override
	public String getHelp() {
		return "Suma a alguien al duelo que pelea su clan";
	}

	@Override
	public String getArguments() {
		return "agregar <jugador>";
	}

	@Override
	public int getMinimumArguments() {
		return 2;
	}

	@Override
	public int getMaximumArguments() {
		return 2;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			options.add("agregar");
		} else if (args.length == 2) {
			for (Player conectado : Bukkit.getOnlinePlayers()) {
				options.add(conectado.getName());
			}
		}
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
