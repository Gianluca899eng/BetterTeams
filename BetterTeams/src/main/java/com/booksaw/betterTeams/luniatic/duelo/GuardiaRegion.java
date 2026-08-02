package com.booksaw.betterTeams.luniatic.duelo;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

/**
 * Consulta el flag PVP de WorldGuard.
 *
 * <p>Esta clase toca clases de WorldGuard, asi que solo se instancia si el plugin
 * esta cargado. Existe para que el duelo no atraviese una zona segura: sin esto,
 * destapar un dano cancelado tambien destaparia el del spawn.
 */
public class GuardiaRegion {

	public boolean permitePvp(Location ubicacion) {
		try {
			RegionQuery consulta = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
			return consulta.testState(BukkitAdapter.adapt(ubicacion), null, Flags.PVP);
		} catch (Throwable t) {
			// Ante la duda no se destapa nada.
			return false;
		}
	}
}
