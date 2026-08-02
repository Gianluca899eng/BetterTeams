package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	/** Desafios sin aceptar, indexados por el clan retado. */
	private final Map<UUID, Desafio> desafios = new HashMap<>();
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
	}

	public int getObjetivoBajas() {
		return objetivoBajas;
	}

	public boolean isHabilitado() {
		return habilitado;
	}

	public boolean isPisaPvpIndividual() {
		return pisaPvpIndividual;
	}

	public Duelo getDuelo(Team clan) {
		return clan == null ? null : enCurso.get(clan.getID());
	}

	/** True solo si los dos clanes estan en el mismo duelo en curso. */
	public boolean sonRivales(Team uno, Team otro) {
		if (uno == null || otro == null || uno.getID().equals(otro.getID())) {
			return false;
		}
		Duelo duelo = enCurso.get(uno.getID());
		return duelo != null && duelo.participa(otro.getID());
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

		Desafio recibido = desafios.get(retador.getID());
		boolean esAceptacion = recibido != null && recibido.getRetador().equals(retado.getID());

		if (esAceptacion) {
			if (recibido.getApuesta() != apuesta) {
				// Los dos tienen que estar de acuerdo con lo que se juega.
				return Resultado.error("duelo.monto_distinto", fmt(recibido.getApuesta()));
			}
			desafios.remove(retador.getID());
			return arrancar(retado, retador, apuesta);
		}

		if (desafios.containsKey(retado.getID())
				&& desafios.get(retado.getID()).getRetador().equals(retador.getID())) {
			return Resultado.error("duelo.ya_desafiado");
		}
		if (apuesta > 0 && retador.getMoney() < apuesta) {
			return Resultado.error("duelo.sin_fondos", fmt(apuesta));
		}

		desafios.put(retado.getID(), new Desafio(retador.getID(), apuesta, System.currentTimeMillis() + esperaMillis));
		avisar(retado, "duelo.recibido", retador.getName(), fmt(apuesta));
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

		Duelo duelo = new Duelo(unClan.getID(), otroClan.getID(), apuesta,
				System.currentTimeMillis() + duracionMillis);
		enCurso.put(unClan.getID(), duelo);
		enCurso.put(otroClan.getID(), duelo);

		long minutos = duracionMillis / 60_000L;
		avisar(unClan, "duelo.arranco", otroClan.getName(), fmt(duelo.getPozo()), String.valueOf(minutos));
		avisar(otroClan, "duelo.arranco", unClan.getName(), fmt(duelo.getPozo()), String.valueOf(minutos));

		if (avisoGlobal) {
			MessageManager.sendMessage(new ArrayList<>(Main.plugin.getServer().getOnlinePlayers()),
					"duelo.aviso_global", unClan.getName(), otroClan.getName());
		}
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
		int bajas = duelo.sumarBaja(clanCaido.getID());

		if (bajas >= objetivoBajas) {
			cerrar(duelo);
			pagar(clanAtacante, duelo.getPozo());
			avisar(clanAtacante, "duelo.gano", clanCaido.getName(), fmt(duelo.getPozo()));
			avisar(clanCaido, "duelo.perdio", clanAtacante.getName(), fmt(duelo.getApuesta()));
			return;
		}

		String marcador = bajas + "/" + objetivoBajas;
		avisar(clanCaido, "duelo.marcador_propio", marcador, clanAtacante.getName());
		avisar(clanAtacante, "duelo.marcador_rival", clanCaido.getName(), marcador);
	}

	/** Revisa vencimientos. La llama una tarea repetitiva, no cada evento. */
	public void revisar() {
		long ahora = System.currentTimeMillis();

		desafios.entrySet().removeIf(entrada -> entrada.getValue().vencio(ahora));

		List<Duelo> terminados = new ArrayList<>();
		for (Duelo duelo : new ArrayList<>(enCurso.values())) {
			if (duelo.vencio(ahora) && !terminados.contains(duelo)) {
				terminados.add(duelo);
			}
		}
		for (Duelo duelo : terminados) {
			cerrar(duelo);
			resolverPorTiempo(duelo);
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
		for (Duelo duelo : new ArrayList<>(enCurso.values())) {
			cerrar(duelo);
			devolver(duelo, "duelo.cancelado_apagado");
		}
		desafios.clear();
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

	private void cerrar(Duelo duelo) {
		enCurso.remove(duelo.getClanA());
		enCurso.remove(duelo.getClanB());
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

	private String fmt(double monto) {
		return String.format("%.2f", monto);
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
