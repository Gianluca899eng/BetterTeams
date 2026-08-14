package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.*;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.events.InventoryManagement;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

public class EchestCommand extends TeamSubCommand {

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {

		// Luniatic: el cofre es un hito, no viene con el clan.
		//
		// Va antes que todo lo demas, y en especial antes del adminViewers.put de mas abajo:
		// registrar al jugador ahi y despues negarle el cofre lo dejaria marcado como que
		// tiene un inventario abierto, y la guarda anti-duplicacion le negaria el cofre para
		// siempre, incluso despues de desbloquearlo.
		//
		// Gatear aca alcanza para las dos entradas: el boton del menu ejecuta este comando.
		if (Main.plugin.getHitosManager() != null && !Main.plugin.getHitosManager().tiene(team.getID(),
				com.booksaw.betterTeams.luniatic.hitos.Recompensa.COFRE)) {
			Main.plugin.getHitosManager().avisarBloqueado(player.getPlayer().getPlayer(), team.getID(),
					com.booksaw.betterTeams.luniatic.hitos.Recompensa.COFRE);
			return new CommandResponse(false);
		}

		if (InventoryManagement.adminViewers.containsKey(player.getPlayer().getPlayer())) {
			Main.plugin.getLogger().warning(player.getPlayer().getPlayer().getName() + " tried duping :C get em!");
			return new CommandResponse(false);
		}

		InventoryManagement.adminViewers.put(player.getPlayer().getPlayer(), team);
		if (team.getEchest() == null || team.getEchest().getSize() == 0) {
			Main.plugin.getLogger().warning("EnderChest was found to be null or empty " + team.getEchest()
					+ " this should never occur, report to booksaw");
		}

		Main.plugin.getFoliaLib().getScheduler().runAtEntity(player.getPlayer().getPlayer(), task -> Objects.requireNonNull(player.getPlayer().getPlayer()).openInventory(team.getEchest()));

		return new CommandResponse(true);
	}

	@Override
	public String getCommand() {
		return "echest";
	}

	@Override
	public String getNode() {
		return "echest";
	}

	@Override
	public String getHelp() {
		return "View your teams ender chest";
	}

	@Override
	public String getArguments() {
		return "";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 0;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.DEFAULT;
	}

}
