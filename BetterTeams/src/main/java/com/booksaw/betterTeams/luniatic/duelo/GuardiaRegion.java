package com.booksaw.betterTeams.luniatic.duelo;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

/**
 * Decide si el duelo puede pelearse en un lugar.
 *
 * <p>Distingue dos cosas que WorldGuard trata igual:
 * <ul>
 * <li><b>Una region del staff</b> —el spawn, una zona de evento— que prohibe PvP.
 * Eso el duelo NUNCA lo pisa: son las reglas del servidor.
 * <li><b>Un claim de un jugador</b>, que trae {@code pvp deny} de fabrica. Eso el
 * duelo si lo pisa, si esta configurado asi: si no, media pelea termina cuando
 * alguien se mete en cualquier proteccion.
 * </ul>
 *
 * <p>Los claims se reconocen por el prefijo {@code ps} del id de la region, que es
 * el mismo criterio que usa ProtectionStones. ⚠️ Una region del staff llamada
 * {@code psAlgo} se confundiria con un claim: no llamar asi a las regiones propias.
 *
 * <p>Esta clase toca clases de WorldGuard, asi que solo se instancia si el plugin
 * esta cargado.
 */
public class GuardiaRegion {

	private final boolean pisaClaims;

	public GuardiaRegion(boolean pisaClaims) {
		this.pisaClaims = pisaClaims;
	}

	/**
	 * True si el duelo puede pelearse en esa ubicacion.
	 *
	 * <p>Ante cualquier error devuelve false. Es la unica direccion segura: un fallo
	 * apaga el override, no abre el spawn.
	 */
	public boolean permitePvp(Location ubicacion) {
		try {
			if (ubicacion == null || ubicacion.getWorld() == null) {
				return false;
			}
			RegionManager gestor = WorldGuard.getInstance().getPlatform().getRegionContainer()
					.get(BukkitAdapter.adapt(ubicacion.getWorld()));
			if (gestor == null) {
				return true;
			}
			ApplicableRegionSet regiones = gestor.getApplicableRegions(BlockVector3.at(
					ubicacion.getBlockX(), ubicacion.getBlockY(), ubicacion.getBlockZ()));

			for (ProtectedRegion region : regiones) {
				if (region.getFlag(Flags.PVP) != StateFlag.State.DENY) {
					continue;
				}
				// Un claim de jugador se pisa (si esta configurado); una region del
				// staff, nunca.
				if (!pisaClaims || !esClaim(region)) {
					return false;
				}
			}
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/** Mismo criterio que ProtectionStones: sus regiones se llaman psX. */
	private boolean esClaim(ProtectedRegion region) {
		String id = region.getId();
		return id != null && id.startsWith("ps");
	}
}
