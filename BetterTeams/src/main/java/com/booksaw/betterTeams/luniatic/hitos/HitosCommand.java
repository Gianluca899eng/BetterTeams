package com.booksaw.betterTeams.luniatic.hitos;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.luniatic.Texto;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /clan hitos - el progreso del clan hacia cada hito.
 *
 * <p>El camino normal es el menu, pero el comando existe igual por el mismo motivo que
 * {@code /clan icono}: el boton del menu dispara un comando que ya existe, asi que permisos y
 * rangos siguen valiendo en un solo lugar.
 *
 * <p><b>Reusa el nodo {@code info}</b>, que upstream declara con {@code default: true}. Un
 * nodo propio nace sin declarar en el {@code plugin.yml} y para Bukkit eso es OP-only: es el
 * pozo que dejo {@code /clan duelo} inservible. Y encaja, porque esto es informacion del clan.
 *
 * <p>Los textos van escritos aca y no en {@code es.yml}, igual que en el menu: son una
 * superficie propia de Luniatic, y meterlos en los archivos de idioma agrega claves a dos
 * archivos que hay que mantener sincronizados con cada version de upstream, sin ganar nada.
 */
public class HitosCommand extends TeamSubCommand {

	private static final String MARCA = "&#9235FF";
	private static final String CUERPO = "&#E4D9FF";
	private static final String ETIQUETA = "&#7162FF";

	private final HitosManager manager;

	public HitosCommand(HitosManager manager) {
		this.manager = manager;
	}

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {
		Player jugador = player.getPlayer().getPlayer();
		if (jugador == null) {
			return new CommandResponse(false);
		}
		for (String linea : construirDetalle(manager, team)) {
			jugador.sendMessage(linea);
		}
		return new CommandResponse(true);
	}

	/**
	 * Arma el detalle completo. Estatico y publico porque el menu muestra lo mismo y no tiene
	 * sentido escribir dos veces un texto que despues se desincroniza.
	 */
	public static List<String> construirDetalle(HitosManager manager, Team team) {
		long minados = manager.getBloquesMinados(team.getID());

		List<String> lineas = new java.util.ArrayList<>();
		lineas.add("");
		lineas.add(Texto.col(ETIQUETA + "Clanes » " + MARCA + "Hitos del clan"));
		lineas.add(Texto.col(CUERPO + "Minado entre todos: " + MARCA + numero(minados)
				+ CUERPO + " bloques"));
		lineas.add("");

		for (Hito hito : manager.getHitos()) {
			boolean listo = hito.alcanzado(minados);
			// Marca de estado, no decoracion: es el unico dato de la linea que se lee de un
			// vistazo sin comparar dos numeros.
			String marca = listo ? "&#9235FF✔ " : ETIQUETA + "✖ ";
			lineas.add(Texto.col("  " + marca + (listo ? MARCA : CUERPO) + hito.getNombre()));

			// La descripcion va SIEMPRE, y en especial cuando el hito falta: es cuando mas
			// importa. Mostrarla solo en los alcanzados dejaba al jugador viendo "faltan
			// 99.996" sin ninguna forma de saber que estaba por conseguir, que es justo lo
			// que tiene que motivarlo. Ademas el menu ya las mostraba en los dos estados, o
			// sea que las dos superficies decian cosas distintas.
			if (!hito.getDescripcion().isEmpty()) {
				lineas.add(Texto.col("     " + CUERPO + hito.getDescripcion()));
			}
			if (!listo) {
				long faltan = hito.getUmbral() - minados;
				lineas.add(Texto.col("     " + ETIQUETA + "Faltan " + MARCA + numero(faltan)
						+ ETIQUETA + " de " + MARCA + numero(hito.getUmbral())
						+ ETIQUETA + "  (" + porcentaje(hito.progreso(minados)) + ")"));
			}
		}

		lineas.add("");
		return lineas;
	}

	/**
	 * Separador de miles fijo, sin depender del idioma de la maquina.
	 *
	 * <p>{@code String.format("%,d")} usa el locale por defecto de la JVM, que en un host
	 * cualquiera puede ser el ingles y escribir {@code 100,000}: en castellano eso se lee como
	 * cien coma cero, que es justo lo contrario del numero.
	 */
	public static String numero(long valor) {
		String crudo = Long.toString(Math.abs(valor));
		StringBuilder salida = new StringBuilder();
		for (int i = 0; i < crudo.length(); i++) {
			if (i > 0 && (crudo.length() - i) % 3 == 0) {
				salida.append('.');
			}
			salida.append(crudo.charAt(i));
		}
		return (valor < 0 ? "-" : "") + salida;
	}

	/**
	 * El porcentaje, redondeado hacia abajo.
	 *
	 * <p>Hacia abajo y no al mas cercano: con redondeo normal, a falta de un puñado de bloques
	 * ya diria 100 % y el jugador leeria que termino cuando todavia le falta.
	 */
	public static String porcentaje(double fraccion) {
		return ((int) Math.floor(fraccion * 100)) + "%";
	}

	@Override
	public String getCommand() {
		return "hitos";
	}

	@Override
	public String getNode() {
		return "info";
	}

	@Override
	public String getHelp() {
		return "Que le falta a tu clan para cada hito";
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

	/** El progreso es del clan entero: lo mira cualquiera, no solo quien manda. */
	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.DEFAULT;
	}

	@Override
	public boolean runAsync(String[] args) {
		return false;
	}
}
