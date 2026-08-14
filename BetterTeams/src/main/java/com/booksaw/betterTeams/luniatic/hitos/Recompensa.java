package com.booksaw.betterTeams.luniatic.hitos;

/**
 * Lo que un hito habilita cuando el clan lo alcanza.
 *
 * <p>El conjunto es cerrado a proposito. El umbral, el nombre, el icono y el orden de un
 * hito se cambian solo tocando el config; la recompensa no, porque cada valor necesita
 * codigo que la haga valer en el lugar correcto. Inventar un valor nuevo en el config sin
 * ese enganche daria un hito que se desbloquea y no hace nada, que es peor que no tenerlo:
 * el jugador cumple el objetivo y no recibe lo prometido.
 *
 * <p>Por eso {@code HitosManager} descarta con un aviso en consola cualquier hito cuya
 * recompensa no este en esta lista, en vez de cargarlo igual.
 */
public enum Recompensa {

	/**
	 * Habilita el cofre compartido del clan.
	 *
	 * <p>Lo hace valer {@code EchestCommand}, que es el unico camino a la GUI del cofre:
	 * el boton del menu termina ejecutando ese mismo comando, asi que gatear el comando
	 * gatea las dos entradas de una sola vez.
	 */
	COFRE,

	/**
	 * Suma lugares de warp por encima del limite que da el nivel del clan.
	 *
	 * <p>Lo hace valer {@code Team.getMaxWarps()}, que es el unico punto por donde pasan
	 * tanto {@code SetWarpCommand} como la ficha del clan.
	 */
	WARPS,

	/**
	 * Suma lugares de miembro por encima del limite que da el nivel del clan.
	 *
	 * <p>Lo hace valer {@code Team.getTeamLimit()}.
	 */
	LUGARES
}
