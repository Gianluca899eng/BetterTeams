package com.booksaw.betterTeams.luniatic;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.ReferencedFormatMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/**
 * /clan icono &lt;material&gt; - elige con que se muestra el clan en el menu.
 *
 * <p>El camino normal es el menu, que es una grilla de opciones: elegir un item de una
 * lista es justo lo que un menu hace mejor que escribir. Este comando existe porque el
 * boton lo ejecuta —los botones disparan comandos que ya existen, asi que permisos y
 * rangos siguen valiendo en un solo lugar— y de paso queda para quien prefiera escribir.
 *
 * <p>🔑 <b>Reusa el nodo {@code name}</b>, que upstream declara con {@code default: true}.
 * Un nodo propio nace sin declarar en el {@code plugin.yml} y para Bukkit eso es OP-only:
 * el mismo pozo que dejo {@code /clan duelo} inservible. Y encaja: el icono es identidad
 * del clan, igual que el nombre.
 */
public class IconoCommand extends TeamSubCommand {

	private final ClanIconos iconos;

	public IconoCommand(ClanIconos iconos) {
		this.iconos = iconos;
	}

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {
		Material material = Material.matchMaterial(args[0].trim().toUpperCase(Locale.ROOT));
		if (material == null || !iconos.esElegible(material)) {
			return new CommandResponse("clanes.icono_invalido");
		}
		iconos.setIcono(team.getID(), material);
		return new CommandResponse(true,
				new ReferencedFormatMessage("clanes.icono_puesto", nombreLegible(material)));
	}

	/** {@code NETHERITE_SWORD} se lee mucho peor que "netherite sword". */
	static String nombreLegible(Material material) {
		return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	@Override
	public String getCommand() {
		return "icono";
	}

	@Override
	public String getNode() {
		return "name";
	}

	@Override
	public String getHelp() {
		return "Elige con que se muestra tu clan en el menu";
	}

	@Override
	public String getArguments() {
		return "<material>";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			for (Material material : Main.plugin.getClanIconos().getElegibles()) {
				options.add(material.name().toLowerCase(Locale.ROOT));
			}
		}
	}

	/** El icono habla por el clan entero, asi que lo elige quien manda. */
	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.ADMIN;
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
