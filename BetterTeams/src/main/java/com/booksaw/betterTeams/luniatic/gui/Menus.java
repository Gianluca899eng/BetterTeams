package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.luniatic.Texto;
import com.booksaw.betterTeams.luniatic.duelo.Desafio;
import com.booksaw.betterTeams.luniatic.duelo.Duelo;
import com.booksaw.betterTeams.luniatic.duelo.DueloManager;
import com.booksaw.betterTeams.luniatic.hitos.Hito;
import com.booksaw.betterTeams.luniatic.hitos.HitosCommand;
import com.booksaw.betterTeams.luniatic.hitos.HitosManager;
import com.booksaw.betterTeams.luniatic.hitos.Recompensa;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Menu de clanes.
 *
 * <p>Sigue el mismo lenguaje visual que el menu de misiones de LuniaticPerf: marco
 * de cristal gris, interior mas claro, y navegacion fija abajo.
 *
 * <p><b>Los textos van escritos aca y no en los archivos de idioma</b>, igual que
 * en el menu de misiones. Es un menu propio de Luniatic, no de upstream: meterlo
 * en messages.yml agregaria ~60 claves a dos archivos que hay que mantener
 * sincronizados con cada version nueva, sin ganar nada.
 *
 * <p>🔑 <b>El color hay que convertirlo a mano.</b> Los mensajes del plugin viajan
 * como texto crudo con codigos {@code &#RRGGBB} y etiquetas MiniMessage, y el
 * inventario los muestra tal cual: hay que pasarlos por
 * {@code toAdventure} y despues serializarlos a legacy. Sin eso el menu se ve
 * con los codigos escritos, que fue exactamente el primer intento.
 */
public final class Menus {

	private static final int TAM = 54;
	private static final int SLOT_ANTERIOR = 45;
	private static final int SLOT_VOLVER = 48;
	private static final int SLOT_CERRAR = 49;
	private static final int SLOT_SIGUIENTE = 53;
	/** Nota informativa de la pantalla, pegada a la izquierda de Volver. */
	private static final int SLOT_NOTA = 47;

	/** Interior del cofre: cuatro filas de siete. */
	private static final int[] CONTENIDO = {
			10, 11, 12, 13, 14, 15, 16,
			19, 20, 21, 22, 23, 24, 25,
			28, 29, 30, 31, 32, 33, 34,
			37, 38, 39, 40, 41, 42, 43
	};

	// Rejilla de las pantallas fijas. Los botones caen siempre en las mismas
	// columnas: una fila con 6 items y otra con 5 corridos se ve desprolijo.
	/** Centro de la primera fila, para el titulo de la pantalla. */
	private static final int CABECERA = 13;
	/** Fila de arriba, sus siete columnas. */
	private static final int[] FILA_A = {19, 20, 21, 22, 23, 24, 25};
	/** Fila de abajo, sus siete columnas. Se usan las impares para centrar. */
	private static final int[] FILA_B = {28, 29, 30, 31, 32, 33, 34};
	/** Centro de la ultima fila, para la accion que cierra la pantalla. */
	private static final int PIE = 40;

	// Regla fija para cualquier par de si/no, en cualquier pantalla: el NO a la
	// izquierda y el SI a la derecha, siempre. Tenerlo al reves en una pantalla y
	// no en otra hace que la gente confirme de memoria y se equivoque.
	private static final int SLOT_NO = 29;
	private static final int SLOT_SI = 33;

	// Paleta de Luniatic, con un rol fijo cada color. Mezclarlos deja al jugador sin
	// forma de saber que significa un color cuando lo ve.
	/** Titulo del boton y valor activo. */
	private static final String MARCA = "&#9235FF";
	/** Prosa: lo que explica el boton. */
	private static final String CUERPO = "&#E4D9FF";
	/** Estructura: la accion del clic, los estados apagados y lo que no se puede tocar. */
	private static final String ETIQUETA = "&#7162FF";
	/**
	 * <b>Riesgo real, nada mas.</b> Perder cosas, ser atacable, una accion sin vuelta atras.
	 *
	 * <p>Antes tambien pintaba de rojo el boton de cerrar, un permiso que falta y cualquier
	 * ajuste en NO. Con el rojo puesto en cosas inofensivas deja de avisar de las que si
	 * importan, que es lo unico para lo que sirve.
	 */
	private static final String ERROR = "&#FF4554";

	// La pista de clic va SOLO donde el clic no se adivina: interruptores y selectores,
	// diciendo que va a pasar ("Clic para ponerlo en NO"). En un menu donde todo se
	// clickea, "Clic para ver la lista" abajo de un boton que dice "Miembros" es ruido, y
	// tenerla en la mitad de los botones hace dudar de la otra mitad.


	private Menus() {
	}

	// ------------------------------------------------------------------ portada

	public static void abrirPortada(Player jugador) {
		Team clan = Team.getTeam(jugador);
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Clanes", clan == null ? "sin clan" : limpiar(clan.getName()));

		if (clan == null) {
			inv.setItem(CABECERA, boton(Material.PAPER, "Todavía no tienes clan",
					CUERPO + "Un clan te da chat propio, cofre compartido,",
					CUERPO + "casa del clan y banco en común.",
					"",
					CUERPO + "A los abiertos entras con un clic;",
					CUERPO + "a los cerrados hay que ser invitado."));

			inv.setItem(FILA_A[1], boton(Material.COMPASS, "Explorar clanes",
					CUERPO + "Todos los clanes del servidor.",
					CUERPO + "A los abiertos te sumas desde ahí."));
			holder.asignar(FILA_A[1], j -> abrirLista(j, 0, null));

			inv.setItem(FILA_A[3], boton(Material.WRITABLE_BOOK, "Crear tu clan",
					CUERPO + "Es lo único que hay que escribir,",
					CUERPO + "porque el nombre lo eliges tú.",
					"", ETIQUETA + "Clic y te paso el comando"));
			holder.asignar(FILA_A[3], j -> {
				j.closeInventory();
				mensaje(j, "Para crear tu clan escribí " + ETIQUETA + "/clan create <nombre>");
			});

			inv.setItem(FILA_A[5], boton(Material.NETHER_STAR, "Ranking",
					CUERPO + "Los clanes con más puntaje."));
			holder.asignar(FILA_A[5], j -> abrirRanking(j));
		} else {
			TeamPlayer yo = clan.getTeamPlayer(jugador);
			boolean mando = yo != null && yo.getRank() != PlayerRank.DEFAULT;

			inv.setItem(CABECERA, boton(Material.BOOK, limpiar(clan.getName()),
					unir(datosClan(clan), "", ETIQUETA + "Clic para ver la ficha")));
			holder.asignar(CABECERA, j -> abrirFicha(j, clan.getName(), () -> abrirPortada(j)));

			// Fila de arriba: el dia a dia, agrupado — tu gente, los bienes compartidos,
			// los lugares, y al final lo que pasa con los otros clanes.
			inv.setItem(FILA_A[0], boton(Material.PLAYER_HEAD, "Miembros",
					CUERPO + "Conectados: " + MARCA + clan.getMembers().getOnlinePlayers().size()
							+ CUERPO + " de " + MARCA + clan.getMembers().size()));
			holder.asignar(FILA_A[0], j -> abrirMiembros(j, 0));

			// El cofre no viene con el clan: es el primer hito. Bloqueado se muestra igual y
			// apagado —misma regla que sacar del banco sin mando— porque un boton que
			// desaparece no ensenia que existe algo por conseguir, que es todo el punto.
			// Y el clic no se pierde: lleva a la pantalla que explica que falta.
			boolean cofre = Main.plugin.getHitosManager() == null
					|| Main.plugin.getHitosManager().tiene(clan.getID(), Recompensa.COFRE);
			inv.setItem(FILA_A[1], cofre
					? boton(Material.ENDER_CHEST, "Cofre del clan",
							CUERPO + "El cofre compartido de todo el clan.")
					: boton(Material.GRAY_DYE, "Cofre del clan",
							CUERPO + "El cofre compartido de todo el clan.",
							"",
							ETIQUETA + "Se desbloquea con " + textoFaltanCofre(clan)));
			holder.asignar(FILA_A[1], cofre
					? j -> comando(j, "clan echest")
					: Menus::abrirHitos);

			inv.setItem(FILA_A[2], boton(Material.GOLD_INGOT, "Banco del clan",
					CUERPO + "Saldo: " + MARCA + "$" + clan.getBalance()));
			holder.asignar(FILA_A[2], Menus::abrirBanco);

			inv.setItem(FILA_A[3], boton(Material.RED_BED, "Casa del clan",
					CUERPO + "Te lleva a la casa del clan.",
					"",
					mando ? ETIQUETA + "Para moverla, entra en Ajustes"
							: ETIQUETA + "La mueve el líder o un colíder"));
			holder.asignar(FILA_A[3], j -> comando(j, "clan home"));

			inv.setItem(FILA_A[4], boton(Material.OAK_SIGN, "Warps del clan",
					CUERPO + "Los puntos guardados del clan."));
			holder.asignar(FILA_A[4], Menus::abrirWarps);

			inv.setItem(FILA_A[5], boton(Material.SHIELD, "Aliados",
					CUERPO + "Los clanes con los que no te pegas."));
			holder.asignar(FILA_A[5], j -> abrirAliados(j));

			inv.setItem(FILA_A[6], itemDuelos(clan));
			holder.asignar(FILA_A[6], Menus::abrirDuelos);

			// Fila de abajo: lo que no es el dia a dia del clan. Cuatro columnas pares para
			// que quede pareja: primero lo tuyo, despues lo de afuera, y el mando al final.
			inv.setItem(FILA_B[0], itemChat(clan, jugador));
			holder.asignar(FILA_B[0], j -> {
				j.performCommand("clan chat");
				abrirPortada(j);
			});

			inv.setItem(FILA_B[2], boton(Material.COMPASS, "Explorar clanes",
					CUERPO + "Todos los clanes del servidor.",
					"", ETIQUETA + "Buscar por nombre: /clan menu <texto>"));
			holder.asignar(FILA_B[2], j -> abrirLista(j, 0, null));

			// En el centro exacto de la fila, y no al lado del cofre: los hitos no son una
			// funcion mas del dia a dia, son la escalera de la que cuelgan varias de las
			// otras. Los huecos quedan simetricos alrededor.
			//
			// Con el sistema apagado el boton no va: no habria nada que mostrar, y un boton
			// que abre una pantalla vacia se lee como que algo se rompio.
			if (Main.plugin.getHitosManager() != null && Main.plugin.getHitosManager().isHabilitado()) {
				inv.setItem(FILA_B[3], itemHitos(clan));
				holder.asignar(FILA_B[3], Menus::abrirHitos);
			}

			inv.setItem(FILA_B[4], boton(Material.NETHER_STAR, "Ranking",
					CUERPO + "Los clanes con más puntaje."));
			holder.asignar(FILA_B[4], j -> abrirRanking(j));

			inv.setItem(FILA_B[6], boton(Material.COMPARATOR, "Ajustes del clan",
					mando ? CUERPO + "Identidad, color, icono y cómo se entra."
							: ETIQUETA + "Los toca el líder o un colíder."));
			holder.asignar(FILA_B[6], j -> abrirAjustes(j));

			// Si es el unico duenio no puede irse: el plugin se lo va a negar. En ese
			// caso lo que corresponde ofrecer es disolver, no salir.
			boolean unicoDuenio = yo != null && yo.getRank() == PlayerRank.OWNER
					&& clan.getRank(PlayerRank.OWNER).size() == 1;
			if (unicoDuenio) {
				inv.setItem(PIE, boton(Material.TNT, ERROR + "Disolver el clan",
						CUERPO + "Eres el único dueño, así que no puedes irte:",
						CUERPO + "o le pasas el mando a alguien, o lo disuelves.",
						"", ERROR + "Se pierden el cofre y el banco."));
				holder.asignar(PIE, j -> abrirConfirmacion(j, "Disolver el clan",
						new String[]{
								CUERPO + "Se borra " + MARCA + limpiar(clan.getName()) + CUERPO + " para todos.",
								ERROR + "Se pierden los ítems del cofre y el dinero del banco."
						}, "clan disband confirm", () -> abrirPortada(j)));
			} else {
				inv.setItem(PIE, boton(Material.IRON_DOOR, ERROR + "Salir del clan",
						CUERPO + "Te vas de " + MARCA + limpiar(clan.getName()) + CUERPO + ".",
						"", ERROR + "Pierdes el cofre, el banco y la casa."));
				holder.asignar(PIE, j -> abrirConfirmacion(j, "Salir del clan",
						new String[]{
								CUERPO + "Te vas de " + MARCA + limpiar(clan.getName()) + CUERPO + ".",
								CUERPO + "Pierdes el acceso al cofre y al banco del clan."
						}, "clan leave", () -> abrirPortada(j)));
			}
		}

		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	/**
	 * El canal en el que escribes.
	 *
	 * <p>🔑 <b>Vive en la portada y no en Ajustes, que es donde estaba.</b> Todo lo demas de
	 * Ajustes cambia el clan para todos; esto cambia una sola cosa de una sola persona, y
	 * ademas es lo unico de esa pantalla que un miembro comun puede tocar. Sacarlo de ahi es
	 * lo que deja a Ajustes siendo enteramente del mando.
	 *
	 * <p>Y muestra en que canal estas: un interruptor que no dice como esta obliga a
	 * probarlo para saberlo.
	 */
	private static ItemStack itemChat(Team clan, Player jugador) {
		TeamPlayer yo = clan.getTeamPlayer(jugador);
		boolean interno = yo != null && yo.isInTeamChat();
		return boton(interno ? Material.WRITABLE_BOOK : Material.PAPER, "Tu chat",
				CUERPO + "Escribes en: " + (interno ? MARCA + "el chat del clan" : MARCA + "el chat general"),
				"",
				CUERPO + "En el del clan sólo te leen los tuyos.",
				"", ETIQUETA + "Clic para pasarte al "
						+ (interno ? "chat general" : "chat del clan"));
	}

	// ------------------------------------------------------------------ duelos

	private static ItemStack itemDuelos(Team clan) {
		DueloManager manager = Main.plugin.getDueloManager();
		if (manager == null || !manager.isHabilitado()) {
			return boton(Material.GRAY_DYE, CUERPO + "Duelos",
					CUERPO + "Estan desactivados en este servidor.");
		}
		Duelo duelo = manager.getDuelo(clan);
		if (duelo != null) {
			Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
			return boton(Material.NETHERITE_SWORD, ERROR + "Duelo en curso",
					CUERPO + "Contra " + MARCA + (rival == null ? "?" : limpiar(rival.getName())),
					CUERPO + "Caidas: tú " + MARCA + duelo.getBajas(clan.getID())
							+ CUERPO + ", ellos " + MARCA
							+ (rival == null ? "?" : duelo.getBajas(rival.getID())),
					"", ETIQUETA + "Clic para ver el duelo");
		}
		List<Desafio> recibidos = manager.getDesafiosRecibidos(clan);
		if (!recibidos.isEmpty()) {
			Team retador = Team.getTeam(recibidos.get(0).getRetador());
			return boton(Material.BELL, MARCA + "Te desafiaron",
					CUERPO + (retador == null ? "?" : limpiar(retador.getName()))
							+ CUERPO + " quiere duelo por " + MARCA + "$"
							+ fmt(recibidos.get(0).getApuesta()),
					recibidos.size() > 1
							? CUERPO + "y " + MARCA + (recibidos.size() - 1) + CUERPO + " invitación(es) más"
							: "",
					"", ETIQUETA + "Clic para responder");
		}
		return boton(Material.IRON_SWORD, "Duelos",
				CUERPO + "Un duelo pactado contra otro clan,",
				CUERPO + "con una apuesta que ponen los dos.",
				"", ETIQUETA + "Clic para entrar");
	}

	public static void abrirDuelos(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		DueloManager manager = Main.plugin.getDueloManager();
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Duelos", limpiar(clan.getName()));

		if (manager == null || !manager.isHabilitado()) {
			inv.setItem(CABECERA, boton(Material.GRAY_DYE, CUERPO + "Duelos desactivados",
					CUERPO + "El servidor los tiene apagados."));
			volverA(inv, holder, Menus::abrirPortada);
			jugador.openInventory(inv);
			return;
		}

		Duelo duelo = manager.getDuelo(clan);
		List<Desafio> recibidos = manager.getDesafiosRecibidos(clan);
		// Lider y colider, igual que el comando: si el menu mostrara el boton a un
		// miembro comun, seria un boton que no hace nada.
		boolean mando = tieneMando(clan, jugador);

		if (duelo != null) {
			Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
			String nombreRival = rival == null ? "?" : limpiar(rival.getName());
			// 🔑 El objetivo y el tiempo salen del DUELO, no del config: cada duracion trae
			// el suyo, asi que el valor global mentia en todo duelo que no fuera el preset
			// mas corto — decia 10 en uno de 7 dias, que tiene 50.
			inv.setItem(CABECERA, boton(Material.NETHERITE_SWORD, ERROR + "Duelo contra " + nombreRival,
					CUERPO + "Pozo: " + MARCA + "$" + fmt(duelo.getPozo()),
					CUERPO + "Pierde el que llegue a " + MARCA + duelo.getObjetivoBajas()
							+ CUERPO + " caídas",
					"",
					CUERPO + "Ustedes cayeron " + MARCA + duelo.getBajas(clan.getID()) + CUERPO + " veces",
					CUERPO + "Ellos cayeron " + MARCA
							+ (rival == null ? "?" : duelo.getBajas(rival.getID())) + CUERPO + " veces",
					"",
					CUERPO + "Queda " + MARCA + tiempoCorto(duelo.segundosRestantes(System.currentTimeMillis()))
							+ CUERPO + ". Al vencer gana el que menos cayo."));

			inv.setItem(FILA_B[1], boton(Material.CLOCK, "Actualizar",
					CUERPO + "Vuelve a leer el marcador."));
			holder.asignar(FILA_B[1], Menus::abrirDuelos);

			if (!duelo.esPrincipal(clan.getID())) {
				Team principal = Team.getTeam(duelo.getPrincipalDe(clan.getID()));
				inv.setItem(FILA_B[5], boton(Material.SHIELD, CUERPO + "Entraste como aliado",
						CUERPO + "El duelo lo pactó " + MARCA
								+ (principal == null ? "?" : limpiar(principal.getName())) + CUERPO + ".",
						CUERPO + "Sólo ellos pueden rendirlo, y el pozo",
						CUERPO + "es de ellos: ustedes sólo pelean.",
						"", ERROR + "Mientras dure, pueden matarte."));
			} else if (mando) {
				inv.setItem(FILA_B[5], boton(Material.WHITE_BANNER, ERROR + "Rendirse",
						CUERPO + "Cortas el duelo y el pozo entero",
						CUERPO + "se lo lleva " + MARCA + nombreRival + CUERPO + ".",
						"", CUERPO + "Sirve cuando ya está perdido y",
						CUERPO + "no vale la pena esperar al reloj."));
				holder.asignar(FILA_B[5], j -> abrirConfirmacion(j, "Rendirse",
						new String[]{
								CUERPO + "El pozo de " + MARCA + "$" + fmt(duelo.getPozo())
										+ CUERPO + " se lo lleva " + MARCA + nombreRival + CUERPO + "."
						}, "clan duelo rendirse", () -> abrirDuelos(j)));
			}
		} else {
			// El objetivo NO se puede decir aca: depende de la duracion que se pacte, y
			// darlo como un numero fijo era lo que hacia que despues no coincidiera.
			inv.setItem(CABECERA, boton(cabezaInfo(), "Duelos pactados",
					CUERPO + "Los dos clanes tienen que estar de acuerdo:",
					CUERPO + "uno desafía con un monto y el otro lo acepta.",
					"",
					CUERPO + "Pierde el bando que llegue primero al",
					CUERPO + "objetivo de caídas, que sale de la duración.",
					CUERPO + "Si se cumple el tiempo, gana el que menos cayó.",
					"",
					CUERPO + "No se toca nada de los claims: es pelea",
					CUERPO + "entre personas, no permiso para romper."));

			if (mando) {
				inv.setItem(FILA_B[1], boton(Material.IRON_SWORD, "Desafiar a un clan",
						CUERPO + "Eliges contra quién y cuánto.",
						CUERPO + "No hace falta que estén conectados."));
				holder.asignar(FILA_B[1], j -> abrirElegirRival(j, 0));

				inv.setItem(FILA_B[5], boton(
						recibidos.isEmpty() ? Material.GRAY_DYE : Material.BELL,
						recibidos.isEmpty() ? CUERPO + "Sin invitaciones"
								: MARCA + "Invitaciones (" + recibidos.size() + ")",
						recibidos.isEmpty() ? CUERPO + "Nadie te desafió por ahora."
								: ETIQUETA + "Clic para aceptarlas o rechazarlas"));
				if (!recibidos.isEmpty()) {
					holder.asignar(FILA_B[5], j -> abrirInvitaciones(j));
				}
			} else {
				inv.setItem(FILA_B[3], boton(Material.BARRIER, CUERPO + "Los pacta el mando",
						ETIQUETA + "Sólo el líder o un colíder"));
			}
		}

		// Preferencias: va en la fila de arriba y la ve cualquiera, tenga mando o no. Es
		// justamente lo unico de esta pantalla que decide cada uno por su cuenta.
		inv.setItem(FILA_A[3], itemPreferencias(manager, jugador));
		holder.asignar(FILA_A[3], Menus::abrirPreferenciasDuelo);

		volverA(inv, holder, Menus::abrirPortada);
		jugador.openInventory(inv);
	}

	private static ItemStack itemPreferencias(DueloManager manager, Player jugador) {
		boolean participa = manager.getPreferencias().participa(jugador.getUniqueId());
		return boton(participa ? Material.SHIELD : Material.GRAY_DYE, "Mis preferencias",
				CUERPO + "Entras a los duelos de tu clan: "
						+ (participa ? MARCA + "SÍ" : ETIQUETA + "NO"),
				"",
				CUERPO + "El duelo lo pacta el líder, así que",
				CUERPO + "esto es lo único que decides tú.",
				"", ETIQUETA + "Clic para cambiarlo");
	}

	/**
	 * Quien pelea y quien no.
	 *
	 * <p>Dos decisiones distintas y las toma gente distinta: cada jugador elige si entra a
	 * los duelos de su clan, y el mando elige si el clan entra a las de sus aliados. Las
	 * dos vienen en SI: lo que hace falta es poder salirse, no tener que anotarse.
	 */
	public static void abrirPreferenciasDuelo(Player jugador) {
		Team clan = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (clan == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		boolean participa = manager.getPreferencias().participa(jugador.getUniqueId());
		boolean ayuda = manager.getPreferencias().ayudaAliados(clan.getID());
		boolean enDuelo = manager.getDuelo(clan) != null;
		boolean mando = tieneMando(clan, jugador);

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Preferencias de duelo", limpiar(clan.getName()));

		// El lore se arma con un if y no con ternarios que devuelvan null: boton() llama
		// isEmpty() sobre cada linea, asi que un null lo revienta.
		if (enDuelo) {
			inv.setItem(CABECERA, boton(Material.WRITABLE_BOOK, "Preferencias de duelo",
					CUERPO + "Lo que eliges aquí vale para los",
					CUERPO + "duelos que empiecen de ahora en más.",
					"",
					ETIQUETA + "El duelo en curso no cambia:",
					ETIQUETA + "se decide al arrancar."));
		} else {
			inv.setItem(CABECERA, boton(Material.WRITABLE_BOOK, "Preferencias de duelo",
					CUERPO + "Lo que eliges aquí vale para los",
					CUERPO + "duelos que empiecen de ahora en más."));
		}

		inv.setItem(FILA_A[2], boton(participa ? Material.IRON_SWORD : Material.GRAY_DYE,
				"Entrar a los duelos de mi clan",
				CUERPO + "Ahora: " + (participa ? MARCA + "SÍ" : ETIQUETA + "NO"),
				"",
				CUERPO + "En NO, los rivales no te pueden matar",
				CUERPO + "por el duelo y tus caídas no cuentan.",
				CUERPO + "Tampoco te alcanzan sus restricciones.",
				"", ETIQUETA + "Clic para poner " + (participa ? "NO" : "SÍ")));
		holder.asignar(FILA_A[2], j -> ejecutarYVolver(j,
				"clan preferencias duelo " + (participa ? "no" : "si"),
				() -> abrirPreferenciasDuelo(j)));

		if (mando) {
			inv.setItem(FILA_A[4], boton(ayuda ? Material.SHIELD : Material.GRAY_DYE,
					"Ayudar a nuestros aliados",
					CUERPO + "Ahora: " + (ayuda ? MARCA + "SÍ" : ETIQUETA + "NO"),
					"",
					CUERPO + "En SÍ, el clan entra automáticamente",
					CUERPO + "en los duelos que pacten sus aliados.",
					CUERPO + "En NO, se queda afuera.",
					"", ETIQUETA + "Clic para poner " + (ayuda ? "NO" : "SÍ")));
			holder.asignar(FILA_A[4], j -> ejecutarYVolver(j,
					"clan preferencias aliados " + (ayuda ? "no" : "si"),
					() -> abrirPreferenciasDuelo(j)));
		} else {
			inv.setItem(FILA_A[4], boton(Material.GRAY_DYE, CUERPO + "Ayudar a nuestros aliados",
					CUERPO + "Ahora: " + (ayuda ? MARCA + "SÍ" : ETIQUETA + "NO"),
					"", CUERPO + "Lo decide el líder o un colíder."));
		}

		volverA(inv, holder, Menus::abrirDuelos);
		jugador.openInventory(inv);
	}

	/**
	 * Las invitaciones a duelo que le llegaron al clan.
	 *
	 * <p>Son varias porque varios clanes pueden desafiarte a la vez. Cada una se
	 * acepta o se rechaza por separado, y rechazar avisa al que la mando: dejarla
	 * vencer en silencio es peor que decir que no.
	 */
	public static void abrirInvitaciones(Player jugador) {
		Team clan = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (clan == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		List<Desafio> recibidos = manager.getDesafiosRecibidos(clan);

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Invitaciones", recibidos.size() + " sin responder");
		vaciarContenido(inv);

		boolean mando = tieneMando(clan, jugador);
		int i = 0;
		for (Desafio desafio : recibidos) {
			if (i >= CONTENIDO.length) {
				break;
			}
			Team retador = Team.getTeam(desafio.getRetador());
			if (retador == null) {
				continue;
			}
			String nombre = retador.getName();
			String monto = fmt(desafio.getApuesta());
			// Con quienes vienen y cuantos son, ANTES de aceptar. Sin esto la queja
			// posterior es inevitable y tiene razon: "no sabia que eran tantos".
			List<String> lore = new ArrayList<>();
			lore.add(CUERPO + "Apuesta: " + MARCA + "$" + monto + CUERPO + " cada uno");
			lore.add("");
			lore.add(ETIQUETA + "Jugadores que van a pelear");
			lore.add(CUERPO + " Su bando: " + MARCA + manager.fuerzaDe(retador)
					+ CUERPO + " (clan + aliados)");
			lore.add(CUERPO + " Su bando de ustedes: " + MARCA + manager.fuerzaDe(clan)
					+ CUERPO + " (clan + aliados)");
			lore.add(ETIQUETA + " No cuenta a los que se bajaron");
			lore.add("");
			Set<UUID> aliadosDeEllos = manager.aliadosQueEntrarian(retador);
			if (aliadosDeEllos.isEmpty()) {
				lore.add(ETIQUETA + "Pelean solos, sin aliados");
			} else {
				lore.add(ETIQUETA + "Aliados que entran con ellos (" + aliadosDeEllos.size() + ")");
				for (UUID idAliado : aliadosDeEllos) {
					Team aliado = Team.getTeam(idAliado);
					if (aliado != null) {
						lore.add(CUERPO + " " + limpiar(aliado.getName()));
					}
				}
			}
			lore.add("");
			lore.add(mando ? MARCA + "Clic izquierdo: aceptar" : CUERPO + "Sólo el líder y los colíderes responden");
			lore.add(mando ? ERROR + "Clic derecho: rechazar" : "");
			inv.setItem(CONTENIDO[i], boton(Material.BELL, limpiar(nombre),
					lore.toArray(new String[0])));
			// Un nombre con espacios correria el monto a otro argumento. Hoy no puede
			// pasar, pero depende de una config que alguien puede vaciar.
			if (mando && argumentoSeguro(nombre)) {
				holder.asignarConClic(CONTENIDO[i],
						j -> abrirConfirmacion(j, "Aceptar el duelo",
								new String[]{
										CUERPO + "Contra " + MARCA + limpiar(nombre) + CUERPO + ".",
										CUERPO + "Se te van " + MARCA + "$" + monto
												+ CUERPO + " del banco del clan.",
										CUERPO + "Los recuperas doblados si ganan."
								}, "clan duelo aceptar " + nombre, () -> abrirInvitaciones(j)),
						j -> ejecutarYVolver(j, "clan duelo rechazar " + nombre,
								() -> abrirInvitaciones(j)));
			}
			i++;
		}

		if (i == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Sin invitaciones",
					CUERPO + "Nadie desafió a tu clan por ahora."));
		}

		volverA(inv, holder, Menus::abrirDuelos);
		jugador.openInventory(inv);
	}

	/** Elegir contra quien, sin escribir el nombre. */
	public static void abrirElegirRival(Player jugador, int pagina) {
		Team propio = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (propio == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		Main.plugin.getFoliaLib().getScheduler().runAsync(t -> {
			List<String> nombres = buscar(null);
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador, t2 -> {
				// El filtro se resuelve por id, sin cargar los clanes: solo se instancian los de la pagina.
				List<String> candidatos = new ArrayList<>();
				for (String nombre : nombres) {
					UUID id = Team.getTeamManager().getTeamUUID(nombre);
					if (id != null && !id.equals(propio.getID()) && manager.estaLibre(id)) {
						candidatos.add(nombre);
					}
				}

				int porPagina = CONTENIDO.length;
				int paginas = Math.max(1, (int) Math.ceil(candidatos.size() / (double) porPagina));
				int actual = Math.max(0, Math.min(pagina, paginas - 1));

				MenuHolder holder = new MenuHolder();
				Inventory inv = crear(holder, "Desafiar", (actual + 1) + "/" + paginas);
				vaciarContenido(inv);

				int desde = actual * porPagina;
				for (int i = desde; i < candidatos.size() && i - desde < porPagina; i++) {
					Team rival = Team.getTeam(candidatos.get(i));
					if (rival == null) {
						continue;
					}
					inv.setItem(CONTENIDO[i - desde], boton(Material.IRON_SWORD,
							limpiar(rival.getName()), datosClan(rival)));
					String nombre = rival.getName();
					holder.asignar(CONTENIDO[i - desde], j -> abrirElegirApuesta(j, nombre));
				}
				if (candidatos.isEmpty()) {
					inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "No hay a quién desafiar",
							CUERPO + "No hay otro clan libre en este momento."));
				}

				if (actual > 0) {
					inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
					holder.asignar(SLOT_ANTERIOR, j -> abrirElegirRival(j, actual - 1));
				}
				if (actual < paginas - 1) {
					inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
					holder.asignar(SLOT_SIGUIENTE, j -> abrirElegirRival(j, actual + 1));
				}
				volverA(inv, holder, Menus::abrirDuelos);
				jugador.openInventory(inv);
			});
		});
	}

	/** Elegir cuanto, sin escribir el monto. */
	public static void abrirElegirApuesta(Player jugador, String nombreRival) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Apuesta", "contra " + limpiar(nombreRival));

		inv.setItem(CABECERA, boton(Material.PAPER, "Cuánto se juega",
				CUERPO + "Cada clan pone lo mismo y el ganador",
				CUERPO + "se lleva las dos partes.",
				"",
				CUERPO + "Tu banco tiene " + MARCA + "$" + clan.getBalance() + CUERPO + ".",
				"",
				CUERPO + "El otro clan tiene que aceptar el mismo",
				CUERPO + "monto para que arranque."));

		if (!argumentoSeguro(nombreRival)) {
			// Ver argumentoSeguro: con espacios en el nombre, el monto se corre.
			inv.setItem(CABECERA, boton(Material.BARRIER, ERROR + "Ese clan no se puede desafiar",
					CUERPO + "Su nombre tiene caracteres que rompen el comando."));
			volverA(inv, holder, j -> abrirElegirRival(j, 0));
			jugador.openInventory(inv);
			return;
		}

		int[] opciones = {0, 100, 1000, 10000};
		Material[] iconos = {Material.PAPER, Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.GOLD_BLOCK};
		int[] slots = {FILA_B[0], FILA_B[2], FILA_B[4], FILA_B[6]};
		for (int i = 0; i < opciones.length; i++) {
			int monto = opciones[i];
			inv.setItem(slots[i], boton(iconos[i], monto == 0 ? "Sin apuesta" : "$" + monto,
					monto == 0 ? CUERPO + "Sólo por el orgullo." : CUERPO + "Cada clan pone $" + monto));
			holder.asignar(slots[i], j -> abrirElegirDuracion(j, nombreRival, monto));
		}

		volverA(inv, holder, j -> abrirElegirRival(j, 0));
		jugador.openInventory(inv);
	}

	/**
	 * Tercer paso del desafio: cuanto dura.
	 *
	 * <p>Va despues del monto y antes de confirmar, y es una pantalla propia y no un
	 * detalle de la anterior porque <b>la duracion cambia lo que es el duelo</b>: media
	 * hora es una pelea pactada, siete dias es un duelo. Cada preset trae su objetivo
	 * de caidas, asi que el boton muestra los dos numeros juntos.
	 *
	 * <p>Los dos clanes tienen que pactar la misma, igual que el monto: aceptar un duelo
	 * de 30 minutos no puede arrancar uno de una semana.
	 */
	public static void abrirElegirDuracion(Player jugador, String nombreRival, int monto) {
		Team clan = Team.getTeam(jugador);
		DueloManager manager = Main.plugin.getDueloManager();
		if (clan == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Duracion", "contra " + limpiar(nombreRival));

		inv.setItem(CABECERA, boton(Material.CLOCK, "Cuánto dura",
				CUERPO + "Termina antes si un clan llega al",
				CUERPO + "objetivo de caídas.",
				"",
				CUERPO + "El otro clan tiene que aceptar la misma",
				CUERPO + "duración para que arranque."));

		// De menor a mayor compromiso: el color dice solo que lo de abajo es mas serio.
		Material[] iconos = {Material.LIME_DYE, Material.YELLOW_DYE, Material.ORANGE_DYE, Material.RED_DYE};
		List<DueloManager.Duracion> duraciones = manager.getDuraciones();
		for (int i = 0; i < duraciones.size() && i < FILA_B.length; i++) {
			DueloManager.Duracion duracion = duraciones.get(i);
			inv.setItem(FILA_B[i], boton(iconos[Math.min(i, iconos.length - 1)], duracion.etiqueta,
					CUERPO + "Objetivo: " + MARCA + duracion.bajas + CUERPO + " caídas del rival.",
					"", ETIQUETA + "Clic para mandar el desafío"));
			holder.asignar(FILA_B[i], j -> abrirConfirmacion(j, "Desafiar",
					new String[]{
							CUERPO + "Le mandas el desafío a " + MARCA + limpiar(nombreRival) + CUERPO + ".",
							CUERPO + "Apuesta: " + MARCA + "$" + monto + CUERPO + " cada uno.",
							CUERPO + "Dura: " + MARCA + duracion.etiqueta + CUERPO + ", a "
									+ MARCA + duracion.bajas + CUERPO + " caídas.",
							"",
							CUERPO + "No pasa nada hasta que ellos acepten."
					}, "clan duelo " + nombreRival + " " + monto + " " + duracion.id,
					() -> abrirElegirDuracion(j, nombreRival, monto)));
		}

		volverA(inv, holder, j -> abrirElegirApuesta(j, nombreRival));
		jugador.openInventory(inv);
	}

	// -------------------------------------------------------------------- lista

	public static void abrirLista(Player jugador, int pagina, String filtro) {
		// Ordenar lee el yml de todos los clanes: se hace fuera del hilo del tick. Lo que vuelve
		// son nombres; los clanes se resuelven despues, en el hilo principal y solo los de la pagina.
		Main.plugin.getFoliaLib().getScheduler().runAsync(t -> {
			List<String> nombres = buscar(filtro);
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador,
					t2 -> jugador.openInventory(construirLista(jugador, nombres, pagina, filtro)));
		});
	}

	private static Inventory construirLista(Player jugador, List<String> nombres, int pagina, String filtro) {
		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(nombres.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Explorar clanes",
				(actual + 1) + "/" + paginas + (filtro == null ? "" : "  filtro: " + filtro));
		vaciarContenido(inv);

		Team propio = Team.getTeam(jugador);
		int desde = actual * porPagina;
		int puestos = 0;
		for (int i = desde; i < nombres.size() && i - desde < porPagina; i++) {
			String nombre = nombres.get(i);
			Team clan = Team.getTeam(nombre);
			if (clan == null) {
				continue;
			}
			int slot = CONTENIDO[i - desde];
			inv.setItem(slot, itemClan(clan, propio));
			holder.asignar(slot, j -> abrirFicha(j, nombre, () -> abrirLista(j, actual, filtro)));
			puestos++;
		}

		if (puestos == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "No hay nada",
					filtro == null ? "Todavía no hay ningun clan." : "Ningun clan coincide con " + filtro));
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirLista(j, actual - 1, filtro));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirLista(j, actual + 1, filtro));
		}

		nota(inv, "Buscar por nombre", CUERPO + "/clan menu <texto>");
		volverA(inv, holder, Menus::abrirPortada);
		return inv;
	}

	/**
	 * Los NOMBRES de los clanes, ordenados y filtrados. Devuelve nombres y no clanes a
	 * proposito: instanciar un Team los mete en el mapa de cargados de TeamManager, que el
	 * hilo del tick recorre en cada evento de dano. Hacerlo desde el hilo async corrompia
	 * ese mapa, y ademas dejaba cargado para siempre todo clan que alguien mirara una vez.
	 */
	private static List<String> buscar(String filtro) {
		List<String> encontrados = new ArrayList<>();
		String aguja = filtro == null ? null : filtro.toLowerCase(Locale.ROOT);
		for (String nombre : Team.getTeamManager().sortTeamsByMembers()) {
			if (aguja != null && !nombre.toLowerCase(Locale.ROOT).contains(aguja)) {
				continue;
			}
			encontrados.add(nombre);
		}
		return encontrados;
	}

	/**
	 * El material dice de un vistazo si puedes entrar, que es para lo que se abre
	 * este menu. No se usan cabezas: pedir una por clan obliga a resolver perfiles,
	 * y con online-mode apagado ademas salen mal.
	 */
	/**
	 * El clan como item de la lista.
	 *
	 * <p>🔑 <b>El material pasó a ser el icono elegido por el clan, y eso cambio de lugar
	 * una señal.</b> Antes el material decia si podias entrar —lana verde, celeste o gris—;
	 * ahora dice <b>quien</b> es el clan, que es lo que sirve cuando hay veinte en pantalla
	 * y todos se llaman distinto pero se ven igual. El estado de entrada se lee igual de
	 * rapido en la primera linea, con color y en una palabra.
	 */
	private static ItemStack itemClan(Team clan, Team propio) {
		boolean esMio = propio != null && propio.getID().equals(clan.getID());
		List<String> lore = new ArrayList<>();
		if (esMio) {
			lore.add(MARCA + "Tu clan");
		} else if (clan.isOpen()) {
			lore.add(MARCA + "Abierto: entras con un clic");
		} else {
			lore.add(ETIQUETA + "Cerrado: sólo por invitación");
		}
		lore.add("");
		lore.addAll(Arrays.asList(datosClan(clan)));
		lore.add("");
		lore.add(ETIQUETA + "Clic para ver la ficha");
		return boton(Main.plugin.getClanIconos().getIcono(clan.getID()),
				limpiar(clan.getName()), lore.toArray(new String[0]));
	}

	/**
	 * Los tres datos que describen a un clan, iguales en todas las pantallas.
	 *
	 * <p>Ya no incluye si esta abierto: eso es una <b>accion</b> —puedes entrar o no— y vive
	 * arriba de todo en la lista, no mezclado entre los numeros. Repetirlo en los dos
	 * lugares hacia que la ficha lo dijera dos veces.
	 */
	private static String[] datosClan(Team clan) {
		return new String[]{
				CUERPO + "Miembros: " + MARCA + clan.getMembers().size()
						+ CUERPO + " (" + MARCA + clan.getMembers().getOnlinePlayers().size()
						+ CUERPO + " conectados)",
				CUERPO + "Puntaje: " + MARCA + clan.getScore(),
				CUERPO + "Banco: " + MARCA + "$" + clan.getBalance()
		};
	}

	// -------------------------------------------------------------------- ficha

	public static void abrirFicha(Player jugador, String nombreClan, Runnable volver) {
		Team clan = Team.getTeam(nombreClan);
		if (clan == null) {
			mensaje(jugador, ERROR + "Ese clan ya no existe.");
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, limpiar(clan.getName()), "ficha del clan");

		List<String> datos = new ArrayList<>(Arrays.asList(datosClan(clan)));
		String tag = clan.getTag();
		if (tag != null && !tag.isEmpty()) {
			datos.add(0, CUERPO + "Etiqueta: " + MARCA + limpiar(tag));
		}
		String descripcion = clan.getDescription();
		if (descripcion != null && !descripcion.isEmpty()) {
			datos.add("");
			datos.add(CUERPO + limpiar(descripcion));
		}
		// La ficha se abre con el icono del clan, que es como lo viste en la lista: si
		// cambiara de item entre una pantalla y otra habria que volver a reconocerlo.
		inv.setItem(FILA_A[1], boton(Main.plugin.getClanIconos().getIcono(clan.getID()),
				limpiar(clan.getName()), datos.toArray(new String[0])));

		inv.setItem(FILA_A[3], boton(Material.PLAYER_HEAD, "Miembros",
				CUERPO + "Conectados: " + MARCA + clan.getMembers().getOnlinePlayersString(),
				CUERPO + "Desconectados: " + MARCA + clan.getMembers().getOfflinePlayersString()));

		Team propio = Team.getTeam(jugador);
		if (propio != null && propio.getID().equals(clan.getID())) {
			inv.setItem(FILA_A[5], boton(Material.LIME_DYE, "Este es tu clan"));
		} else if (propio != null) {
			// Con clan propio lo util no es unirse sino proponer alianza.
			if (tieneMando(propio, jugador) && !propio.isAlly(clan)) {
				inv.setItem(FILA_A[5], boton(Material.SHIELD, "Proponer alianza",
						CUERPO + "Le manda la propuesta a " + MARCA + limpiar(clan.getName()) + CUERPO + ".",
						CUERPO + "Se sella cuando ellos la piden de vuelta."));
				holder.asignar(FILA_A[5], j -> ejecutarYVolver(j, "clan ally " + clan.getName(),
						() -> abrirFicha(j, nombreClan, volver)));
			} else if (propio.isAlly(clan)) {
				inv.setItem(FILA_A[5], boton(Material.SHIELD, "Ya son aliados"));
			} else {
				inv.setItem(FILA_A[5], boton(Material.BARRIER, "Ya estás en un clan",
						CUERPO + "Sólo el mando puede proponer alianzas."));
			}
		} else if (clan.isOpen()) {
			inv.setItem(FILA_A[5], boton(Material.LIME_DYE, "Unirte a este clan",
					ETIQUETA + "Clic para entrar"));
			// Se delega en el comando: permisos, limites, baneos y cooldowns siguen valiendo.
			holder.asignar(FILA_A[5], j -> comando(j, "clan join " + clan.getName()));
		} else {
			inv.setItem(FILA_A[5], boton(Material.GRAY_DYE, "Sólo por invitación",
					CUERPO + "Alguien del clan tiene que invitarte."));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, j -> volver.run());
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ----------------------------------------------------------------- miembros

	public static void abrirMiembros(Player jugador, int pagina) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		List<TeamPlayer> miembros = new ArrayList<>(clan.getMembers().getClone());
		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(miembros.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Miembros", (actual + 1) + "/" + paginas);
		vaciarContenido(inv);

		boolean mando = tieneMando(clan, jugador);
		int desde = actual * porPagina;
		for (int i = desde; i < miembros.size() && i - desde < porPagina; i++) {
			TeamPlayer miembro = miembros.get(i);
			boolean conectado = miembro.getPlayer() != null && miembro.getPlayer().isOnline();
			String nombre = miembro.getPlayer() == null ? "?" : miembro.getPlayer().getName();
			boolean yoMismo = nombre.equals(jugador.getName());
			inv.setItem(CONTENIDO[i - desde], boton(
					conectado ? Material.PLAYER_HEAD : Material.SKELETON_SKULL,
					(conectado ? MARCA : CUERPO) + nombre,
					CUERPO + "Rango: " + MARCA + rango(miembro.getRank()),
					conectado ? MARCA + "Conectado" : CUERPO + "Desconectado",
					"",
					mando && !yoMismo ? ETIQUETA + "Clic para administrarlo"
							: CUERPO + (yoMismo ? "Eres tú." : "")));
			if (mando && !yoMismo) {
				holder.asignar(CONTENIDO[i - desde], j -> abrirMiembro(j, nombre));
			}
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirMiembros(j, actual - 1));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirMiembros(j, actual + 1));
		}
		if (mando) {
			inv.setItem(SLOT_CERRAR - 3, boton(Material.WRITABLE_BOOK, "Invitar gente",
					CUERPO + "Elegí de los que están conectados."));
			holder.asignar(SLOT_CERRAR - 3, j -> abrirInvitar(j, 0));
		}
		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------ aliados

	public static void abrirAliados(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Aliados", limpiar(clan.getName()));
		vaciarContenido(inv);

		int i = 0;
		for (UUID id : clan.getAllies().getClone()) {
			if (i >= CONTENIDO.length) {
				break;
			}
			Team aliado = Team.getTeam(id);
			if (aliado == null) {
				continue;
			}
			inv.setItem(CONTENIDO[i], boton(Material.SHIELD, limpiar(aliado.getName()), datosClan(aliado)));
			String nombre = aliado.getName();
			holder.asignar(CONTENIDO[i], j -> abrirFicha(j, nombre, () -> abrirAliados(j)));
			i++;
		}
		if (i == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Sin aliados",
					CUERPO + "Un aliado no te puede pegar, y entra",
					CUERPO + "contigo a los duelos que pactes."));
		}

		nota(inv, "Proponer alianza",
				CUERPO + "Desde la ficha de un clan, o con",
				CUERPO + "/clan ally <clan>",
				"",
				ETIQUETA + "Se sella cuando ellos la piden de vuelta");
		volverA(inv, holder, Menus::abrirPortada);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------ ranking

	public static void abrirRanking(Player jugador) {
		Main.plugin.getFoliaLib().getScheduler().runAsync(t -> {
			String[] orden = Team.getTeamManager().sortTeamsByScore();
			Main.plugin.getFoliaLib().getScheduler().runAtEntity(jugador, t2 -> {
				MenuHolder holder = new MenuHolder();
				Inventory inv = crear(holder, "Ranking", "por puntaje");
				vaciarContenido(inv);

				// El puesto lo cuenta esta pantalla y no el indice del arreglo: un clan que
				// no se pueda leer no tiene que dejar un hueco ni saltearse un numero.
				int puesto = 0;
				for (int i = 0; i < orden.length && puesto < CONTENIDO.length; i++) {
					Team clan = Team.getTeam(orden[i]);
					if (clan == null) {
						continue;
					}
					puesto++;
					int slot = CONTENIDO[puesto - 1];
					inv.setItem(slot, boton(CabezasNumero.cabeza(puesto),
							"#" + puesto + "  " + limpiar(clan.getName()), datosClan(clan)));
					String nombre = clan.getName();
					holder.asignar(slot, j -> abrirFicha(j, nombre, () -> abrirRanking(j)));
				}
				if (puesto == 0) {
					inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Todavía no hay clanes"));
				}

				nota(inv, "Cómo se puntúa",
						CUERPO + "El puntaje sale de las misiones",
						CUERPO + "que completan los del clan.",
						"",
						ETIQUETA + "Se reinicia el 1 de cada mes");
				volverA(inv, holder, Menus::abrirPortada);
				jugador.openInventory(inv);
			});
		});
	}

	// ------------------------------------------------------------------ ajustes

	/**
	 * Los ajustes del clan, que ahora son <b>todos del mando</b>.
	 *
	 * <p>🔑 <b>Se le puede poner puerta porque el chat se fue a la portada.</b> Era el unico
	 * item de rango DEFAULT que habia aca, asi que la pantalla se le abria a cualquiera y
	 * despues cada boton le fallaba por consola: seis trampas y ninguna pista de cual iba a
	 * andar. Sin el, la puerta no le saca nada a nadie.
	 *
	 * <p>Y adentro hay dos escalones, no uno: casi todo es del <b>lider</b> y el colider solo
	 * puede el icono y la casa. Lo que no le corresponde se le muestra igual —para que sepa
	 * que existe— pero apagado y diciendo de quien es.
	 *
	 * <p>El orden es: arriba <b>quien es el clan</b>, abajo <b>como funciona</b>, y al pie lo
	 * que no tiene vuelta atras. Es la misma forma que la portada, donde salir del clan
	 * tambien vive al pie.
	 */
	public static void abrirAjustes(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		if (!tieneMando(clan, jugador)) {
			mensaje(jugador, "Los ajustes del clan los toca el líder o un colíder.");
			abrirPortada(jugador);
			return;
		}
		TeamPlayer yo = clan.getTeamPlayer(jugador);
		boolean esLider = yo != null && yo.getRank() == PlayerRank.OWNER;

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Ajustes", limpiar(clan.getName()));

		// Arriba: quien es el clan hacia afuera.
		inv.setItem(FILA_A[1], boton(Material.NAME_TAG, "Nombre, etiqueta y descripción",
				CUERPO + "Cómo se llama el clan y con qué",
				CUERPO + "etiqueta aparece en el chat.",
				soloLider(esLider)));
		if (esLider) {
			holder.asignar(FILA_A[1], Menus::abrirIdentidad);
		}

		inv.setItem(FILA_A[3], boton(Material.MAGENTA_DYE, "Color del clan",
				CUERPO + "Pinta la etiqueta del clan en el chat",
				CUERPO + "y sobre la cabeza de cada miembro.",
				soloLider(esLider)));
		if (esLider) {
			holder.asignar(FILA_A[3], Menus::abrirColores);
		}

		inv.setItem(FILA_A[5], boton(Main.plugin.getClanIconos().getIcono(clan.getID()),
				"Icono del clan",
				CUERPO + "Con qué se muestra tu clan cuando",
				CUERPO + "alguien explora la lista de clanes."));
		holder.asignar(FILA_A[5], j -> abrirIconos(j, 0));

		// Abajo: cómo funciona el clan puertas adentro.
		inv.setItem(FILA_B[2], boton(clan.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
				clan.isOpen() ? "Clan abierto" : "Clan cerrado",
				clan.isOpen() ? CUERPO + "Cualquiera entra solo."
						: CUERPO + "Sólo se entra por invitación.",
				esLider ? ETIQUETA + "Clic para dejarlo "
						+ (clan.isOpen() ? "sólo por invitación" : "abierto")
						: soloLider(false)));
		if (esLider) {
			holder.asignar(FILA_B[2], j -> {
				j.performCommand("clan open");
				abrirAjustes(j);
			});
		}

		inv.setItem(FILA_B[4], boton(Material.RED_BED, "Fijar la casa aquí",
				CUERPO + "Deja la casa del clan donde estás parado.",
				"", ETIQUETA + "La usa todo el clan con /clan home"));
		holder.asignar(FILA_B[4], j -> ejecutarYVolver(j, "clan sethome", () -> abrirAjustes(j)));

		if (esLider) {
			inv.setItem(PIE, boton(Material.TNT, ERROR + "Disolver el clan",
					CUERPO + "Borra el clan para todos.",
					"", ERROR + "Se pierden el cofre y el banco."));
			// "disband confirm" en un solo tiro: el comando pelado abre su propia
			// confirmacion por chat, que despues de una pantalla de confirmacion sobra.
			holder.asignar(PIE, j -> abrirConfirmacion(j, "Disolver el clan",
					new String[]{
							CUERPO + "Se borra " + MARCA + limpiar(clan.getName()) + CUERPO + " para todos.",
							ERROR + "Se pierden los ítems del cofre y el dinero del banco."
					}, "clan disband confirm", () -> abrirAjustes(j)));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirPortada);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	/**
	 * La ultima linea de un boton que el colider ve pero no puede usar.
	 *
	 * <p>Devuelve cadena vacia para el lider: {@code boton()} trata la vacia como renglon en
	 * blanco, asi que el mismo arreglo de lore sirve para los dos casos sin armarlo aparte.
	 */
	private static String soloLider(boolean esLider) {
		return esLider ? "" : ETIQUETA + "Esto lo cambia el líder";
	}

	// ---------------------------------------------------------------- identidad

	/**
	 * Nombre, etiqueta y descripcion: lo unico del clan que es texto libre.
	 *
	 * <p>Antes esta pantalla se limitaba a dictar los tres comandos. Ahora los pide por
	 * chat y vuelve sola, que es la regla de la casa: se elige, y lo unico que se
	 * escribe es lo que no se puede ofrecer como opcion.
	 *
	 * <p>La <b>etiqueta</b> es lo unico del clan que entra al chat, y por eso esta
	 * acotada a {@code maxTagLength}: el nombre completo va al tab y al nombre flotante,
	 * donde sobra lugar. Un clan nuevo ya nace con una derivada del nombre, asi que esto
	 * es para cambiarla, no para ponerla.
	 */
	public static void abrirIdentidad(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Identidad", limpiar(clan.getName()));

		int max = Main.plugin.getConfig().getInt("maxTagLength", 4);

		inv.setItem(FILA_A[1], boton(Material.NAME_TAG, "Nombre",
				CUERPO + "Ahora: " + MARCA + limpiar(clan.getName()),
				CUERPO + "Se ve en el tab y sobre la cabeza.",
				"", ETIQUETA + "Clic para cambiarlo"));
		holder.asignar(FILA_A[1], j -> pedirTexto(j,
				"Escribe el nombre nuevo del clan.", "clan name", true));

		inv.setItem(FILA_A[3], boton(Material.OAK_SIGN, "Etiqueta",
				CUERPO + "Ahora: " + MARCA + etiquetaActual(clan),
				CUERPO + "Es lo que sale en el chat, hasta " + MARCA + max + CUERPO + " letras.",
				"", ETIQUETA + "Clic para cambiarla"));
		holder.asignar(FILA_A[3], j -> pedirTexto(j,
				"Escribe la etiqueta nueva, hasta " + max + " caracteres.", "clan tag", true));

		inv.setItem(FILA_A[5], boton(Material.BOOK, "Descripción",
				CUERPO + "Ahora: " + MARCA + descripcionActual(clan),
				"", ETIQUETA + "Clic para cambiarla"));
		holder.asignar(FILA_A[5], j -> pedirTexto(j,
				"Escribe la descripción del clan.", "clan description", false));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirAjustes);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	/**
	 * Pide el texto por chat, ejecuta el comando y vuelve a la misma pantalla.
	 *
	 * <p>Con {@code unaPalabra} los espacios pasan a guion bajo: {@code /clan name} y
	 * {@code /clan tag} toman un solo argumento y si no se quedarian con la primera
	 * palabra sin avisar. La descripcion si acepta varias.
	 */
	private static void pedirTexto(Player jugador, String consigna, String comando, boolean unaPalabra) {
		EsperaTexto.pedir(jugador, consigna,
				texto -> {
					jugador.performCommand(comando + " " + (unaPalabra ? texto.replace(' ', '_') : texto));
					abrirIdentidad(jugador);
				},
				Menus::abrirIdentidad);
	}

	private static String etiquetaActual(Team clan) {
		String tag = clan.getOriginalTag();
		return tag == null || tag.isEmpty() ? "sin etiqueta" : limpiar(tag);
	}

	private static String descripcionActual(Team clan) {
		String desc = clan.getDescription();
		if (desc == null || desc.isEmpty()) {
			return "sin descripción";
		}
		String limpio = limpiar(desc);
		return limpio.length() > 30 ? limpio.substring(0, 30) + "..." : limpio;
	}

	// --------------------------------------------------- acciones sobre un miembro

	public static void abrirMiembro(Player jugador, String nombreMiembro) {
		Team clan = Team.getTeam(jugador);
		if (clan == null || !tieneMando(clan, jugador)) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, limpiar(nombreMiembro), "miembro del clan");

		// Cabecera con a quien estas por tocar. Sin esto el unico lugar donde figuraba el
		// nombre era el titulo de la ventana, que es justo lo que no se mira antes de
		// apretar "Expulsar": tres botones iguales para todos los miembros del clan.
		TeamPlayer miembro = miembroPorNombre(clan, nombreMiembro);
		boolean conectado = miembro != null && miembro.getPlayer() != null
				&& miembro.getPlayer().isOnline();
		inv.setItem(CABECERA, boton(conectado ? Material.PLAYER_HEAD : Material.SKELETON_SKULL,
				limpiar(nombreMiembro),
				CUERPO + "Rango: " + MARCA + (miembro == null ? "?" : rango(miembro.getRank())),
				CUERPO + "Ahora: " + (conectado ? MARCA + "conectado" : ETIQUETA + "desconectado")));

		inv.setItem(FILA_B[1], boton(Material.EMERALD, "Ascender",
				CUERPO + "Le da un escalón más de mando."));
		holder.asignar(FILA_B[1], j -> ejecutarYVolver(j, "clan promote " + nombreMiembro,
				() -> abrirMiembro(j, nombreMiembro)));

		inv.setItem(FILA_B[3], boton(Material.REDSTONE, "Bajar de rango",
				CUERPO + "Le saca un escalón de mando."));
		holder.asignar(FILA_B[3], j -> ejecutarYVolver(j, "clan demote " + nombreMiembro,
				() -> abrirMiembro(j, nombreMiembro)));

		inv.setItem(FILA_B[5], boton(Material.IRON_DOOR, ERROR + "Expulsar del clan",
				CUERPO + "Lo saca del clan y pierde el acceso",
				CUERPO + "al cofre y al banco."));
		holder.asignar(FILA_B[5], j -> abrirConfirmacion(j, "Expulsar a " + limpiar(nombreMiembro),
				new String[]{
						CUERPO + "Se va del clan y pierde el acceso",
						CUERPO + "al cofre y al banco."
				}, "clan kick " + nombreMiembro, () -> abrirMiembros(j, 0)));

		volverA(inv, holder, j -> abrirMiembros(j, 0));
		jugador.openInventory(inv);
	}

	private static TeamPlayer miembroPorNombre(Team clan, String nombre) {
		for (TeamPlayer miembro : clan.getMembers().getClone()) {
			if (miembro.getPlayer() != null && nombre.equals(miembro.getPlayer().getName())) {
				return miembro;
			}
		}
		return null;
	}

	// ----------------------------------------------------------------- invitar

	public static void abrirInvitar(Player jugador, int pagina) {
		Team clan = Team.getTeam(jugador);
		if (clan == null || !tieneMando(clan, jugador)) {
			abrirPortada(jugador);
			return;
		}
		// Solo los conectados: invitar por lista evita tener que escribir el nombre,
		// que es lo unico que un menu no puede pedir bien desde Bedrock.
		List<Player> candidatos = new ArrayList<>();
		for (Player otro : Bukkit.getOnlinePlayers()) {
			if (Team.getTeam(otro) == null) {
				candidatos.add(otro);
			}
		}

		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(candidatos.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Invitar", (actual + 1) + "/" + paginas);
		vaciarContenido(inv);

		int desde = actual * porPagina;
		for (int i = desde; i < candidatos.size() && i - desde < porPagina; i++) {
			String nombre = candidatos.get(i).getName();
			inv.setItem(CONTENIDO[i - desde], boton(Material.PLAYER_HEAD, MARCA + nombre,
					CUERPO + "Sin clan",
					"", ETIQUETA + "Clic para invitarlo"));
			holder.asignar(CONTENIDO[i - desde], j -> ejecutarYVolver(j, "clan invite " + nombre,
					() -> abrirInvitar(j, actual)));
		}
		if (candidatos.isEmpty()) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "No hay a quién invitar",
					CUERPO + "Todos los conectados ya tienen clan."));
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirInvitar(j, actual - 1));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirInvitar(j, actual + 1));
		}
		nota(inv, "A alguien desconectado", CUERPO + "/clan invite <jugador>");
		volverA(inv, holder, j -> abrirMiembros(j, 0));
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------- banco

	private static final int[] MONTOS = {100, 1000, 10000};

	public static void abrirBanco(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		boolean puedeSacar = tieneMando(clan, jugador);
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Banco", "$" + clan.getBalance());

		inv.setItem(CABECERA, boton(Material.GOLD_BLOCK, "Saldo del clan",
				CUERPO + "Hay " + MARCA + "$" + clan.getBalance() + CUERPO + " en el banco."));

		// Montos fijos: es la unica forma de mover dinero sin pedir que escriban.
		//
		// Las dos direcciones van en filas separadas y las dos de menor a mayor. Antes
		// compartian fila y los de sacar iban al reves, espejados hacia el centro: quedaba
		// simetrico y se leia mal, porque el boton de la misma columna era otro monto.
		for (int i = 0; i < MONTOS.length; i++) {
			int monto = MONTOS[i];
			int columna = 1 + i * 2;

			inv.setItem(FILA_A[columna], boton(Material.GOLD_NUGGET, "Poner $" + monto,
					CUERPO + "Saca " + MARCA + "$" + monto + CUERPO + " de lo tuyo",
					CUERPO + "y lo pone en el banco del clan."));
			holder.asignar(FILA_A[columna],
					j -> ejecutarYVolver(j, "clan deposit " + monto, () -> abrirBanco(j)));

			// Poner puede cualquiera; sacar es del mando. Se muestran igual y apagados, que
			// es mas claro que una fila que aparece y desaparece segun quien mire.
			inv.setItem(FILA_B[columna], boton(
					puedeSacar ? Material.GOLD_INGOT : Material.GRAY_DYE, "Sacar $" + monto,
					CUERPO + "Saca " + MARCA + "$" + monto + CUERPO + " del banco",
					CUERPO + "y te lo pone a ti.",
					puedeSacar ? "" : ETIQUETA + "Esto lo hace el líder o un colíder"));
			if (puedeSacar) {
				holder.asignar(FILA_B[columna],
						j -> ejecutarYVolver(j, "clan withdraw " + monto, () -> abrirBanco(j)));
			}
		}

		nota(inv, "Otro monto", CUERPO + "/clan deposit <monto>", CUERPO + "/clan withdraw <monto>");
		volverA(inv, holder, Menus::abrirPortada);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------- hitos

	/**
	 * El boton de la portada: cuanto lleva minado el clan y cual es el proximo escalon.
	 *
	 * <p>Muestra el proximo y no la lista entera porque en la portada solo hay lugar para
	 * cuatro renglones, y lo unico accionable es lo que sigue.
	 */
	private static ItemStack itemHitos(Team clan) {
		HitosManager manager = Main.plugin.getHitosManager();
		long minados = manager.getBloquesMinados(clan.getID());
		Hito proximo = manager.getProximo(clan.getID());

		if (proximo == null) {
			return boton(Material.NETHERITE_PICKAXE, "Hitos del clan",
					CUERPO + "Minado entre todos: " + MARCA + HitosCommand.numero(minados),
					"",
					MARCA + "Están todos alcanzados.");
		}
		return boton(Material.IRON_PICKAXE, "Hitos del clan",
				CUERPO + "Minado entre todos: " + MARCA + HitosCommand.numero(minados),
				"",
				CUERPO + "Sigue: " + MARCA + proximo.getNombre(),
				CUERPO + "Faltan " + MARCA + HitosCommand.numero(proximo.getUmbral() - minados)
						+ CUERPO + " bloques.");
	}

	/** Lo que le falta al clan para el cofre, para el lore del boton apagado. */
	private static String textoFaltanCofre(Team clan) {
		HitosManager manager = Main.plugin.getHitosManager();
		Hito hito = manager == null ? null : manager.hitoDe(Recompensa.COFRE);
		if (hito == null) {
			return "un hito del clan";
		}
		long faltan = Math.max(0, hito.getUmbral() - manager.getBloquesMinados(clan.getID()));
		return HitosCommand.numero(faltan) + " bloques más";
	}

	/**
	 * La escalera completa, un item por escalon.
	 *
	 * <p>Los alcanzados salen con el icono que les puso el config y los que faltan en gris,
	 * igual que los botones apagados del resto del menu: la forma de la escalera se lee de un
	 * vistazo sin tener que comparar numeros.
	 */
	public static void abrirHitos(Player jugador) {
		Team clan = Team.getTeam(jugador);
		HitosManager manager = Main.plugin.getHitosManager();
		// Se sale a la portada en vez de abrir una pantalla vacia. Pasa si el sistema se
		// apaga con alguien mirando el menu, y tambien lo cubre por si alguna pantalla futura
		// enlaza aca sin comprobarlo.
		if (clan == null || manager == null || !manager.isHabilitado()) {
			abrirPortada(jugador);
			return;
		}

		long minados = manager.getBloquesMinados(clan.getID());
		List<Hito> hitos = manager.getHitos();

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Hitos", HitosCommand.numero(minados) + " bloques");

		inv.setItem(CABECERA, boton(Material.IRON_PICKAXE, "Minado entre todos",
				CUERPO + "El clan lleva " + MARCA + HitosCommand.numero(minados)
						+ CUERPO + " bloques minados.",
				"",
				CUERPO + "Suma lo que mina cada miembro, y no",
				CUERPO + "se gasta ni se reinicia nunca."));

		// Hasta siete entran en una fila y se centran; de ocho en adelante se usan las dos.
		// Mas de catorce no entran en la pantalla: el comando las lista todas igual.
		int cabenArriba = FILA_A.length;
		boolean unaFila = hitos.size() <= cabenArriba;
		int visibles = Math.min(hitos.size(), cabenArriba + FILA_B.length);

		for (int i = 0; i < visibles; i++) {
			Hito hito = hitos.get(i);
			boolean listo = hito.alcanzado(minados);

			// La descripcion es opcional en el config: si falta, no se deja el renglon vacio,
			// que se ve como un lore roto y no como un espacio a proposito.
			List<String> lore = new ArrayList<>();
			if (!hito.getDescripcion().isEmpty()) {
				lore.add(CUERPO + hito.getDescripcion());
				lore.add("");
			}
			if (listo) {
				lore.add(MARCA + "Alcanzado.");
			} else {
				lore.add(CUERPO + "Faltan " + MARCA
						+ HitosCommand.numero(hito.getUmbral() - minados)
						+ CUERPO + " de " + MARCA + HitosCommand.numero(hito.getUmbral()));
				lore.add(ETIQUETA + "Llevan un "
						+ HitosCommand.porcentaje(hito.progreso(minados)));
			}

			inv.setItem(slotDeHito(i, unaFila, hitos.size()),
					boton(listo ? hito.getIcono() : Material.GRAY_DYE, hito.getNombre(),
							lore.toArray(new String[0])));
		}

		if (hitos.size() > visibles) {
			nota(inv, "Hay más", CUERPO + "No entran en la pantalla.",
					CUERPO + "Vélos todos con /clan hitos");
		} else {
			nota(inv, "Cómo se suma", CUERPO + "Minando piedra y menas.",
					CUERPO + "No cuenta romper lo que",
					CUERPO + "tú mismo acabas de poner.");
		}

		volverA(inv, holder, Menus::abrirPortada);
		jugador.openInventory(inv);
	}

	/**
	 * Donde cae cada escalon.
	 *
	 * <p>Con pocos hitos se dejan huecos entre medio y el bloque queda centrado en la fila;
	 * una fila de tres pegados a la izquierda se ve como si algo hubiera fallado.
	 */
	private static int slotDeHito(int indice, boolean unaFila, int total) {
		if (!unaFila) {
			return indice < FILA_A.length
					? FILA_A[indice]
					: FILA_B[indice - FILA_A.length];
		}
		// Hasta cuatro caben salteando una columna, que es lo que mejor se ve.
		int paso = total <= 4 ? 2 : 1;
		int ancho = (total - 1) * paso + 1;
		int inicio = (FILA_A.length - ancho) / 2;
		return FILA_A[inicio + indice * paso];
	}

	// ------------------------------------------------------------------- warps

	public static void abrirWarps(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Warps", limpiar(clan.getName()));
		vaciarContenido(inv);

		int i = 0;
		for (com.booksaw.betterTeams.Warp warp : clan.getWarps().getClone()) {
			if (i >= CONTENIDO.length) {
				break;
			}
			inv.setItem(CONTENIDO[i], boton(Material.LODESTONE, limpiar(warp.getName()),
					CUERPO + "Mundo: " + MARCA + (warp.getLocation() == null || warp.getLocation().getWorld() == null
							? "?" : warp.getLocation().getWorld().getName()),
					"", ETIQUETA + "Clic para viajar"));
			String nombre = warp.getName();
			holder.asignar(CONTENIDO[i], j -> comando(j, "clan warp " + nombre));
			i++;
		}
		if (i == 0) {
			inv.setItem(CONTENIDO[10], boton(Material.COBWEB, "Sin warps",
					CUERPO + "Un warp es un punto al que viaja",
					CUERPO + "cualquiera del clan."));
		}

		nota(inv, "Crear un warp",
				CUERPO + "Párate donde lo quieres y escribe",
				CUERPO + "/clan setwarp <nombre>");
		volverA(inv, holder, Menus::abrirPortada);
		jugador.openInventory(inv);
	}

	// ----------------------------------------------------------------- colores

	/**
	 * Los 16 de siempre, que es lo que acepta /clan color.
	 *
	 * <p>Columnas: codigo del comando · nombre visible · lana que lo representa · el
	 * caracter legacy, que es con lo que {@code Team.getColor()} dice cual esta puesto.
	 */
	private static final String[][] COLORES = {
			{"dark_red", "Rojo oscuro", "RED_WOOL", "4"}, {"red", "Rojo", "PINK_WOOL", "c"},
			{"gold", "Dorado", "ORANGE_WOOL", "6"}, {"yellow", "Amarillo", "YELLOW_WOOL", "e"},
			{"dark_green", "Verde oscuro", "GREEN_WOOL", "2"}, {"green", "Verde", "LIME_WOOL", "a"},
			{"aqua", "Celeste", "LIGHT_BLUE_WOOL", "b"}, {"dark_aqua", "Turquesa", "CYAN_WOOL", "3"},
			{"dark_blue", "Azul oscuro", "BLUE_WOOL", "1"}, {"blue", "Azul", "LIGHT_BLUE_WOOL", "9"},
			{"light_purple", "Rosa", "MAGENTA_WOOL", "d"}, {"dark_purple", "Violeta", "PURPLE_WOOL", "5"},
			{"white", "Blanco", "WHITE_WOOL", "f"}, {"gray", "Gris", "LIGHT_GRAY_WOOL", "7"},
			{"dark_gray", "Gris oscuro", "GRAY_WOOL", "8"}, {"black", "Negro", "BLACK_WOOL", "0"}
	};

	/**
	 * Elegir el icono del clan, paginado.
	 *
	 * <p>Es lo que hace que explorar clanes sirva: sin icono la lista es el mismo item
	 * repetido veinte veces y lo unico que distingue es leer los nombres de a uno.
	 *
	 * <p>El que ya esta puesto se marca en su propia casilla en vez de moverlo al principio:
	 * si la grilla se reordenara, el icono que buscas cambiaria de lugar cada vez que entras.
	 */
	public static void abrirIconos(Player jugador, int pagina) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		if (!tieneMando(clan, jugador)) {
			mensaje(jugador, ERROR + "El icono lo elige el líder o un colíder.");
			abrirAjustes(jugador);
			return;
		}
		List<Material> elegibles = Main.plugin.getClanIconos().getElegibles();
		Material puesto = Main.plugin.getClanIconos().getIcono(clan.getID());

		int porPagina = CONTENIDO.length;
		int paginas = Math.max(1, (int) Math.ceil(elegibles.size() / (double) porPagina));
		int actual = Math.max(0, Math.min(pagina, paginas - 1));

		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Icono del clan", (actual + 1) + "/" + paginas);
		vaciarContenido(inv);

		int desde = actual * porPagina;
		for (int i = desde; i < elegibles.size() && i - desde < porPagina; i++) {
			Material material = elegibles.get(i);
			int slot = CONTENIDO[i - desde];
			boolean esElPuesto = material == puesto;
			inv.setItem(slot, boton(material, nombreDeMaterial(material),
					esElPuesto ? MARCA + "Es el que tienes puesto" : ETIQUETA + "Clic para usarlo"));
			if (!esElPuesto) {
				holder.asignar(slot, j -> ejecutarYVolver(j, "clan icono " + material.name(),
						() -> abrirIconos(j, actual)));
			}
		}

		if (actual > 0) {
			inv.setItem(SLOT_ANTERIOR, boton(Material.SPECTRAL_ARROW, "Anterior"));
			holder.asignar(SLOT_ANTERIOR, j -> abrirIconos(j, actual - 1));
		}
		if (actual < paginas - 1) {
			inv.setItem(SLOT_SIGUIENTE, boton(Material.SPECTRAL_ARROW, "Siguiente"));
			holder.asignar(SLOT_SIGUIENTE, j -> abrirIconos(j, actual + 1));
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirAjustes);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	/** {@code NETHERITE_SWORD} se lee peor que "Netherite sword". */
	private static String nombreDeMaterial(Material material) {
		String texto = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
	}

	public static void abrirColores(Player jugador) {
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			abrirPortada(jugador);
			return;
		}
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, "Color del clan", limpiar(clan.getName()));
		vaciarContenido(inv);

		// El puesto se marca en su casilla, igual que en los iconos. Sin esto la pantalla
		// no decia cual era el color del clan, que es el dato que uno viene a mirar.
		String puesto = clan.getColor() == null ? null : String.valueOf(clan.getColor().getChar());
		for (int i = 0; i < COLORES.length && i < CONTENIDO.length; i++) {
			String codigo = COLORES[i][0];
			Material material = Material.matchMaterial(COLORES[i][2]);
			boolean esElPuesto = puesto != null && puesto.equals(COLORES[i][3]);
			inv.setItem(CONTENIDO[i], boton(material == null ? Material.WHITE_WOOL : material,
					COLORES[i][1],
					esElPuesto ? MARCA + "Es el que tienen puesto" : ETIQUETA + "Clic para usarlo"));
			if (!esElPuesto) {
				holder.asignar(CONTENIDO[i], j -> ejecutarYVolver(j, "clan color " + codigo,
						() -> abrirColores(j)));
			}
		}

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, Menus::abrirAjustes);
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------ confirmacion

	/**
	 * Pantalla de confirmacion para lo que no tiene vuelta atras.
	 *
	 * <p>Verde es SI y rojo es NO, que es como se lee en un menu de Minecraft. La
	 * version anterior los tenia al reves —verde para cancelar, rojo para la accion
	 * destructiva, que es la convencion de escritorio— y en el juego se leia
	 * invertido.
	 *
	 * <p>Y la posicion es siempre la misma: NO a la izquierda, SI a la derecha. Ver
	 * {@link #SLOT_NO} y {@link #SLOT_SI}.
	 */
	public static void abrirConfirmacion(Player jugador, String titulo, String[] aviso,
			String comando, Runnable volver) {
		MenuHolder holder = new MenuHolder();
		Inventory inv = crear(holder, titulo, "confirmar");

		List<String> lore = new ArrayList<>(Arrays.asList(aviso));
		lore.add("");
		lore.add(ERROR + "Esto no se puede deshacer.");
		inv.setItem(13, boton(Material.PAPER, ERROR + titulo, lore.toArray(new String[0])));

		inv.setItem(SLOT_NO, boton(Material.RED_DYE, ERROR + "No, volver",
				CUERPO + "No pasa nada."));
		holder.asignar(SLOT_NO, j -> volver.run());

		inv.setItem(SLOT_SI, boton(Material.LIME_DYE, "Si, hacerlo",
				CUERPO + "Se hace ahora, de una."));
		holder.asignar(SLOT_SI, j -> comando(j, comando));

		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, j -> volver.run());
		cerrar(inv, holder);
		jugador.openInventory(inv);
	}

	// ------------------------------------------------------------------- comun

	private static Inventory crear(MenuHolder holder, String titulo, String sub) {
		// Sin color ni negrita: el cliente dibuja el titulo de un cofre en gris
		// oscuro salvo que el texto traiga color propio. Se lo deja hacer, asi el
		// menu se ve como uno de vanilla. Vale para todos los forks.
		String nombre = col(titulo + (sub == null ? "" : "  " + sub));
		Inventory inv = Bukkit.createInventory(holder, TAM, nombre);
		holder.setInventario(inv);

		// Dos tonos: el marco mas oscuro y el interior mas claro. Se lee donde
		// termina el marco y donde empieza el contenido.
		ItemStack borde = boton(Material.GRAY_STAINED_GLASS_PANE, " ");
		ItemStack fondo = boton(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
		for (int i = 0; i < TAM; i++) {
			inv.setItem(i, borde);
		}
		for (int slot : CONTENIDO) {
			inv.setItem(slot, fondo);
		}
		return inv;
	}

	/** Lore de varias piezas, para no armar listas a mano en cada boton. */
	private static String[] unir(String[] base, String... extra) {
		List<String> lineas = new ArrayList<>(Arrays.asList(base));
		lineas.addAll(Arrays.asList(extra));
		return lineas.toArray(new String[0]);
	}

	/**
	 * Nota al pie de una pantalla, en el marco y al lado de Volver.
	 *
	 * <p>Es donde va lo que el menu no puede hacer y hay que escribir a mano. <b>Antes eso
	 * vivia en el lore del boton de Volver</b>, o sea escondido en el unico boton que nadie
	 * lee: la gente no descubria que se podia buscar por nombre ni crear un warp.
	 */
	private static void nota(Inventory inv, String titulo, String... lineas) {
		inv.setItem(SLOT_NOTA, boton(Material.PAPER, ETIQUETA + titulo, lineas));
	}

	private static void vaciarContenido(Inventory inv) {
		for (int slot : CONTENIDO) {
			inv.setItem(slot, null);
		}
	}

	/** Pone el boton de volver, su destino, y el de cerrar. */
	private static void volverA(Inventory inv, MenuHolder holder, Consumer<Player> destino) {
		inv.setItem(SLOT_VOLVER, boton(Material.ARROW, "Volver"));
		holder.asignar(SLOT_VOLVER, destino);
		cerrar(inv, holder);
	}

	/**
	 * Sin decimales cuando el monto es redondo, y con {@link Locale#ROOT} siempre.
	 *
	 * <p>Este texto termina adentro de un comando, y el separador decimal del
	 * sistema —coma, en espaniol— no lo acepta el parser.
	 */
	/**
	 * Tiempo restante en la unidad que se entiende sola.
	 *
	 * <p>Los duelos duran de 30 minutos a 7 dias: decir "10079 minutos" no informa nada, y
	 * era lo que hacia la pantalla antes de los presets en dias.
	 */
	private static String tiempoCorto(long segundos) {
		long minutos = segundos / 60;
		if (minutos < 60) {
			return Math.max(1, minutos) + " min";
		}
		long horas = minutos / 60;
		if (horas < 48) {
			return horas + " h";
		}
		return (horas / 24) + " dias";
	}

	private static String fmt(double monto) {
		return monto == Math.floor(monto) && !Double.isInfinite(monto)
				? String.valueOf((long) monto)
				: String.format(Locale.ROOT, "%.2f", monto);
	}

	/** El de cerrar no va en rojo: cerrar el menu no le hace nada a nadie. */
	private static void cerrar(Inventory inv, MenuHolder holder) {
		inv.setItem(SLOT_CERRAR, boton(Material.BARRIER, ETIQUETA + "Cerrar"));
		holder.asignar(SLOT_CERRAR, Player::closeInventory);
	}

	private static ItemStack boton(Material material, String nombre, String... lore) {
		return boton(new ItemStack(material), nombre, lore);
	}

	/** Hash de la textura de la cabeza de info (una "i"), usada en cabeceras que no accionan nada. */
	private static final String HASH_CABEZA_INFO =
			"2b8d0de4ed3a220007a5fd97d2969d02d21075dc55d6f5e4c21d3be6ef4ef98";
	private static PlayerProfile perfilCabezaInfo;

	/** Cabeza de info, cacheada: el perfil se arma una sola vez, no en cada apertura del menu. */
	private static ItemStack cabezaInfo() {
		ItemStack pila = new ItemStack(Material.PLAYER_HEAD);
		if (perfilCabezaInfo == null) {
			try {
				UUID id = UUID.nameUUIDFromBytes("luniatic-cabeza-info".getBytes());
				PlayerProfile perfil = Bukkit.createPlayerProfile(id, null);
				URL url = URI.create("https://textures.minecraft.net/texture/" + HASH_CABEZA_INFO).toURL();
				perfil.getTextures().setSkin(url);
				perfilCabezaInfo = perfil;
			} catch (Exception e) {
				Main.plugin.getLogger().warning("[clanes] no se pudo armar la cabeza de info: " + e.getMessage());
				return pila;
			}
		}
		if (pila.getItemMeta() instanceof SkullMeta meta) {
			meta.setOwnerProfile(perfilCabezaInfo);
			pila.setItemMeta(meta);
		}
		return pila;
	}

	/** Para los botones que llegan con la pila ya armada, como las cabezas del ranking. */
	private static ItemStack boton(ItemStack pila, String nombre, String... lore) {
		ItemMeta meta = pila.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(col(nombre.startsWith("&") ? nombre : MARCA + nombre));
			// Los renglones vacios del final se descartan: media docena de botones arman su
			// ultima linea con un ternario que a veces devuelve "", y eso dejaba el globo
			// del item con un hueco abajo sin que se viera de donde salia.
			int fin = lore.length;
			while (fin > 0 && lore[fin - 1].isEmpty()) {
				fin--;
			}
			if (fin > 0) {
				List<String> lineas = new ArrayList<>();
				for (int i = 0; i < fin; i++) {
					String linea = lore[i];
					lineas.add(col(linea.isEmpty() || linea.startsWith("&") ? linea : CUERPO + linea));
				}
				meta.setLore(lineas);
			}
			pila.setItemMeta(meta);
		}
		return pila;
	}

	/**
	 * Convierte a color de verdad. Son tres pasos y hacen falta los tres.
	 *
	 * <p>1) {@code toAdventure} pasa los codigos {@code &#RRGGBB} y {@code &c} a
	 * etiquetas MiniMessage. 2) el formateador las convierte en componente, que es
	 * lo que tambien resuelve el MiniMessage que ya traen los nombres de clan.
	 * 3) se serializa a legacy con {@code hexColors()}.
	 *
	 * <p>🔑 <b>El tercer paso no puede usar {@code LegacyTextUtils.parseAllAdventure}</b>:
	 * ese serializa sin hex, asi que aplasta cada color al legacy mas parecido y toda
	 * la paleta termina saliendo del mismo azul. Comprobado.
	 */
	static String col(String texto) {
		return Texto.col(texto);
	}

	/** Saca el color de un texto, para usarlo dentro de otro que ya tiene color. */
	private static String limpiar(String texto) {
		return Texto.limpiar(texto);
	}

	private static String rango(PlayerRank rango) {
		if (rango == PlayerRank.OWNER) {
			return "Duenio";
		}
		if (rango == PlayerRank.ADMIN) {
			return "Mando";
		}
		return "Miembro";
	}

	/** Cierra el menu y ejecuta. Para lo que saca al jugador de la pantalla. */
	private static void comando(Player jugador, String comando) {
		jugador.closeInventory();
		if (seguro(jugador, comando)) {
			jugador.performCommand(comando);
		}
	}

	/**
	 * Que el comando armado sea un solo comando.
	 *
	 * <p>Los botones interpolan nombres de clan y de jugador en una linea de
	 * comando. Hoy no hay riesgo porque {@code allowedChars} deja los nombres en
	 * alfanumerico puro, pero eso es una config que alguien puede vaciar —su propio
	 * comentario dice que en blanco permite todo—, y ahi un nombre con espacios
	 * correria los argumentos: un clan llamado "x 999999" convertiria una apuesta de
	 * 1 en otra cosa. Se valida al armar, no se confia en la config.
	 */
	private static boolean seguro(Player jugador, String comando) {
		if (comando.indexOf('\n') >= 0 || comando.indexOf('\r') >= 0) {
			mensaje(jugador, ERROR + "Ese nombre tiene caracteres que no se pueden usar.");
			return false;
		}
		return true;
	}

	/** Un argumento que se interpola en un comando: sin espacios ni saltos. */
	private static boolean argumentoSeguro(String valor) {
		if (valor == null || valor.isEmpty()) {
			return false;
		}
		for (int i = 0; i < valor.length(); i++) {
			if (Character.isWhitespace(valor.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Ejecuta sin cerrar y vuelve a dibujar la pantalla, para que se vea el efecto.
	 *
	 * <p>Se delega en el comando a proposito: permisos, limites, baneos, costos y
	 * cooldowns siguen valiendo, y el jugador recibe el mensaje de error del propio
	 * plugin si algo no se puede.
	 */
	private static void ejecutarYVolver(Player jugador, String comando, Runnable volverADibujar) {
		if (seguro(jugador, comando)) {
			jugador.performCommand(comando);
		}
		volverADibujar.run();
	}

	/** Si puede administrar el clan. La palabra final la tiene igual el comando. */
	private static boolean tieneMando(Team clan, Player jugador) {
		TeamPlayer yo = clan.getTeamPlayer(jugador);
		return yo != null && yo.getRank() != PlayerRank.DEFAULT;
	}

	/** {@code Clanes »} y no {@code [Clanes]}: es la marca de la casa para todo el servidor. */
	private static void mensaje(Player jugador, String texto) {
		jugador.sendMessage(col(ETIQUETA + "Clanes » " + CUERPO + texto));
	}
}
