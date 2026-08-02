package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /team menu [texto] - abre el menu de clanes, opcionalmente filtrado por nombre.
 *
 * <p>La busqueda va como argumento y no como menu de escritura a proposito: los
 * menus tipo yunque son de lo que peor funciona por Geyser, y Bedrock es una parte
 * real del publico.
 */
public class MenuCommand extends SubCommand {

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return new CommandResponse("menu.solo_jugadores");
		}
		MenuClanes.abrirLista((Player) sender, 0, args.length > 0 ? args[0] : null);
		return new CommandResponse(true);
	}

	@Override
	public String getCommand() {
		return "menu";
	}

	@Override
	public String getNode() {
		return "list";
	}

	@Override
	public String getHelp() {
		return "Abre el menu de clanes para mirarlos y unirte";
	}

	@Override
	public String getArguments() {
		return "[texto]";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			options.add("[texto]");
		}
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
