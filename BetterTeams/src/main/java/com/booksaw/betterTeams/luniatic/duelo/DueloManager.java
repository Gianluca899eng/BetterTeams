package com.booksaw.betterTeams.luniatic.duelo;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.luniatic.Texto;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import com.booksaw.betterTeams.text.Formatter;
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
	private final boolean debug;
	private final boolean pisaClaims;
	private final Set<String> comandosBloqueados;
	private final int radioBaseRival;
	private final boolean bloquearTeletransporte;
	private final boolean bloquearRespawnCama;
	private final boolean avisoDeDuelo;
	private final boolean libroDeDuelo;
	private final long esperaTrasRechazoMillis;
	/**
	 * Las lineas de la advertencia que recibe cada participante al empezar el duelo.
	 *
	 * <p>Son varias claves y no una sola con saltos: asi cada linea se puede traducir,
	 * reordenar o borrar desde el archivo de mensajes sin tocar codigo.
	 */
	private static final String[] AVISOS_DE_GUERRA = {
			"duelo.advertencia_titulo",
			"duelo.advertencia_matan",
			"duelo.advertencia_aliados",
			"duelo.advertencia_base",
			"duelo.advertencia_perdes",
			"duelo.advertencia_claims",
			"duelo.advertencia_salir"
	};
	private final Set<String> comandosDeCasa;
	/**
	 * Quien entra a los duelos: por jugador y por clan.
	 *
	 * <p>Se crea siempre, aunque los duelos esten apagados: el jugador tiene que poder
	 * dejar elegida su preferencia antes de que exista la primera duelo. Leer un archivo
	 * que no existe no cuesta nada y no lo crea.
	 */
	private final DueloPreferencias preferencias = new DueloPreferencias();
	/** Lo que hay que entregarle al que estaba desconectado cuando arranco el duelo. */
	private final DueloBuzon buzon = new DueloBuzon();
	/**
	 * Hasta cuando no puede volver a desafiar cada par: retador → retado → millis.
	 *
	 * <p><b>Existe por un agujero concreto, no por precaucion.</b> Un desafio pendiente ya
	 * bloquea el segundo —{@code ya_desafiado}— y vence solo a las 24 h, asi que el spam por
	 * repeticion no era posible… salvo despues de un rechazo: ahi el desafio se borra y se
	 * puede mandar otro en el acto. Con el libro de desafio siendo un item, eso pasa de ser
	 * molesto a llenarle el inventario a alguien.
	 *
	 * <p>Vive en memoria, igual que los desafios: un reinicio lo limpia. Es tolerable porque
	 * lo que protege son minutos, no dias.
	 */
	private final Map<UUID, Map<UUID, Long>> esperaTrasRechazo = new HashMap<>();
	/** Quienes cayeron a manos de un rival y todavia no volvieron a morir de otra forma. */
	private final Set<UUID> caidosPorRival = new java.util.HashSet<>();
	/** Caches del corte de vuelo. Se limpian al cerrarse el ultimo duelo. */
	private final Map<UUID, ClaimsCacheados> rivalEnCasa = new HashMap<>();
	private final Map<UUID, MiembrosCacheados> miembrosRivales = new HashMap<>();
	private final Map<UUID, MiembrosCacheados> miembrosPropios = new HashMap<>();
	private DueloBossBar barra;
	private GuardiaRegion guardia;


	/**
	 * Desafios sin aceptar: clan retado -> (clan retador -> desafio).
	 *
	 * <p>Son varios por clan a proposito. Con uno solo, el segundo que te desafiaba
	 * pisaba al primero sin que nadie se enterara.
	 */
	private final Map<UUID, Map<UUID, Desafio>> desafios = new HashMap<>();
	/** Duelos en curso, indexados por CADA participante (dos entradas por duelo). */
	private final Map<UUID, Duelo> enCurso = new HashMap<>();

	/**
	 * Una duracion que se puede pactar, con el objetivo de caidas que le corresponde.
	 *
	 * <p>El objetivo va atado a la duracion y no suelto: con un objetivo fijo, un duelo
	 * de siete dias se resolveria en la primera escaramuza y elegir la duracion no
	 * significaria nada.
	 */
	public static final class Duracion {
		public final String id;
		public final String etiqueta;
		public final long millis;
		public final int bajas;

		Duracion(String id, String etiqueta, long millis, int bajas) {
			this.id = id;
			this.etiqueta = etiqueta;
			this.millis = millis;
			this.bajas = bajas;
		}
	}

	/** Presets pactables. Nunca vacia: si la config no trae ninguno, se arma uno. */
	private final List<Duracion> duraciones;

	/**
	 * Lee los presets, con respaldo a las claves viejas.
	 *
	 * <p>Un servidor que venga de la version anterior tiene {@code duracion-minutos} y
	 * {@code objetivo-bajas} sueltos y ninguna lista: en vez de dejarlo sin duelos, se
	 * arma un unico preset con esos valores.
	 */
	private static List<Duracion> leerDuraciones(ConfigurationSection seccion) {
		List<Duracion> lista = new ArrayList<>();
		List<Map<?, ?>> crudas = seccion.getMapList("duraciones");
		for (Map<?, ?> cruda : crudas) {
			Object id = cruda.get("id");
			Object minutos = cruda.get("minutos");
			if (id == null || !(minutos instanceof Number)) {
				continue;
			}
			long millis = Math.max(1, ((Number) minutos).longValue()) * 60_000L;
			int bajas = cruda.get("bajas") instanceof Number
					? Math.max(1, ((Number) cruda.get("bajas")).intValue()) : 10;
			Object etiqueta = cruda.get("etiqueta");
			lista.add(new Duracion(id.toString(),
					etiqueta == null ? id.toString() : etiqueta.toString(), millis, bajas));
		}
		if (lista.isEmpty()) {
			long millis = Math.max(1, seccion.getInt("duracion-minutos", 30)) * 60_000L;
			int bajas = Math.max(1, seccion.getInt("objetivo-bajas", 10));
			lista.add(new Duracion("default", (millis / 60_000L) + " minutos", millis, bajas));
		}
		return lista;
	}

	public List<Duracion> getDuraciones() {
		return duraciones;
	}

	/** El preset por defecto es el primero de la lista: el mas corto. */
	public Duracion duracionPorDefecto() {
		return duraciones.get(0);
	}

	/** Null si no existe ese id, para que el comando responda el error que corresponda. */
	public Duracion duracionPorId(String id) {
		if (id == null) {
			return null;
		}
		for (Duracion duracion : duraciones) {
			if (duracion.id.equalsIgnoreCase(id)) {
				return duracion;
			}
		}
		return null;
	}

	public DueloManager(ConfigurationSection seccion) {
		if (seccion == null) {
			habilitado = false;
			duraciones = java.util.Collections.singletonList(new Duracion("default", "30 minutos", 30 * 60_000L, 10));
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
			debug = false;
			pisaClaims = false;
			comandosBloqueados = java.util.Collections.emptySet();
			radioBaseRival = 0;
			bloquearTeletransporte = false;
			bloquearRespawnCama = false;
			comandosDeCasa = java.util.Collections.emptySet();
			avisoDeDuelo = false;
			libroDeDuelo = false;
			esperaTrasRechazoMillis = 0;
			return;
		}
		habilitado = seccion.getBoolean("enabled", false);
		duraciones = leerDuraciones(seccion);
		duracionMillis = duraciones.get(0).millis;
		esperaMillis = Math.max(10, seccion.getInt("espera-aceptacion-segundos", 120)) * 1000L;
		apuestaMinima = Math.max(0, seccion.getDouble("apuesta.minima", 0));
		apuestaMaxima = Math.max(apuestaMinima, seccion.getDouble("apuesta.maxima", 100000));
		pisaPvpIndividual = seccion.getBoolean("pisa-pvp-individual", false);
		avisoGlobal = seccion.getBoolean("aviso-global", false);
		objetivoBajas = Math.max(1, seccion.getInt("objetivo-bajas", 10));
		barraActiva = seccion.getBoolean("barra", true);
		avisoTab = seccion.getString("aviso-tab", "&#FF4554PvP forzado por duelo");
		arrastraAliados = seccion.getBoolean("arrastra-aliados", true);
		debug = seccion.getBoolean("debug", false);
		pisaClaims = seccion.getBoolean("pisa-claims", true);
		comandosBloqueados = leerComandosBloqueados(seccion);
		radioBaseRival = Math.max(0, seccion.getInt("radio-base-rival", 300));
		bloquearTeletransporte = seccion.getBoolean("bloquear-aparicion.teletransporte", true);
		bloquearRespawnCama = seccion.getBoolean("bloquear-aparicion.respawn-cama", true);
		comandosDeCasa = leerLista(seccion, "bloquear-aparicion.comandos-sethome", "sethome");
		avisoDeDuelo = seccion.getBoolean("advertencia", true);
		libroDeDuelo = seccion.getBoolean("libro", true);
		esperaTrasRechazoMillis = Math.max(0, seccion.getInt("espera-tras-rechazo-minutos", 60)) * 60_000L;
	}

	/**
	 * Lee una lista de comandos, normalizada igual que {@link #leerComandosBloqueados}.
	 *
	 * <p>Lista vacia significa "no bloquear nada", no "usar el default": el default sale
	 * cuando la clave no existe. Es la diferencia entre no configurarlo y apagarlo.
	 */
	private static Set<String> leerLista(ConfigurationSection seccion, String ruta, String... porDefecto) {
		List<String> crudos = seccion.contains(ruta)
				? seccion.getStringList(ruta)
				: java.util.Arrays.asList(porDefecto);
		Set<String> limpios = new java.util.HashSet<>();
		for (String crudo : crudos) {
			if (crudo == null) {
				continue;
			}
			String limpio = crudo.trim().toLowerCase(Locale.ROOT);
			if (limpio.startsWith("/")) {
				limpio = limpio.substring(1);
			}
			if (!limpio.isEmpty()) {
				limpios.add(limpio);
			}
		}
		return limpios;
	}

	/**
	 * Comandos que no se pueden usar mientras el clan esta en duelo.
	 *
	 * <p>Se guardan en minusculas y sin la barra para poder comparar de una.
	 */
	private static Set<String> leerComandosBloqueados(ConfigurationSection seccion) {
		List<String> crudos = seccion.getStringList("comandos-bloqueados");
		if (crudos.isEmpty()) {
			crudos = java.util.Arrays.asList("dback", "back");
		}
		Set<String> limpios = new java.util.HashSet<>();
		for (String crudo : crudos) {
			if (crudo == null) {
				continue;
			}
			String limpio = crudo.trim().toLowerCase(Locale.ROOT);
			if (limpio.startsWith("/")) {
				limpio = limpio.substring(1);
			}
			if (!limpio.isEmpty()) {
				limpios.add(limpio);
			}
		}
		return limpios;
	}

	/** Si hay al menos un comando que bloquear. Evita registrar el listener al pedo. */
	public boolean hayComandosBloqueados() {
		return !comandosBloqueados.isEmpty();
	}

	/**
	 * Anota como fue la ULTIMA caida del jugador.
	 *
	 * <p>Se guarda la ultima y no un contador porque {@code /dback} devuelve al lugar
	 * de la ultima muerte: si despues caes a la lava, volver ahi ya no te devuelve a
	 * la pelea y no hay nada que bloquear.
	 */
	public void marcarCaida(UUID jugador, boolean porRival) {
		if (porRival) {
			caidosPorRival.add(jugador);
		} else {
			caidosPorRival.remove(jugador);
		}
	}

	/** Si su ultima caida fue a manos del clan rival, o pegada a una base rival. */
	public boolean cayoPorRival(UUID jugador) {
		return caidosPorRival.contains(jugador);
	}

	/**
	 * Los UUID de todos los miembros del bando rival de ese clan.
	 *
	 * <p>Vacio si el clan no esta en duelo. Recorre los miembros de cada clan del bando,
	 * asi que no se llama por tick: hoy sale una vez por muerte.
	 */
	public Set<UUID> miembrosDelBandoRival(Team clan) {
		Duelo duelo = getDuelo(clan);
		if (duelo == null) {
			return java.util.Collections.emptySet();
		}
		Set<UUID> miembros = new java.util.HashSet<>();
		for (UUID idClan : duelo.getBandoRival(clan.getID())) {
			Team rival = Team.getTeam(idClan);
			if (rival == null) {
				continue;
			}
			for (TeamPlayer miembro : rival.getMembers().getClone()) {
				miembros.add(miembro.getPlayerUUID());
			}
		}
		return miembros;
	}

	/**
	 * Si esa ubicacion esta pegada a una base del bando rival.
	 *
	 * <p>Existe para tapar el atajo obvio de la regla de {@code /dback}: si solo contara
	 * quien te mato, alcanzaba con tirarse a la lava adentro de la base enemiga para
	 * conservar el regreso.
	 */
	public boolean cercaDeBaseRival(Location ubicacion, Team clan) {
		if (guardia == null || clan == null) {
			return false;
		}
		return guardia.hayClaimDe(ubicacion, miembrosDelBandoRival(clan), radioBaseRival);
	}

	public int getRadioBaseRival() {
		return radioBaseRival;
	}

	public boolean isBloquearTeletransporte() {
		return bloquearTeletransporte;
	}

	public boolean isBloquearRespawnCama() {
		return bloquearRespawnCama;
	}

	/** Si hay algo que vigilar; si no, el listener de aparicion no se registra. */
	public boolean hayBloqueosDeAparicion() {
		return radioBaseRival > 0
				&& (bloquearTeletransporte || bloquearRespawnCama || !comandosDeCasa.isEmpty());
	}

	public boolean esComandoDeCasa(String comando) {
		return comando != null && comandosDeCasa.contains(comando);
	}

	/**
	 * Si a ese jugador no se le puede dejar APARECER en ese punto por estar dentro del
	 * radio de una base rival.
	 *
	 * <p>Es la regla de destino, distinta de la de {@link #cercaDeBaseRival}: aquella mira
	 * donde moriste, esta mira a donde vas. Con el radio grande cubre lo que la otra no
	 * puede —volver por {@code /home}, por cama o por un warp— sin depender de como te
	 * moriste.
	 *
	 * <p>🔑 <b>Salvo que ese punto sea una base propia.</b> Sin esa excepcion, dos clanes
	 * vecinos se quedan sin casa durante todo el duelo, y el que mas pierde es el que vive
	 * cerca, que no eligio nada. La base propia se resuelve por punto exacto, no por radio:
	 * lo que se protege es poder entrar a tu casa, no rondarla.
	 *
	 * <p>Corre en teletransportes de comando y en respawns, nunca por tick. El orden de los
	 * chequeos va de lo barato a lo caro: sin duelos en curso cuesta una comparacion.
	 */
	public boolean aparicionProhibida(Player jugador, Location destino) {
		if (!habilitado || guardia == null || jugador == null || destino == null
				|| radioBaseRival <= 0 || !hayDuelos()) {
			return false;
		}
		Team clan = Team.getTeam(jugador);
		Duelo duelo = getDuelo(clan);
		// Al que se bajo del duelo no se le restringe nada: no es parte de ella.
		if (duelo == null || !duelo.esParticipante(jugador.getUniqueId())) {
			return false;
		}
		if (!guardia.hayClaimDe(destino, miembrosCacheados(clan, true), radioBaseRival)) {
			return false;
		}
		// Excepcion de base propia. Se pregunta ultimo, y solo cuando el destino ya cayo en
		// zona rival: es el caso raro.
		return guardia.claimsDe(destino, miembrosCacheados(clan, false)).isEmpty();
	}

	/**
	 * Si a ese jugador hay que cortarle el vuelo por estar mezclado con una base del
	 * duelo. Dos casos, los dos pedidos por Gianluca:
	 * <ul>
	 * <li>Esta parado adentro de una base <b>rival</b>.
	 * <li>Un <b>rival</b> esta parado adentro de una base de <b>su</b> clan.
	 * </ul>
	 *
	 * <p>Lo consulta wFly una vez por segundo y por jugador conectado, asi que el orden
	 * importa: primero los cortes que no tocan WorldGuard ni recorren clanes.
	 */
	public boolean enZonaDeBaseDelDuelo(Player jugador) {
		if (!habilitado || guardia == null || jugador == null || !hayDuelos()) {
			return false;
		}
		Team clan = Team.getTeam(jugador);
		Duelo duelo = getDuelo(clan);
		// Al que se bajo del duelo no se le corta el vuelo: no la esta peleando.
		if (duelo == null || !duelo.esParticipante(jugador.getUniqueId())) {
			return false;
		}
		// Una sola consulta de regiones para los dos casos: miran el mismo punto.
		GuardiaRegion.Ubicacion aca = guardia.clasificar(jugador.getLocation(),
				miembrosCacheados(clan, true), miembrosCacheados(clan, false));

		// Caso 1: yo adentro de una base rival. Sin radio: la base es la base, no sus
		// alrededores. El radio de la regla del /dback existe por otro motivo.
		if (aca.isEnBaseRival()) {
			return true;
		}

		// Caso 2: un rival adentro de la base donde estoy YO. Si no estoy en ninguna base
		// propia no hay nada que comparar, y ese es el caso de casi todo el mundo.
		if (aca.getClaimsPropios().isEmpty()) {
			return false;
		}
		for (String ocupado : claimsPropiosConRival(clan)) {
			if (aca.getClaimsPropios().contains(ocupado)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Ids de los claims del clan donde hay parado un rival ahora mismo.
	 *
	 * <p>Devuelve el conjunto y no un booleano para que el corte sea preciso: al defensor
	 * se le baja el vuelo solo si el rival esta en <b>su</b> base, no en otra del clan al
	 * otro lado del mapa. Ese era el error de la primera version.
	 *
	 * <p>Cacheado un segundo <b>por clan</b>. No depende de quien pregunta, asi que los
	 * diez conectados de un clan comparten el mismo calculo. Es lo unico de todo esto que
	 * recorre jugadores.
	 */
	private Set<String> claimsPropiosConRival(Team clan) {
		long ahora = System.currentTimeMillis();
		ClaimsCacheados cacheados = rivalEnCasa.get(clan.getID());
		if (cacheados != null && cacheados.vence > ahora) {
			return cacheados.ids;
		}

		Set<UUID> propios = miembrosCacheados(clan, false);
		Set<String> ocupados = new java.util.HashSet<>();
		for (UUID id : miembrosCacheados(clan, true)) {
			Player rival = Bukkit.getPlayer(id);
			if (rival == null || !rival.isOnline()) {
				continue;
			}
			ocupados.addAll(guardia.claimsDe(rival.getLocation(), propios));
		}
		rivalEnCasa.put(clan.getID(), new ClaimsCacheados(ocupados, ahora + 1000L));
		return ocupados;
	}

	/**
	 * Miembros del bando rival o del propio, cacheados cinco segundos.
	 *
	 * <p>Recorrer los miembros de cada clan del bando es barato pero no gratis, y la
	 * composicion de un clan no cambia varias veces por segundo. Cinco segundos de
	 * atraso al sumar a alguien en pleno duelo no rompe nada.
	 */
	private Set<UUID> miembrosCacheados(Team clan, boolean rival) {
		Map<UUID, MiembrosCacheados> cache = rival ? miembrosRivales : miembrosPropios;
		long ahora = System.currentTimeMillis();
		MiembrosCacheados cacheados = cache.get(clan.getID());
		if (cacheados != null && cacheados.vence > ahora) {
			return cacheados.miembros;
		}

		Set<UUID> miembros;
		if (rival) {
			miembros = miembrosDelBandoRival(clan);
		} else {
			miembros = new java.util.HashSet<>();
			for (TeamPlayer miembro : clan.getMembers().getClone()) {
				miembros.add(miembro.getPlayerUUID());
			}
		}
		cache.put(clan.getID(), new MiembrosCacheados(miembros, ahora + 5000L));
		return miembros;
	}

	private static final class ClaimsCacheados {
		private final Set<String> ids;
		private final long vence;

		private ClaimsCacheados(Set<String> ids, long vence) {
			this.ids = ids;
			this.vence = vence;
		}
	}

	private static final class MiembrosCacheados {
		private final Set<UUID> miembros;
		private final long vence;

		private MiembrosCacheados(Set<UUID> miembros, long vence) {
			this.miembros = miembros;
			this.vence = vence;
		}
	}

	/**
	 * Si ese comando esta bloqueado para quien esta en duelo.
	 *
	 * @param comando nombre ya normalizado: minusculas, sin barra y sin namespace.
	 */
	public boolean esComandoBloqueado(String comando) {
		return comandosBloqueados.contains(comando);
	}

	/**
	 * Si el duelo pisa el {@code pvp deny} de los claims de jugadores.
	 *
	 * <p>Las regiones del staff —el spawn— no se pisan nunca, valga lo que valga
	 * esto.
	 */
	public boolean isPisaClaims() {
		return pisaClaims;
	}

	/** Logs de por que el override no destapo un dano. Apagado salvo diagnostico. */
	public boolean isDebug() {
		return debug;
	}

	public void setGuardia(GuardiaRegion guardia) {
		this.guardia = guardia;
	}

	public GuardiaRegion getGuardia() {
		return guardia;
	}

	/**
	 * Si al jugador lo pueden matar acá por estar en duelo, aca y ahora.
	 *
	 * <p>Mira tres cosas, y las tres tienen que dar: que su clan este en duelo, que
	 * el override este encendido, y que <b>en el lugar donde esta parado</b>
	 * WorldGuard permita PvP.
	 *
	 * <p>🔑 <b>Lo tercero es lo que hace honesto al cartel.</b> Los claims de
	 * ProtectionStones traen {@code pvp deny}, asi que adentro de una proteccion el
	 * duelo no aplica —a proposito— y decir "forzado" ahi seria mentir justo sobre
	 * si te pueden matar. La consulta a WorldGuard solo se hace si las dos primeras
	 * ya dieron, o sea casi nunca.
	 */
	public boolean pvpForzadoAca(Player jugador) {
		// hayDuelos() primero: esto lo pide el scoreboard una vez por segundo y por
		// jugador conectado, y resolver el clan recorre todos los clanes. Sin duelos en
		// curso —o sea casi siempre— tiene que costar una comparacion.
		if (!habilitado || !pisaPvpIndividual || jugador == null || !hayDuelos()) {
			return false;
		}
		Duelo duelo = getDuelo(Team.getTeam(jugador));
		if (duelo == null) {
			return false;
		}
		return guardia == null || guardia.permitePvp(jugador.getLocation());
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

	public long getDuracionMillis() {
		return duracionMillis;
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
	/**
	 * Acepta el desafio que mando ese clan, con las condiciones que el puso.
	 *
	 * <p>Existe porque la otra forma de aceptar —repetir el comando de desafio con el
	 * mismo monto— es innecesariamente dificil: el desafio ya dice cuanto y cuanto dura,
	 * y hacerselo escribir de nuevo es friccion, no consentimiento. El consentimiento es
	 * el acto de aceptar.
	 *
	 * <p>Es el simetrico de {@link #rechazar(Team, Team)}, y comparte su error cuando no
	 * hay nada que aceptar.
	 */
	public Resultado aceptar(Team retado, Team retador) {
		Map<UUID, Desafio> recibidos = desafios.get(retado.getID());
		Desafio desafio = recibidos == null ? null : recibidos.get(retador.getID());
		if (desafio == null || desafio.vencio(System.currentTimeMillis())) {
			if (recibidos != null) {
				recibidos.remove(retador.getID());
			}
			return Resultado.error("duelo.sin_desafio");
		}
		// Se delega en desafiar para no tener dos caminos que arranquen un duelo: ahi
		// estan los chequeos de fondos, de clanes ocupados y el consumo del desafio.
		// duracion null = "la que pacto el otro".
		return desafiar(retado, retador, desafio.getApuesta(), null);
	}

	public Resultado rechazar(Team retado, Team retador) {
		Map<UUID, Desafio> recibidos = desafios.get(retado.getID());
		if (recibidos == null || recibidos.remove(retador.getID()) == null) {
			return Resultado.error("duelo.sin_desafio");
		}
		// El que fue rechazado tiene que esperar antes de volver a desafiar a ESE clan.
		// Sin esto, rechazar es lo que habilita el spam: el desafio se borra y se puede
		// mandar otro en el acto.
		if (esperaTrasRechazoMillis > 0) {
			esperaTrasRechazo.computeIfAbsent(retador.getID(), id -> new HashMap<>())
					.put(retado.getID(), System.currentTimeMillis() + esperaTrasRechazoMillis);
		}
		avisar(retador, "duelo.rechazado", retado.getName());
		return Resultado.ok("duelo.rechazaste", retador.getName());
	}

	/** Minutos que le faltan a ese clan para poder volver a desafiar al otro; 0 si puede ya. */
	private long minutosDeEspera(UUID retador, UUID retado) {
		Map<UUID, Long> suyos = esperaTrasRechazo.get(retador);
		if (suyos == null) {
			return 0;
		}
		Long hasta = suyos.get(retado);
		if (hasta == null) {
			return 0;
		}
		long faltan = hasta - System.currentTimeMillis();
		return faltan <= 0 ? 0 : Math.max(1, faltan / 60_000L);
	}

	/** Si el clan esta libre para pactar un duelo. */
	public boolean estaLibre(Team clan) {
		return clan != null && estaLibre(clan.getID());
	}

	/** Por id, para no cargar el clan entero solo para saber si esta en duelo. */
	public boolean estaLibre(UUID clanId) {
		return clanId != null && !enCurso.containsKey(clanId);
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
		return desafiar(retador, retado, apuesta, null);
	}

	public Resultado desafiar(Team retador, Team retado, double apuesta, Duracion duracion) {
		if (!habilitado) {
			return Resultado.error("duelo.apagado");
		}
		// duracion == null significa "no elegi ninguna", y NO es lo mismo que elegir la
		// por defecto: si esto es una aceptacion, hay que tomar la que pacto el otro.
		// Resolverlo aca arriba rompia aceptar cualquier duelo que no fuera del preset
		// corto, porque ni el mensaje ni el menu le piden la duracion al que acepta.
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
			// La duracion la puso el que desafio: aceptar es aceptar SUS condiciones.
			// Solo se avisa si el que acepta pidio explicitamente otra distinta.
			Duracion pactada = duracionPorId(recibido.getDuracionId());
			if (pactada == null) {
				pactada = duracionPorDefecto();
			}
			if (duracion != null && !pactada.id.equalsIgnoreCase(duracion.id)) {
				return Resultado.error("duelo.duracion_distinta", pactada.etiqueta);
			}
			duracion = pactada;
			// 🔑 El desafio se consume SOLO si el duelo arranco. Antes se borraba
			// antes de intentar, asi que aceptar sin dinero en el banco te dejaba sin
			// invitacion y sin duelo: habia que pedirle al otro que la mandara de
			// nuevo por un error que no cambiaba nada del acuerdo.
			Resultado resultado = arrancar(retado, retador, apuesta, duracion);
			if (resultado.exito) {
				misRecibidos.remove(retado.getID());
			}
			return resultado;
		}

		// Desde aca es un desafio nuevo: sin eleccion va el preset mas corto, para que
		// mandar una semana de duelo sea siempre una decision y nunca lo que pasa solo.
		if (duracion == null) {
			duracion = duracionPorDefecto();
		}

		// La espera se comprueba SOLO en la rama de desafio nuevo: si esto termino siendo
		// una aceptacion, ya se resolvio arriba. Rechazar un desafio y aceptar el suyo no
		// tiene por que estar penado.
		long faltan = minutosDeEspera(retador.getID(), retado.getID());
		if (faltan > 0) {
			return Resultado.error("duelo.espera_para_desafiar", retado.getName(), String.valueOf(faltan));
		}

		Map<UUID, Desafio> susRecibidos = desafios.computeIfAbsent(retado.getID(), id -> new HashMap<>());
		if (susRecibidos.containsKey(retador.getID())) {
			return Resultado.error("duelo.ya_desafiado");
		}
		if (apuesta > 0 && retador.getMoney() < apuesta) {
			return Resultado.error("duelo.sin_fondos", fmt(apuesta));
		}

		long vence = System.currentTimeMillis() + esperaMillis;
		susRecibidos.put(retador.getID(), new Desafio(retador.getID(), apuesta, duracion.id, vence));
		entregarDesafio(retador, retado, apuesta, duracion, vence);
		avisar(retado, "duelo.recibido", retador.getName(), fmt(apuesta), duracion.etiqueta);
		// Y en pantalla al mando, que es el unico que puede responderlo: un mensaje
		// de chat se pierde entre lo demas y el desafio se queda esperando.
		avisarEnPantallaAlMando(retado, "duelo.recibido_titulo", retador.getName(), fmt(apuesta));
		// Y los botones, que es la via rapida para el que si puede responder.
		botonesDeRespuesta(retado, retador);
		return Resultado.ok("duelo.enviado", retado.getName(), fmt(apuesta));
	}

	private Resultado arrancar(Team unClan, Team otroClan, double apuesta, Duracion duracion) {
		// Los aliados se congelan al arrancar: aliarse en medio del duelo no trae
		// refuerzos, y desaliarse no saca a nadie del lio en el que ya estaba.
		Duelo duelo = new Duelo(unClan.getID(), aliadosLibres(unClan),
				otroClan.getID(), aliadosLibres(otroClan),
				apuesta, System.currentTimeMillis() + duracion.millis,
				duracion.millis, duracion.bajas);

		// Y los participantes tambien, por el mismo motivo: si se leyera en vivo,
		// alcanzaria con bajarse cuando te estan por matar.
		Set<UUID> ladoA = participantesDe(duelo.getBando(unClan.getID()));
		Set<UUID> ladoB = participantesDe(duelo.getBando(otroClan.getID()));
		// Se comprueba ANTES de tocar el dinero: devolver un error despues de descontar el
		// pozo dejaria a los dos clanes sin duelo y sin dinero.
		if (ladoA.isEmpty() || ladoB.isEmpty()) {
			return Resultado.error("duelo.sin_participantes");
		}
		Set<UUID> participantes = new java.util.HashSet<>(ladoA);
		participantes.addAll(ladoB);
		duelo.congelarParticipantes(participantes);

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

		for (UUID id : duelo.todosLosClanes()) {
			enCurso.put(id, duelo);
		}
		guardar();

		// El aviso dice la etiqueta del preset ("7 dias"), no los minutos: 10080 no le
		// dice nada a nadie.
		avisar(unClan, "duelo.arranco", otroClan.getName(), fmt(duelo.getPozo()), duracion.etiqueta);
		avisar(otroClan, "duelo.arranco", unClan.getName(), fmt(duelo.getPozo()), duracion.etiqueta);

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
						duracion.etiqueta);
			}
		}

		if (avisoGlobal) {
			MessageManager.sendMessage(new ArrayList<>(Main.plugin.getServer().getOnlinePlayers()),
					"duelo.aviso_global", unClan.getName(), otroClan.getName());
		}

		advertirYEntregarLibro(duelo, unClan, otroClan, duracion);
		dibujarTodos(duelo);
		return Resultado.ok("duelo.arranco_confirmacion", unClan.getName());
	}

	/**
	 * Los aliados que entrarian con ese clan si el duelo empezara ahora.
	 *
	 * <p>Lo usa el menu para que el que recibe un desafio sepa contra cuantos se esta
	 * metiendo <b>antes</b> de aceptar. Ya descuenta a los que estan en otro duelo y a los
	 * que decidieron no entrar en duelos ajenas.
	 */
	public Set<UUID> aliadosQueEntrarian(Team clan) {
		return clan == null ? java.util.Collections.emptySet() : aliadosLibres(clan);
	}

	/**
	 * Cuantos jugadores pondria ese bando: el clan mas sus aliados, contando solo a los
	 * que participan.
	 *
	 * <p>Es el numero honesto para decidir si aceptar. Un conteo de miembros a secas
	 * mentiria en la direccion peligrosa: parecerian mas de los que van a pelear.
	 */
	public int fuerzaDe(Team clan) {
		if (clan == null) {
			return 0;
		}
		Set<UUID> clanes = new java.util.LinkedHashSet<>();
		clanes.add(clan.getID());
		clanes.addAll(aliadosLibres(clan));
		return participantesDe(clanes).size();
	}

	/** Los miembros de esos clanes que eligieron participar. */
	private Set<UUID> participantesDe(Set<UUID> clanes) {
		Set<UUID> jugadores = new java.util.LinkedHashSet<>();
		for (UUID idClan : clanes) {
			Team clan = Team.getTeam(idClan);
			if (clan == null) {
				continue;
			}
			for (TeamPlayer miembro : clan.getMembers().getClone()) {
				if (preferencias.participa(miembro.getPlayerUUID())) {
					jugadores.add(miembro.getPlayerUUID());
				}
			}
		}
		return jugadores;
	}

	/**
	 * La advertencia y el libro, a cada participante.
	 *
	 * <p><b>Al que esta desconectado se le encola</b>: un duelo la pactan dos lideres, no
	 * el clan entero conectado, y el que no estaba es justamente el que menos sabe en que
	 * lo metieron. Ver {@link DueloBuzon}.
	 *
	 * <p>El libro se arma <b>una vez</b> y se le entrega a todos: resolver nombres de
	 * jugadores desconectados es lo unico caro y no hace falta repetirlo por persona.
	 */
	private void advertirYEntregarLibro(Duelo duelo, Team clanA, Team clanB, Duracion duracion) {
		if (!avisoDeDuelo && !libroDeDuelo) {
			return;
		}
		String titulo = libroDeDuelo ? LibroDelDuelo.titulo(clanA, clanB) : "";
		List<String> paginas = libroDeDuelo
				? LibroDelDuelo.paginas(duelo, clanA, clanB, duracion.etiqueta)
				: java.util.Collections.emptyList();

		// 🔑 El aviso nombra al clan rival, asi que se arma UNA VEZ POR BANDO. Con un solo
		// texto para los dos, a la mitad de los participantes se le decia que su propio
		// clan los podia matar. El libro si es el mismo: muestra los dos lados.
		entregarABando(duelo, duelo.getBando(duelo.getClanA()),
				avisosContra(clanB, duracion), titulo, paginas);
		entregarABando(duelo, duelo.getBando(duelo.getClanB()),
				avisosContra(clanA, duracion), titulo, paginas);
	}

	/**
	 * El libro de desafio, a lider y colideres del clan desafiado.
	 *
	 * <p>Solo al mando: un miembro comun no puede aceptar ni rechazar, asi que darle un
	 * libro que no puede usar es ruido — y con el libro siendo un item, ruido que ocupa
	 * lugar en el inventario.
	 *
	 * <p>Al desconectado se le encola <b>con la fecha del desafio</b>, no con la general del
	 * buzon: si entra tres dias despues, el desafio ya vencio y el libro invitaria a algo que
	 * no existe.
	 */
	private void entregarDesafio(Team retador, Team retado, double apuesta, Duracion duracion, long vence) {
		if (!libroDeDuelo) {
			return;
		}
		String titulo = LibroDelDuelo.tituloDesafio(retador);
		List<String> paginas = LibroDelDuelo.paginasDeDesafio(retador, retado, apuesta,
				duracion.etiqueta, duracion.bajas, fuerzaDe(retador), fuerzaDe(retado),
				aliadosQueEntrarian(retador), Math.max(1, esperaMillis / 3_600_000L));
		List<String> sinAvisos = java.util.Collections.emptyList();

		for (TeamPlayer miembro : retado.getMembers().getClone()) {
			if (miembro.getRank() == PlayerRank.DEFAULT) {
				continue;
			}
			Player conectado = Bukkit.getPlayer(miembro.getPlayerUUID());
			if (conectado != null && conectado.isOnline()) {
				entregarLibro(conectado, titulo, paginas);
			} else {
				buzon.encolar(miembro.getPlayerUUID(), sinAvisos, titulo, paginas, vence);
			}
		}
	}

	/**
	 * El libro de resultados, a todos los que pelearon.
	 *
	 * <p>Espejo del de arranque, y por el mismo motivo: quien gano, con que marcador y
	 * cuanto se llevo se dice hoy en un mensaje de chat que se pierde en dos minutos. Al
	 * que estaba desconectado cuando termino se le encola.
	 *
	 * <p>No se entrega cuando el duelo se abandona por apagado del servidor: ahi no hay
	 * resultado que contar.
	 */
	private void entregarResumen(Duelo duelo, LibroDelDuelo.Final comoTermino, Team ganador) {
		if (!libroDeDuelo) {
			return;
		}
		Team clanA = Team.getTeam(duelo.getClanA());
		Team clanB = Team.getTeam(duelo.getClanB());
		String titulo = LibroDelDuelo.tituloResumen(clanA, clanB);
		List<String> paginas = LibroDelDuelo.paginasDeResumen(duelo, clanA, clanB, comoTermino, ganador);
		List<String> sinAvisos = java.util.Collections.emptyList();

		// Se recorren los bandos en vez de la lista de participantes: asi un duelo viejo
		// —guardado antes de que existiera esa lista— tambien reparte el resumen, porque
		// ahi esParticipante() contesta que si a todos. Los avisos de chat ya salieron por
		// su cuenta; esto es solo el libro.
		entregarABando(duelo, duelo.getBando(duelo.getClanA()), sinAvisos, titulo, paginas);
		entregarABando(duelo, duelo.getBando(duelo.getClanB()), sinAvisos, titulo, paginas);
	}

	/** Las lineas de la advertencia, nombrando al rival de ese bando. */
	private List<String> avisosContra(Team rival, Duracion duracion) {
		List<String> avisos = new ArrayList<>();
		if (!avisoDeDuelo) {
			return avisos;
		}
		String nombreRival = rival == null ? "?" : rival.getName();
		for (String referencia : AVISOS_DE_GUERRA) {
			// Se guarda ya coloreado: al entregarlo desde el buzon se manda como texto
			// plano, y ahi el &#RRGGBB no lo traduce nadie.
			avisos.add(Texto.col(MessageManager.getMessage(referencia, nombreRival, duracion.etiqueta)));
		}
		return avisos;
	}

	private void entregarABando(Duelo duelo, Set<UUID> clanes, List<String> avisos,
			String titulo, List<String> paginas) {
		for (UUID idClan : clanes) {
			Team clan = Team.getTeam(idClan);
			if (clan == null) {
				continue;
			}
			for (TeamPlayer miembro : clan.getMembers().getClone()) {
				if (duelo.esParticipante(miembro.getPlayerUUID())) {
					entregarPaquete(miembro.getPlayerUUID(), avisos, titulo, paginas);
				}
			}
		}
	}

	/** Al conectado se le entrega ahora; al que no esta, se le encola para cuando entre. */
	private void entregarPaquete(UUID id, List<String> avisos, String titulo, List<String> paginas) {
		Player conectado = Bukkit.getPlayer(id);
		if (conectado != null && conectado.isOnline()) {
			for (String linea : avisos) {
				conectado.sendMessage(linea);
			}
			entregarLibro(conectado, titulo, paginas);
		} else {
			buzon.encolar(id, avisos, titulo, paginas);
		}
	}

	/** Al inventario, y lo que no entre al piso: perder el libro por inventario lleno seria
	 * perder justo lo que explica en que te metieron. */
	private void entregarLibro(Player jugador, String titulo, List<String> paginas) {
		if (paginas.isEmpty()) {
			return;
		}
		for (org.bukkit.inventory.ItemStack sobrante
				: jugador.getInventory().addItem(LibroDelDuelo.armar(titulo, paginas)).values()) {
			jugador.getWorld().dropItem(jugador.getLocation(), sobrante);
		}
	}

	/** Entrega lo que quedo pendiente. Lo llama el listener de entrada. */
	public void entregarPendientes(Player jugador) {
		buzon.entregar(jugador);
	}

	public boolean tienePendientes(UUID jugador) {
		return buzon.tienePendiente(jugador);
	}

	public DueloPreferencias getPreferencias() {
		return preferencias;
	}

	/**
	 * Suma a alguien al duelo que esta peleando su clan. Herramienta de staff.
	 *
	 * <p>El caso que resuelve es concreto: alguien se olvido de anotarse y su clan quedo
	 * peleando sin el. <b>No le cambia la preferencia</b> para las proximas duelos — eso
	 * lo elige el jugador, no el staff; el mensaje se lo recuerda.
	 *
	 * <p>Le llega la misma advertencia y el mismo libro que a los demas, o se le encolan
	 * si esta desconectado.
	 */
	public Resultado agregarADuelo(org.bukkit.OfflinePlayer jugador) {
		if (!habilitado) {
			return Resultado.error("duelo.apagado");
		}
		Team clan = Team.getTeam(jugador);
		if (clan == null) {
			return Resultado.error("duelo.agregar_sin_clan");
		}
		Duelo duelo = getDuelo(clan);
		if (duelo == null) {
			return Resultado.error("duelo.agregar_sin_duelo");
		}
		if (!duelo.agregarParticipante(jugador.getUniqueId())) {
			return Resultado.error("duelo.agregar_ya_estaba");
		}
		guardar();

		Team rival = Team.getTeam(duelo.rivalDe(clan.getID()));
		Duracion duracion = duracionDe(duelo);
		String titulo = libroDeDuelo
				? LibroDelDuelo.titulo(Team.getTeam(duelo.getClanA()), Team.getTeam(duelo.getClanB()))
				: "";
		List<String> paginas = libroDeDuelo
				? LibroDelDuelo.paginas(duelo, Team.getTeam(duelo.getClanA()),
						Team.getTeam(duelo.getClanB()), duracion.etiqueta)
				: java.util.Collections.emptyList();
		entregarPaquete(jugador.getUniqueId(), avisosContra(rival, duracion), titulo, paginas);

		String nombre = jugador.getName() == null ? jugador.getUniqueId().toString() : jugador.getName();
		return Resultado.ok("duelo.agregar_ok", nombre, clan.getName());
	}

	/**
	 * El preset que corresponde a la duracion de ese duelo.
	 *
	 * <p>El duelo guarda milisegundos, no el preset: si alguien edita las duraciones del
	 * config mientras hay un duelo en curso, ninguno coincide. En ese caso se arma una
	 * etiqueta con los minutos en vez de reventar.
	 */
	private Duracion duracionDe(Duelo duelo) {
		for (Duracion candidata : duraciones) {
			if (candidata.millis == duelo.getDuracionMillis()) {
				return candidata;
			}
		}
		long minutos = Math.max(1, duelo.getDuracionMillis() / 60_000L);
		return new Duracion("?", minutos + " minutos", duelo.getDuracionMillis(), duelo.getObjetivoBajas());
	}

	/**
	 * Si ese jugador esta adentro del duelo de su clan.
	 *
	 * <p>Lo consultan todas las reglas del duelo. Sin duelo en curso —o si se bajo— es
	 * como si el duelo no existiera para el.
	 */
	public boolean esParticipante(Player jugador) {
		if (jugador == null || !hayDuelos()) {
			return false;
		}
		Duelo duelo = getDuelo(Team.getTeam(jugador));
		return duelo != null && duelo.esParticipante(jugador.getUniqueId());
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
			entregarResumen(duelo, LibroDelDuelo.Final.RENDICION, rival);
		} else {
			// El rival dejo de existir: se le devuelve lo suyo al que sigue en pie.
			pagar(clan, duelo.getPozo());
			avisar(clan, "duelo.rival_desaparecio");
		}
		return Resultado.ok("duelo.rendido");
	}

	/**
	 * El clan dejo de existir. Si era principal de un duelo, el duelo se cierra y el pozo
	 * entero va al rival: sin esto el duelo seguia contra un fantasma hasta que venciera el
	 * reloj, y al resolverse la mitad del pozo se evaporaba.
	 */
	public void alDesbandarse(UUID clanId) {
		Duelo duelo = clanId == null ? null : enCurso.get(clanId);
		if (duelo == null) {
			return;
		}

		// Un aliado que se desbanda no define el duelo: solo se saca su entrada.
		if (!duelo.esPrincipal(clanId)) {
			enCurso.remove(clanId);
			guardar();
			return;
		}

		Team rival = Team.getTeam(duelo.rivalDe(clanId));
		cerrar(duelo);

		if (rival != null) {
			pagar(rival, duelo.getPozo());
			avisar(rival, "duelo.rival_desaparecio");
		}
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
		guardar();
		Team principalRival = Team.getTeam(duelo.rivalDe(clanCaido.getID()));

		if (bajas >= duelo.getObjetivoBajas()) {
			cerrar(duelo);
			if (principalRival != null) {
				pagar(principalRival, duelo.getPozo());
				avisar(principalRival, "duelo.gano", clanCaido.getName(), fmt(duelo.getPozo()));
				avisar(clanCaido, "duelo.perdio", principalRival.getName(), fmt(duelo.getApuesta()));
			} else {
				// El clan ganador desaparecio sin pasar por alDesbandarse: borrado a mano o
				// perdido al cargarlo. Cerrar sin pagar destruiria las DOS apuestas en
				// silencio, asi que el pozo se lo lleva el que sigue en pie, igual que en
				// rendirse() cuando el rival ya no existe.
				pagar(clanCaido, duelo.getPozo());
				avisar(clanCaido, "duelo.rival_desaparecio");
				Main.plugin.getLogger().warning("[duelo] el rival de " + clanCaido.getName()
						+ " ya no existe al cerrarse el duelo; el pozo de " + fmt(duelo.getPozo())
						+ " vuelve al clan que quedo en pie.");
			}
			entregarResumen(duelo, LibroDelDuelo.Final.OBJETIVO, principalRival);
			return;
		}

		String marcador = bajas + "/" + duelo.getObjetivoBajas();
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

		// Las esperas vencidas se limpian aca y no al consultarlas: si no, el mapa se queda
		// con una entrada por cada rechazo que hubo desde que arranco el servidor.
		for (Map<UUID, Long> suyas : esperaTrasRechazo.values()) {
			suyas.values().removeIf(hasta -> hasta <= ahora);
		}
		esperaTrasRechazo.values().removeIf(Map::isEmpty);

		buzon.purgarVencidos();

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
			entregarResumen(duelo, LibroDelDuelo.Final.EMPATE, null);
			return;
		}
		Team ganadorClan = Team.getTeam(ganador);
		Team perdedorClan = Team.getTeam(duelo.rivalDe(ganador));
		if (ganadorClan == null) {
			// devolver() le paga a cada clan SU apuesta, asi que con el ganador desaparecido
			// la mitad del pozo se evaporaba. Se lo lleva entero el que sigue en pie.
			if (perdedorClan != null) {
				pagar(perdedorClan, duelo.getPozo());
				avisar(perdedorClan, "duelo.rival_desaparecio");
				Main.plugin.getLogger().warning("[duelo] el clan que ganaba por tiempo ya no existe;"
						+ " el pozo de " + fmt(duelo.getPozo()) + " va a " + perdedorClan.getName() + ".");
			} else {
				Main.plugin.getLogger().severe("[duelo] ningun clan del duelo existe al vencer el"
						+ " reloj; el pozo de " + fmt(duelo.getPozo()) + " se queda sin dueño.");
			}
			entregarResumen(duelo, LibroDelDuelo.Final.EMPATE, null);
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
		entregarResumen(duelo, LibroDelDuelo.Final.TIEMPO, ganadorClan);
	}

	/** Se llama al apagar el plugin: nadie se queda sin su parte del pozo. */
	/**
	 * Cancela todos los duelos y devuelve el pozo.
	 *
	 * <p>Ya <b>no</b> se llama al apagar el servidor —para eso esta
	 * {@link #guardarYSoltar()}— sino cuando de verdad hay que abandonar los duelos en
	 * curso: apagar la funcion, o sacarla del plugin. Cancelar un duelo de tres dias
	 * porque el servidor se reinicio seria peor que no tener duelos largos.
	 */
	public void devolverTodo() {
		for (Duelo duelo : unicos(enCurso.values())) {
			cerrar(duelo);
			devolver(duelo, "duelo.cancelado_apagado");
		}
		desafios.clear();
		if (barra != null) {
			barra.quitarTodas();
		}
		guardar();
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
			// Un aliado que decidio no entrar en duelos ajenas se queda afuera. Es la
			// version de clan de la preferencia individual: el aliado tampoco pacto nada.
			if (!enCurso.containsKey(id) && Team.getTeam(id) != null && preferencias.ayudaAliados(id)) {
				lista.add(id);
			}
		}
		return lista;
	}

	/**
	 * Punto unico de cierre, asi ninguna salida —rendicion, objetivo, tiempo,
	 * apagado del servidor— se olvida de sacar la barra ni de devolver el PvP.
	 */
	private void cerrar(Duelo duelo) {
		for (UUID id : duelo.todosLosClanes()) {
			enCurso.remove(id);
			borrarBarra(Team.getTeam(id));
		}
		// Higiene: sin duelos en curso las marcas ya no significan nada. No hace falta
		// borrarlas por duelo porque el bloqueo pregunta primero si el clan esta en uno.
		if (enCurso.isEmpty()) {
			caidosPorRival.clear();
			rivalEnCasa.clear();
			miembrosRivales.clear();
			miembrosPropios.clear();
		}
		guardar();
	}

	/**
	 * Baja los duelos en curso a disco.
	 *
	 * <p>Se llama cuando cambian —empezar, sumar una baja, cerrar, apagar— y nunca por
	 * tick. Son unos pocos duelos y un archivo chico.
	 */
	private void guardar() {
		DuelosGuardados.guardar(unicos(enCurso.values()));
	}

	/**
	 * Levanta los duelos que quedaron de la sesion anterior.
	 *
	 * <p><b>Es lo que hace posible que un duelo dure dias.</b> Sin esto, cualquier
	 * reinicio caia en el medio y el duelo se cancelaba solo, devolviendo el pozo y
	 * borrando el marcador.
	 *
	 * <p>Se llama despues de que los clanes esten cargados, porque hace falta resolver
	 * los dos principales: un clan borrado con el servidor apagado deja un duelo sin
	 * rival, y ese se descarta en vez de quedar colgado.
	 *
	 * <p>Los que vencieron mientras el servidor estaba apagado no se resuelven aca: los
	 * cierra {@link #revisar()} en su primera pasada, que corre a los 10 segundos. Asi
	 * hay un solo lugar que decide como termina un duelo.
	 */
	public void cargar() {
		if (!habilitado) {
			return;
		}
		int levantados = 0;
		for (Duelo duelo : DuelosGuardados.cargar()) {
			if (Team.getTeam(duelo.getClanA()) == null || Team.getTeam(duelo.getClanB()) == null) {
				Main.plugin.getLogger().warning("[duelo] se descarta un duelo guardado: falta uno de los clanes");
				continue;
			}
			for (UUID id : duelo.todosLosClanes()) {
				enCurso.put(id, duelo);
			}
			dibujarTodos(duelo);
			levantados++;
		}
		if (levantados > 0) {
			Main.plugin.getLogger().info("[duelo] se levantaron " + levantados + " duelo(s) en curso");
			// El archivo se reescribe con lo que quedo: si alguno se descarto, no tiene
			// sentido que siga en disco esperando el proximo arranque.
			guardar();
		}
	}

	/**
	 * Al apagar: guarda y suelta las barras, <b>sin devolver el pozo</b>.
	 *
	 * <p>Es lo contrario de {@link #devolverTodo()}, y la diferencia es a proposito: un
	 * reinicio no termina un duelo de tres dias, asi que el pozo tiene que seguir
	 * retenido igual que el duelo.
	 */
	public void guardarYSoltar() {
		guardar();
		if (barra != null) {
			barra.quitarTodas();
		}
	}

	private void pagar(Team clan, double monto) {
		if (monto <= 0) {
			return;
		}
		double tope = clan.getMaxMoney();
		double nuevo = clan.getMoney() + monto;
		if (tope >= 0 && nuevo > tope) {
			// El tope del banco recorta el pago y ese dinero desaparece. Queda anotado: si no,
			// el clan cobra menos de lo que dice el mensaje y no hay forma de saber por que.
			// El maximo con 0 es por si el banco ya estaba por encima del tope, que pasa si
			// alguien lo baja en el config con clanes ya ricos: ahi no entra nada, no menos.
			double entra = Math.max(0, tope - clan.getMoney());
			Main.plugin.getLogger().warning("[duelo] el banco de " + clan.getName() + " esta en su"
					+ " tope (" + fmt(tope) + "): de " + fmt(monto) + " solo entraron "
					+ fmt(entra) + ".");
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
	/**
	 * Los dos botones para responder el desafio, clickeables en el chat.
	 *
	 * <p>Van <b>solo al mando</b>: aceptar y rechazar piden rango de duenio o colider,
	 * asi que ofrecerselo a un miembro comun seria un boton que falla.
	 *
	 * <p>⚠️ <b>El clic no llega a Bedrock por Geyser.</b> Por eso el mensaje de texto
	 * sigue diciendo el comando entero: los botones son un atajo, no la unica via.
	 */
	private void botonesDeRespuesta(Team retado, Team retador) {
		BukkitAudiences audiencia = Main.plugin.getAdventure();
		String nombre = retador.getName();
		// Un nombre con espacios parte el argumento del comando. El texto de arriba ya
		// dice que escribir, asi que se saltea el boton en vez de ofrecer uno roto.
		if (audiencia == null || nombre == null || nombre.isEmpty() || nombre.contains(" ")) {
			return;
		}

		Component linea = Formatter.absolute().process("<color:#7162FF>» </color>")
				.append(boton("<color:#56FF3B>[Aceptar]</color>", "/clan duelo aceptar " + nombre,
						"<color:#E4D9FF>Aceptar el duelo de </color><color:#9235FF>" + nombre))
				.append(Formatter.absolute().process("  "))
				.append(boton("<color:#FF4554>[Rechazar]</color>", "/clan duelo rechazar " + nombre,
						"<color:#E4D9FF>Rechazar el duelo de </color><color:#9235FF>" + nombre));

		for (TeamPlayer miembro : retado.getMembers().getClone()) {
			if (miembro.getRank() == PlayerRank.DEFAULT) {
				continue;
			}
			Player jugador = Bukkit.getPlayer(miembro.getPlayerUUID());
			if (jugador != null) {
				audiencia.player(jugador).sendMessage(linea);
			}
		}
	}

	private static Component boton(String texto, String comando, String ayuda) {
		return Formatter.absolute().process(texto)
				.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(comando))
				.hoverEvent(net.kyori.adventure.text.event.HoverEvent
						.showText(Formatter.absolute().process(ayuda)));
	}

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
