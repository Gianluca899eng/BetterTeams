package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Duelos pactados entre clanes.
 *
 * <p>Reglas de diseno, para que no se pierdan si alguien toca esto despues:
 * <ul>
 * <li>Los dos clanes consienten. Uno desafia con un monto y el otro acepta ese
 * mismo monto; sin la segunda mitad no pasa nada.
 * <li>Se apuesta dinero del banco del clan, nunca items: con los dupes abiertos
 * una apuesta en items la paga gratis quien tiene granja de dupeo.
 * <li>El pozo lo retiene el sistema desde que arranca. No depende de que el
 * perdedor cumpla su palabra.
 * <li>No se cuentan muertes ni se reparte puntaje. Se resuelve por rendicion o
 * por tiempo cumplido.
 * <li>No otorga ningun derecho sobre claims ajenos. Es PvP entre personas.
 * </ul>
 */
public class DueloManager {

	private final boolean habilitado;
	private final long duracionMillis;
	private final long esperaMillis;
	private final double apuestaMinima;
	private final double apuestaMaxima;
	private final boolean pisaPvpIndividual;
	private final boolean avisoGlobal;
	private final int objetivoBajas;
	private final boolean barraActiva;
	private final String avisoTab;
	private final boolean arrastraAliados;
	private DueloBossBar barra;

	/**
	 * Desafios sin aceptar: clan retado -> (clan retador -> desafio).
	 *
	 * <p>Son varios por clan a proposito. Con uno solo, el segundo que te desafiaba
	 * pisaba al primero sin que nadie se enterara.
	 */
	private final Map<UUID, Map<UUID, Desafio>> desafios = new HashMap<>();
	/** Duelos en curso, indexados por CADA participante (dos entradas por duelo). */
	private final Map<UUID, Duelo> enCurso = new HashMap<>();

	public DueloManager(ConfigurationSection seccion) {
		if (seccion == null) {
			habilitado = false;
			duracionMillis = 0;
			esperaMillis = 0;
			apuestaMinima = 0;
			apuestaMaxima = 0;
			pisaPvpIndividual = false;
			avisoGlobal = false;
			objetivoBajas = 0;
			barraActiva = false;
			avisoTab = "";
			arrastraAliados = false;
			return;
		}
		habilitado = seccion.getBoolean("enabled", false);
		duracionMillis = Math.max(1, seccion.getInt("duracion-minutos", 30)) * 60_000L;
		esperaMillis = Math.max(10, seccion.getInt("espera-aceptacion-segundos", 120)) * 1000L;
		apuestaMinima = Math.max(0, seccion.getDouble("apuesta.minima", 0));
		apuestaMaxima = Math.max(apuestaMinima, seccion.getDouble("apuesta.maxima", 100000));
		pisaPvpIndividual = seccion.getBoolean("pisa-pvp-individual", false);
		avisoGlobal = seccion.getBoolean("aviso-global", false);
		objetivoBajas = Math.max(1, seccion.getInt("objetivo-bajas", 10));
		barraActiva = seccion.getBoolean("barra", true);
		avisoTab = seccion.getString("aviso-tab", "&#FF4554PvP forzado por duelo");
		arrastraAliados = seccion.getBoolean("arrastra-aliados", true);
	}

	public boolean isArrastraAliados() {
		return arrastraAliados;
	}

	public String getAvisoTab() {
		return avisoTab;
	}

	public int getObjetivoBajas() {
		return objetivoBajas;
	}

	public boolean isBarraActiva() {
		return barraActiva;
	}

	public void setBarra(DueloBossBar barra) {
		this.barra = barra;
	}

	/** Los clanes con un duelo en curso, sin repetir. */
	public List<Team> getClanesEnDuelo() {
		List<Team> clanes = new ArrayList<>();
		for (UUID id : enCurso.keySet()) {
			Team clan = Team.getTeam(id);
			if (clan != null) {
				clanes.add(clan);
			}
		}
		return clanes;
	}

	private void dibujar(Team clan) {
		if (barra != null) {
			barra.refrescar(clan);
		}
	}

	private void borrarBarra(Team clan) {
		if (barra != null) {
			barra.quitar(clan);
		}
	}

	/** Redibuja a los dos bandos enteros, aliados incluidos. */
	private void dibujarTodos(Duelo duelo) {
		for (UUID id : duelo.todosLosClanes()) {
			dibujar(Team.getTeam(id));
		}
	}

	public boolean isHabilitado() {
		return habilitado;
	}

	public boolean isPisaPvpIndividual() {
		return pisaPvpIndividual;
	}

	/**
	 * Si hay algun duelo en curso.
	 *
	 * <p>Es el primer chequeo de los listeners de dano y de muerte, y por eso es un
	 * {@code isEmpty()} sobre un mapa: resolver el clan de un jugador recorre todos
	 * los clanes, asi que preguntarlo antes de saber si hace falta es pagar por nada.
	 * Sin duelos en curso —o sea, casi siempre— el listener cuesta una comparacion.
	 */
	public boolean hayDuelos() {
		return !enCurso.isEmpty();
	}

	public Duelo getDuelo(Team clan) {
		return clan == null ? null : enCurso.get(clan.getID());
	}

	/** Todos los desafios vigentes que le mandaron a este clan. */
	public List<Desafio> getDesafiosRecibidos(Team clan) {
		if (clan == null) {
			return new ArrayList<>();
		}
		Map<UUID, Desafio> recibidos = desafios.get(clan.getID());
		if (recibidos == null) {
			return new ArrayList<>();
		}
		long ahora = System.currentTimeMillis();
		recibidos.values().removeIf(d -> d.vencio(ahora));
		return new ArrayList<>(recibidos.values());
	}

	/** Rechaza un desafio. El que lo mando se entera. */
	public Resultado rechazar(Team retado, Team retador) {
		Map<UUID, Desafio> recibidos = desafios.get(retado.getID());
		if (recibidos == null || recibidos.remove(retador.getID()) == null) {
			return Resultado.error("duelo.sin_desafio");
		}
		avisar(retador, "duelo.rechazado", retado.getName());
		return Resultado.ok("duelo.rechazaste", retador.getName());
	}

	/** Si el clan esta libre para pactar un duelo. */
	public boolean estaLibre(Team clan) {
		return clan != null && !enCurso.containsKey(clan.getID());
	}

	/** True solo si estan en el mismo duelo y en bandos opuestos. */
	public boolean sonRivales(Team uno, Team otro) {
		if (uno == null || otro == null || uno.getID().equals(otro.getID())) {
			return false;
		}
		Duelo duelo = enCurso.get(uno.getID());
		// Aliados del mismo bando NO son rivales: no se pegan entre ellos.
		return duelo != null && duelo.getBandoRival(uno.getID()).contains(otro.getID());
	}

	/**
	 * Desafia a otro clan, o acepta su desafio si ya habia uno por el mismo monto.
	 * Es el mismo flujo que usan las alianzas.
	 */
	public Resultado desafiar(Team retador, Team retado, double apuesta) {
		if (!habilitado) {
			return Resultado.error("duelo.apagado");
		}
		if (retador.getID().equals(retado.getID())) {
			return Resultado.error("duelo.uno_mismo");
		}
		if (enCurso.containsKey(retador.getID())) {
			return Resultado.error("duelo.ya_en_duelo");
		}
		if (enCurso.containsKey(retado.getID())) {
			return Resultado.error("duelo.rival_ocupado");
		}
		if (apuesta < apuestaMinima || apuesta > apuestaMaxima) {
			return Resultado.error("duelo.apuesta_fuera_de_rango", fmt(apuestaMinima), fmt(apuestaMaxima));
		}
		if (apuesta > 0 && Main.econ == null) {
			return Resultado.error("duelo.sin_economia");
		}

		// Si ese clan ya me habia desafiado, esto es una aceptacion.
		Map<UUID, Desafio> misRecibidos = desafios.get(retador.getID());
		Desafio recibido = misRecibidos == null ? null : misRecibidos.get(retado.getID());
		if (recibido != null && recibido.vencio(System.currentTimeMillis())) {
			misRecibidos.remove(retado.getID());
			recibido = null;
		}

		if (recibido != null) {
			if (recibido.getApuesta() != apuesta) {
				// Los dos tienen que estar de acuerdo con lo que se juega.
				return Resultado.error("duelo.monto_distinto", fmt(recibido.getApuesta()));
			}
			// 🔑 El desafio se consume SOLO si el duelo arranco. Antes se borraba
			// antes de intentar, asi que aceptar sin plata en el banco te dejaba sin
			// invitacion y sin duelo: habia que pedirle al otro que la mandara de
			// nuevo por un error que no cambiaba nada del acuerdo.
			Resultado resultado = arrancar(retado, retador, apuesta);
			if (resultado.exito) {
				misRecibidos.remove(retado.getID());
			}
			return resultado;
		}

		Map<UUID, Desafio> susRecibidos = desafios.computeIfAbsent(retado.getID(), id -> new HashMap<>());
		if (susRecibidos.containsKey(retador.getID())) {
			return Resultado.error("duelo.ya_desafiado");
		}
		if (apuesta > 0 && retador.getMoney() < apuesta) {
			return Resultado.error("duelo.sin_fondos", fmt(apuesta));
		}

		susRecibidos.put(retador.getID(),
				new Desafio(retador.getID(), apuesta, System.currentTimeMillis() + esperaMillis));
		avisar(retado, "duelo.recibido", retador.getName(), fmt(apuesta));
		// Y en pantalla al mando, que es el unico que puede responderlo: un mensaje
		// de chat se pierde entre lo demas y el desafio se queda esperando.
		avisarEnPantallaAlMando(retado, "duelo.recibido_titulo", retador.getName(), fmt(apuesta));
		return Resultado.ok("duelo.enviado", retado.getName(), fmt(apuesta));
	}

	private Resultado arrancar(Team unClan, Team otroClan, double apuesta) {
		if (apuesta > 0) {
			if (unClan.getMoney() < apuesta) {
				return Resultado.error("duelo.rival_sin_fondos", unClan.getName());
			}
			if (otroClan.getMoney() < apuesta) {
				return Resultado.error("duelo.sin_fondos", fmt(apuesta));
			}
			// El pozo sale de los dos bancos ahora: el sistema lo retiene, no la palabra.
			unClan.setMoney(unClan.getMoney() - apuesta);
			otroClan.setMoney(otroClan.getMoney() - apuesta);
		}

		// Los aliados se congelan al arrancar: aliarse en medio del duelo no trae
		// refuerzos, y desaliarse no saca a nadie del lio en el que ya estaba.
		Duelo duelo = new Duelo(unClan.getID(), aliadosLibres(unClan),
				otroClan.getID(), aliadosLibres(otroClan),
				apuesta, System.currentTimeMillis() + duracionMillis);
		for (UUID id : duelo.todosLosClanes()) {
			enCurso.put(id, duelo);
		}

		long minutos = duracionMillis / 60_000L;
		avisar(unClan, "duelo.arranco", otroClan.getName(), fmt(duelo.getPozo()), String.valueOf(minutos));
		avisar(otroClan, "duelo.arranco", unClan.getName(), fmt(duelo.getPozo()), String.valueOf(minutos));

		// Al aliado hay que decirle que quedo adentro y por que: el no acepto nada.
		for (UUID id : duelo.todosLosClanes()) {
			if (duelo.esPrincipal(id)) {
				continue;
			}
			Team aliado = Team.getTeam(id);
			if (aliado != null) {
				Team suPrincipal = Team.getTeam(duelo.getPrincipalDe(id));
				Team suRival = Team.getTeam(duelo.rivalDe(id));
				avisar(aliado, "duelo.arrastrado",
						suPrincipal == null ? "?" : suPrincipal.getName(),
						suRival == null ? "?" : suRival.getName(),
						String.valueOf(minutos));
			}
		}

		if (avisoGlobal) {
			MessageManager.sendMessage(new ArrayList<>(Main.plugin.getServer().getOnlinePlayers()),
					"duelo.aviso_global", unClan.getName(), otroClan.getName());
		}

		dibujarTodos(duelo);
		return Resultado.ok("duelo.arranco_confirmacion", unClan.getName());
	}

	/**
	 * El clan se rinde: pierde su parte y el rival se lleva el pozo entero. Es la
	 * unica salida anticipada; no se puede cancelar un duelo empezado, o alcanzaria
	 * con apagarlo cuando vas perdiendo.
	 */
	public Resultado rendirse(Team clan) {
		Duelo duelo = enCurso.get(clan.getID());
		if (duelo == null) {
			return Resultado.error("duelo.sin_duelo");
		}
		// Un aliado no puede rendir un duelo que no pacto ni pago.
		if (!duelo.esPrincipal(clan.getID())) {
			return Resultado.error("duelo.no_sos_principal");
		}
		Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
		cerrar(duelo);

		if (rival != null) {
			pagar(rival, duelo.getPozo());
			avisar(rival, "duelo.gano", clan.getName(), fmt(duelo.getPozo()));
			avisar(clan, "duelo.perdio", rival.getName(), fmt(duelo.getApuesta()));
		} else {
			// El rival dejo de existir: se le devuelve lo suyo al que sigue en pie.
			pagar(clan, duelo.getPozo());
			avisar(clan, "duelo.rival_desaparecio");
		}
		return Resultado.ok("duelo.rendido");
	}

	/**
	 * Registra que un jugador de un clan cayo a manos del clan rival.
	 *
	 * <p>Es lo que le da sentido al duelo: sin esto la unica salida era rendirse
	 * perdiendo el pozo o aguantar el reloj para recuperarlo, asi que esconderse
	 * siempre convenia.
	 */
	public void registrarBaja(Team clanCaido, Team clanAtacante) {
		if (!habilitado || !sonRivales(clanCaido, clanAtacante)) {
			return;
		}
		Duelo duelo = enCurso.get(clanCaido.getID());

		// Solo cuentan las caidas de los dos clanes que pactaron. Un aliado que cae
		// no mueve el marcador: si contara, sumar aliados te haria mas facil perder
		// y un aliado podria tirar el duelo a proposito.
		if (!duelo.esPrincipal(clanCaido.getID())) {
			return;
		}

		int bajas = duelo.sumarBaja(clanCaido.getID());
		Team principalRival = Team.getTeam(duelo.rivalDe(clanCaido.getID()));

		if (bajas >= objetivoBajas) {
			cerrar(duelo);
			if (principalRival != null) {
				pagar(principalRival, duelo.getPozo());
				avisar(principalRival, "duelo.gano", clanCaido.getName(), fmt(duelo.getPozo()));
			}
			avisar(clanCaido, "duelo.perdio",
					principalRival == null ? "?" : principalRival.getName(), fmt(duelo.getApuesta()));
			return;
		}

		String marcador = bajas + "/" + objetivoBajas;
		// El marcador lo ven los dos bandos enteros, aliados incluidos: estan peleando.
		for (UUID id : duelo.getBando(clanCaido.getID())) {
			Team clan = Team.getTeam(id);
			if (clan != null) {
				avisar(clan, "duelo.marcador_propio", marcador, clanAtacante.getName());
			}
		}
		for (UUID id : duelo.getBandoRival(clanCaido.getID())) {
			Team clan = Team.getTeam(id);
			if (clan != null) {
				avisar(clan, "duelo.marcador_rival", clanCaido.getName(), marcador);
			}
		}
		dibujarTodos(duelo);
	}

	/** Revisa vencimientos. La llama una tarea repetitiva, no cada evento. */
	public void revisar() {
		long ahora = System.currentTimeMillis();

		for (Map<UUID, Desafio> recibidos : desafios.values()) {
			recibidos.values().removeIf(d -> d.vencio(ahora));
		}
		desafios.values().removeIf(Map::isEmpty);

		List<Duelo> terminados = new ArrayList<>();
		for (Duelo duelo : unicos(enCurso.values())) {
			if (duelo.vencio(ahora)) {
				terminados.add(duelo);
			}
		}
		for (Duelo duelo : terminados) {
			cerrar(duelo);
			resolverPorTiempo(duelo);
		}

		// Lo unico que mueve el reloj de la barra. No hace falta mas seguido.
		if (barra != null) {
			barra.refrescarTodos();
		}
	}

	/**
	 * Se cumplio el tiempo: gana el que tenga MENOS bajas. Empate exacto, cada uno
	 * recupera lo suyo.
	 */
	private void resolverPorTiempo(Duelo duelo) {
		UUID ganador = duelo.getGanandoAhora();
		if (ganador == null) {
			devolver(duelo, "duelo.empate");
			return;
		}
		Team ganadorClan = Team.getTeam(ganador);
		Team perdedorClan = Team.getTeam(duelo.rivalDe(ganador));
		if (ganadorClan == null) {
			devolver(duelo, "duelo.empate");
			return;
		}
		pagar(ganadorClan, duelo.getPozo());
		avisar(ganadorClan, "duelo.gano_por_tiempo",
				String.valueOf(duelo.getBajas(ganador)),
				String.valueOf(duelo.getBajas(duelo.rivalDe(ganador))),
				fmt(duelo.getPozo()));
		if (perdedorClan != null) {
			avisar(perdedorClan, "duelo.perdio_por_tiempo",
					String.valueOf(duelo.getBajas(duelo.rivalDe(ganador))),
					String.valueOf(duelo.getBajas(ganador)),
					fmt(duelo.getApuesta()));
		}
	}

	/** Se llama al apagar el plugin: nadie se queda sin su parte del pozo. */
	public void devolverTodo() {
		for (Duelo duelo : unicos(enCurso.values())) {
			cerrar(duelo);
			devolver(duelo, "duelo.cancelado_apagado");
		}
		desafios.clear();
		if (barra != null) {
			barra.quitarTodas();
		}
	}

	/**
	 * Cada duelo una sola vez.
	 *
	 * <p>🔑 <b>El mapa de duelos en curso guarda el MISMO objeto bajo la clave de
	 * cada clan participante</b> —los dos principales mas cada aliado—, asi que
	 * recorrer sus valores lo devuelve dos veces o mas. Sin esto, cada vuelta de mas
	 * paga la apuesta otra vez: al apagar el servidor los clanes cobraban su parte
	 * una vez por participante. Se veia como un mensaje repetido y era una impresora
	 * de dinero.
	 *
	 * <p>{@link Duelo} no implementa {@code equals}, asi que el conjunto deduplica
	 * por identidad, que es exactamente lo que hace falta.
	 */
	static Collection<Duelo> unicos(Collection<Duelo> duelos) {
		return new LinkedHashSet<>(duelos);
	}

	private void devolver(Duelo duelo, String referencia) {
		Team a = Team.getTeam(duelo.getClanA());
		Team b = Team.getTeam(duelo.getClanB());
		if (a != null) {
			pagar(a, duelo.getApuesta());
			avisar(a, referencia);
		}
		if (b != null) {
			pagar(b, duelo.getApuesta());
			avisar(b, referencia);
		}
	}

	/**
	 * Los aliados que van a entrar con el clan. Solo aliados directos: si entraran
	 * los aliados de los aliados, una pelea de dos clanes se lleva puesto medio
	 * servidor. Se saltea a los que ya esten en otro duelo.
	 */
	private Set<UUID> aliadosLibres(Team clan) {
		Set<UUID> lista = new LinkedHashSet<>();
		if (!arrastraAliados) {
			return lista;
		}
		for (UUID id : clan.getAllies().getClone()) {
			if (!enCurso.containsKey(id) && Team.getTeam(id) != null) {
				lista.add(id);
			}
		}
		return lista;
	}

	/** Punto unico de cierre, asi ninguna salida se olvida de sacar la barra. */
	private void cerrar(Duelo duelo) {
		for (UUID id : duelo.todosLosClanes()) {
			enCurso.remove(id);
			borrarBarra(Team.getTeam(id));
		}
	}

	private void pagar(Team clan, double monto) {
		if (monto <= 0) {
			return;
		}
		double tope = clan.getMaxMoney();
		double nuevo = clan.getMoney() + monto;
		if (tope >= 0 && nuevo > tope) {
			nuevo = tope;
		}
		clan.setMoney(nuevo);
	}

	private void avisar(Team clan, String referencia, Object... argumentos) {
		MessageManager.sendMessage(clan.getMembers().getOnlinePlayers(), referencia, argumentos);
	}

	/**
	 * Titulo en pantalla, solo para lider y colider.
	 *
	 * <p>Van solo ellos porque son los unicos que pueden responder el desafio.
	 * Tirarselo en la cara a todo el clan seria ruido para gente que no puede hacer
	 * nada al respecto.
	 */
	private void avisarEnPantallaAlMando(Team clan, String referencia, Object... argumentos) {
		List<Player> mando = new ArrayList<>();
		for (TeamPlayer miembro : clan.getMembers().getClone()) {
			if (miembro.getRank() == PlayerRank.DEFAULT) {
				continue;
			}
			Player jugador = Bukkit.getPlayer(miembro.getPlayerUUID());
			if (jugador != null) {
				mando.add(jugador);
			}
		}
		if (!mando.isEmpty()) {
			MessageManager.sendTitle(mando, referencia, argumentos);
		}
	}

	/**
	 * Formatea un monto para mostrarlo.
	 *
	 * <p>🔑 <b>Con {@link Locale#ROOT} a proposito.</b> {@code String.format("%.2f")}
	 * usa el separador decimal del sistema, y esta maquina esta en espaniol: el
	 * mensaje que le decia al jugador como aceptar el duelo le dictaba
	 * {@code 0,00}, y el comando parsea con {@code BigDecimal}, que solo acepta
	 * punto. O sea que el mensaje mandaba a escribir algo que el propio comando
	 * rechazaba.
	 *
	 * <p>Y sin decimales cuando el monto es redondo, que es el caso normal.
	 */
	private String fmt(double monto) {
		if (monto == Math.floor(monto) && !Double.isInfinite(monto)) {
			return String.valueOf((long) monto);
		}
		return String.format(Locale.ROOT, "%.2f", monto);
	}

	/** Lo que hay que responderle a quien ejecuto el comando. */
	public static class Resultado {
		public final boolean exito;
		public final String referencia;
		public final Object[] argumentos;

		private Resultado(boolean exito, String referencia, Object... argumentos) {
			this.exito = exito;
			this.referencia = referencia;
			this.argumentos = argumentos;
		}

		static Resultado ok(String referencia, Object... argumentos) {
			return new Resultado(true, referencia, argumentos);
		}

		static Resultado error(String referencia, Object... argumentos) {
			return new Resultado(false, referencia, argumentos);
		}
	}
}
