package com.booksaw.betterTeams;

import com.booksaw.betterTeams.commands.HelpCommand;
import com.booksaw.betterTeams.commands.ParentCommand;
import com.booksaw.betterTeams.commands.PermissionParentCommand;
import com.booksaw.betterTeams.commands.team.*;
import com.booksaw.betterTeams.commands.team.chest.ChestCheckCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestClaimCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestRemoveCommand;
import com.booksaw.betterTeams.commands.team.chest.ChestRemoveallCommand;
import com.booksaw.betterTeams.commands.teama.*;
import com.booksaw.betterTeams.commands.teama.chest.*;
import com.booksaw.betterTeams.commands.teama.meta.MetaGetTeama;
import com.booksaw.betterTeams.commands.teama.meta.MetaRemoveTeama;
import com.booksaw.betterTeams.commands.teama.meta.MetaSetTeama;
import com.booksaw.betterTeams.commands.teama.money.AddMoney;
import com.booksaw.betterTeams.commands.teama.money.RemoveMoney;
import com.booksaw.betterTeams.commands.teama.money.SetMoney;
import com.booksaw.betterTeams.commands.teama.score.AddScore;
import com.booksaw.betterTeams.commands.teama.score.RemoveScore;
import com.booksaw.betterTeams.commands.teama.score.SetScore;
import com.booksaw.betterTeams.cooldown.CooldownManager;
import com.booksaw.betterTeams.cost.CostManager;
import com.booksaw.betterTeams.customEvents.post.PostBetterTeamsReloadEvent;
import com.booksaw.betterTeams.events.*;
import com.booksaw.betterTeams.events.MCTeamManagement.BelowNameType;
import com.booksaw.betterTeams.extension.ExtensionManager;
import com.booksaw.betterTeams.luniatic.duelo.DueloCommand;
import com.booksaw.betterTeams.luniatic.duelo.DueloComandoListener;
import com.booksaw.betterTeams.luniatic.duelo.DueloDamageListener;
import com.booksaw.betterTeams.luniatic.duelo.DueloBossBar;
import com.booksaw.betterTeams.luniatic.duelo.DueloDeathListener;
import com.booksaw.betterTeams.luniatic.duelo.DueloPlaceholders;
import com.booksaw.betterTeams.luniatic.duelo.DueloManager;
import com.booksaw.betterTeams.luniatic.gui.MenuCommand;
import com.booksaw.betterTeams.luniatic.gui.MenuListener;
import com.booksaw.betterTeams.integrations.UltimateClaimsManager;
import com.booksaw.betterTeams.integrations.WorldGuardManagerV7;
import com.booksaw.betterTeams.integrations.apollo.ApolloManager;
import com.booksaw.betterTeams.integrations.hologram.DHHologramManager;
import com.booksaw.betterTeams.integrations.hologram.HDHologramManager;
import com.booksaw.betterTeams.integrations.hologram.HologramManager;
import com.booksaw.betterTeams.integrations.placeholder.TeamPlaceholders;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.score.ScoreManagement;
import com.booksaw.betterTeams.team.level.LevelManager;
import com.booksaw.betterTeams.team.storage.StorageType;
import com.booksaw.betterTeams.team.storage.convert.Converter;
import com.booksaw.betterTeams.team.storage.storageManager.SeparatedYamlStorageManager;
import com.booksaw.betterTeams.team.storage.storageManager.YamlStorageManager;
import com.tcoded.folialib.FoliaLib;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Main class of the plugin, extends JavaPlugin
 *
 * @author booksaw
 */
public class Main extends JavaPlugin {

	public static Main plugin;
	public static Economy econ = null;
	public static Permission perms = null;
	public static boolean placeholderAPI = false;
	public boolean useHolograms = false;
	public MCTeamManagement teamManagement;
	/** Duelos pactados entre clanes. Apagado salvo que se encienda en el config. */
	@Getter
	public DueloManager dueloManager;
	/** El icono con el que cada clan se muestra en el menu. */
	@Getter
	public com.booksaw.betterTeams.luniatic.ClanIconos clanIconos;
	/** Bloques minados por cada clan, y lo que cada hito habilita al alcanzarse. */
	@Getter
	public com.booksaw.betterTeams.luniatic.hitos.HitosManager hitosManager;
	public ChatManagement chatManagement;
	public WorldGuardManagerV7 wgManagement;
	@Getter
	private PermissionParentCommand teamCommand;

	@Getter
	private ParentCommand teamaCommand;

	@Getter
	private BooksawCommand teamBooksawCommand;

	@Getter
	private TeamPlaceholders teamPlaceholders;

	@Getter
	ExtensionManager extensionManager;

	/**
	 * FoliaLib instance for Folia/Paper/Spigot support
	 */
	@Getter
	public FoliaLib foliaLib;

	/**
	 * If the ultimateClaims expansion has been enabled
	 */
	@Getter
	private boolean ultimateClaimsEnabled = false;

	private Metrics metrics = null;

	/**
	 * This is used to store the config file in which the the teams data is stored
	 */
	FileConfiguration teams;
	private DamageManagement damageManagement;

	private HomeAnchorManagement homeAnchorManagement;

	private ConfigManager configManager;

	@Getter
	private BukkitAudiences adventure;

	public boolean isAdventure() {
		return adventure != null;
	}

	@Override
	public void onLoad() {
		plugin = this;
		configManager = new ConfigManager("config", true);

		if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null
				&& configManager.config.getBoolean("worldGuard.enabled")) {
			char ver = Bukkit.getPluginManager().getPlugin("WorldGuard").getDescription().getVersion().charAt(0);
			if (ver == '7') {
				wgManagement = new WorldGuardManagerV7();
			} else {
				Main.plugin.getLogger().warning("Your version of worldgaurd ("
						+ Bukkit.getPluginManager().getPlugin("WorldGuard").getDescription().getVersion()
						+ ") is not yet supported (Currently supported: version 7.x.x), the betterteams flags will not be usable");
			}
		}
	}

	@Override
	public void onEnable() {
		foliaLib = new FoliaLib(this);
		setupMetrics();

		if (adventure == null) try {
			adventure = BukkitAudiences.create(this);
		} catch (Exception e) {
			getLogger().severe("Failed to create BukkitAudiences: " + e.getMessage());
			adventure = null;
		}

		MessageManager.setupMessageSender(adventure);

		loadCustomConfigs();

		LevelManager.reload();

		setupStorage();

		ChatManagement.enable();

		setupExtension();

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
				&& Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("PlaceholderAPI")).isEnabled()) {
			placeholderAPI = true;
			teamPlaceholders = new TeamPlaceholders(this);
			teamPlaceholders.register();
		}

		if (Bukkit.getPluginManager().getPlugin("UltimateClaims") != null
				&& Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UltimateClaims")).isEnabled()) {
			if (getConfig().getBoolean("ultimateClaims.enabled")) {
				ultimateClaimsEnabled = true;
				new UltimateClaimsManager();
			}
		}

		useHolograms = setupHolograms();

		if (!setupEconomy() || !getConfig().getBoolean("useVault")) {
			econ = null;
		}

		if (!setupPermissions()) {
			perms = null;
		}

		setupCommands();
		setupListeners();
	}

	@Override
	public void onDisable() {

		// Los duelos en curso se guardan y siguen despues del reinicio: duran dias, y un
		// reinicio no termina un duelo. El pozo queda retenido igual que el duelo.
		if (dueloManager != null) {
			dueloManager.guardarYSoltar();
		}

		// Antes de cancelar las tareas, porque la que vuelca el contador es una de ellas: sin
		// esto se perderia hasta un ciclo entero de bloques minados en cada reinicio.
		if (hitosManager != null) {
			hitosManager.guardarSiCambio();
		}

		if (extensionManager != null) {
			extensionManager.unloadExtensions();
		}

		foliaLib.getScheduler().cancelAllTasks();

		for (Entry<Player, Team> temp : InventoryManagement.adminViewers.entrySet()) {
			temp.getKey().closeInventory();
			temp.getValue().saveEchest();
		}

		// El echest solo se guardaba al cerrarlo, y aca solo se cerraban los que
		// estaba mirando un admin. Un jugador comun con el cofre de su clan abierto
		// cuando se apaga el servidor sacaba items que quedaban en los dos lados:
		// en su inventario, que Minecraft si guarda, y en el cofre, que no se
		// guardaba. Guardar todos al apagar cierra esa ventana.
		for (Team equipo : Team.getTeamManager().getLoadedTeamListClone().values()) {
			try {
				equipo.saveEchest();
			} catch (Exception e) {
				getLogger().warning("No se pudo guardar el cofre de " + equipo.getName() + ": " + e.getMessage());
			}
		}

		if (useHolograms) {
			HologramManager.holoManager.disable();
		}


		if (teamManagement != null) {
			teamManagement.removeAll(false);
			teamManagement = null;
		}

		if (homeAnchorManagement != null) {
			homeAnchorManagement.unregisterEvent();
			homeAnchorManagement = null;
		}

		HandlerList.unregisterAll(this); // unregister all Listeners
		Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(this);

		damageManagement = null;
		chatManagement = null;

		Team.disable();

		if (adventure != null) {
			adventure.close();
			adventure = null;
		}

		configManager = null;
	}

	public void loadCustomConfigs() {

		String language = getConfig().getString("language");
		if (language == null || language.isEmpty() || language.equals("en")) {
			language = "messages";
		}

		File f = new File(getDataFolder(), language + ".yml");

		try {
			if (!f.exists()) {
				saveResource(language + ".yml", false);
			}
		} catch (Exception e) {
			Main.plugin.getLogger().warning("Could not load selected language: " + language
					+ " go to https://betterteams.booksaw.dev/docs/Translations to view a list of supported languages");
			Main.plugin.getLogger().warning("Reverting to english so the plugin can still function");
			language = "messages";
			if (!new File(getDataFolder(), "messages.yml").exists()) {
				saveResource("messages.yml", false);
			}
		}

		MessageManager.addMessages(language);

		if (!language.equals("messages")) {
			ConfigManager messagesConfigManager = new ConfigManager("messages", true);
			MessageManager.addBackupMessages(messagesConfigManager.config);
		}

		if (getConfig().getBoolean("disableCombat")) {
			if (damageManagement == null) {
				damageManagement = new DamageManagement();
				getServer().getPluginManager().registerEvents(damageManagement, this);
			}

		} else {
			if (damageManagement != null) {
				Main.plugin.getLogger().log(Level.WARNING, "Restart server for damage changes to apply");
			}
		}

		// loading the fully custom help message option
		HelpCommand.setupHelp();

	}

	/*
	 * Determines which holograms plugin the server is running, then creates a new
	 * HologramManager instance for the respective plugin.
	 */
	private boolean setupHolograms() {
		boolean hdHolos = Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays");
		if (hdHolos) {
			new HDHologramManager();
		}
		boolean dhHolos = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
		// Check to make sure the server isn't running both hologram plugins.
		// We don't need two HologramManager instances.
		if (!hdHolos && dhHolos) {
			new DHHologramManager();
		}
		return hdHolos || dhHolos;
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return true;
	}

	private boolean setupPermissions() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		if (rsp == null) {
			return false;
		}
		perms = rsp.getProvider();
		return perms != null;
	}

	public void reload() {
		getLogger().info("Starting BetterTeams reload...");

		onDisable();

		reloadConfig();
		configManager = new ConfigManager("config", true);

		onEnable();
		Bukkit.getPluginManager().callEvent(new PostBetterTeamsReloadEvent());

		getLogger().info("BetterTeams reload complete.");
	}

	public void setupCommands() {
		dueloManager = new DueloManager(getConfig().getConfigurationSection("duelo"));
		clanIconos = new com.booksaw.betterTeams.luniatic.ClanIconos();
		hitosManager = new com.booksaw.betterTeams.luniatic.hitos.HitosManager(
				getConfig().getConfigurationSection("hitos"));
		teamCommand = new PermissionParentCommand(new CostManager("team"), new CooldownManager("team"), "team");
		// add all sub commands here
		teamCommand.addSubCommands(new CreateCommand(teamCommand), new LeaveCommand(), new DisbandCommand(),
				new DescriptionCommand(), new InviteCommand(), new JoinCommand(), new NameCommand(), new OpenCommand(),
				new InfoCommand(teamCommand), new KickCommand(), new PromoteCommand(), new DemoteCommand(),
				new HomeCommand(), new SethomeCommand(), new BanCommand(), new UnbanCommand(),
				new ChatCommand(teamCommand), new ColorCommand(), new TitleCommand(), new TopCommand(),
				new BaltopCommand(), new RankCommand(), new DelHome(), new AllyCommand(), new NeutralCommand(),
				new AllyChatCommand(teamCommand), new ListCommand(), new WarpCommand(), new SetWarpCommand(),
				new DelwarpCommand(), new WarpsCommand(), new EchestCommand(), new RankupCommand(), new TagCommand());

		if (getConfig().getBoolean("anchor.enable")) {
			teamCommand.addSubCommands(new AnchorCommand(), new SetAnchorCommand());
		}

		if (getConfig().getBoolean("disableCombat")) {
			teamCommand.addSubCommand(new PvpCommand());
		}

		if (dueloManager.isHabilitado()) {
			teamCommand.addSubCommand(new DueloCommand(dueloManager));
			// Aparte de /team duelo porque es de rango DEFAULT: pactar el duelo es del
			// lider, elegir si la peleas es de cada uno.
			teamCommand.addSubCommand(
					new com.booksaw.betterTeams.luniatic.duelo.PreferenciasCommand(dueloManager));
		}

		teamCommand.addSubCommand(new com.booksaw.betterTeams.luniatic.IconoCommand(clanIconos));
		if (hitosManager.isHabilitado()) {
			// Solo si hay hitos cargados: con el sistema apagado el comando mostraria una
			// lista vacia, que se lee como que algo se rompio.
			teamCommand.addSubCommand(
					new com.booksaw.betterTeams.luniatic.hitos.HitosCommand(hitosManager));
		}
		teamCommand.addSubCommand(new MenuCommand());
		// /team pelado abre el menu en vez de escupir la ayuda.
		teamCommand.setSubcomandoPorDefecto("menu");
		// only used if a team is only allowed a single owner
		if (getConfig().getBoolean("singleOwner")) {
			teamCommand.addSubCommand(new SetOwnerCommand());
		}

		ParentCommand chest = new PermissionParentCommand("chest");
		chest.addSubCommands(new ChestClaimCommand(), new ChestRemoveCommand(), new ChestRemoveallCommand(), new ChestCheckCommand());
		teamCommand.addSubCommand(chest);

		// El comando es /clan, no /team: en Luniatic son clanes, y el nombre en ingles no
		// lo escribe nadie. Los alias salen de command.clan del config.
		teamBooksawCommand = new BooksawCommand("clan", teamCommand, "betterteams.standard", "Comandos de clan",
				getConfig().getStringList("command.clan"));

		teamaCommand = new ParentCommand("clanadmin");

		teamaCommand.addSubCommands(new ReloadTeama(), new ChatSpyTeama(), new TitleTeama(),
				new VersionTeama("version"), new VersionTeama("debug"), new HomeTeama(), new NameTeama(),
				new DescriptionTeama(), new OpenTeama(), new InviteTeama(), new CreateTeama(), new JoinTeama(),
				new LeaveTeama(), new PromoteTeama(), new DemoteTeama(), new WarpTeama(), new SetwarpTeama(),
				new DelwarpTeama(), new PurgeTeama(), new DisbandTeama(), new ColorTeama(), new EchestTeama(),
				new SetrankTeama(teamaCommand), new TagTeama(), new TeleportTeama(teamaCommand), new AllyTeama(),
				new NeutralTeama(), new ImportmessagesTeama());


		if (dueloManager != null && dueloManager.isHabilitado()) {
			// Herramienta de staff: sumar a alguien al duelo que ya empezo, para el que
			// se olvido de anotarse. Los participantes se congelan al arrancar a proposito,
			// asi que sin esto la unica salida seria esperar a la proxima.
			teamaCommand.addSubCommand(
					new com.booksaw.betterTeams.luniatic.duelo.DueloTeama(dueloManager));
		}

		if (hitosManager.isHabilitado()) {
			// Sin esto no hay forma de probar un desbloqueo salvo minando cien mil bloques.
			teamaCommand.addSubCommand(
					new com.booksaw.betterTeams.luniatic.hitos.HitosTeama(hitosManager));
		}

		if (getConfig().getBoolean("anchor.enable")) {
			teamaCommand.addSubCommands(new AnchorTeama(), new SetAnchorTeama());
		}

		if (getConfig().getBoolean("singleOwner")) {
			teamaCommand.addSubCommand(new SetOwnerTeama());
		}

		ParentCommand teamaScoreCommand = new ParentCommand("score");
		teamaScoreCommand.addSubCommands(new AddScore(), new SetScore(), new RemoveScore());
		teamaCommand.addSubCommand(teamaScoreCommand);

		ParentCommand teamaMoneyCommand = new ParentCommand("money");
		teamaMoneyCommand.addSubCommands(new AddMoney(), new SetMoney(), new RemoveMoney());
		teamaCommand.addSubCommand(teamaMoneyCommand);

		ParentCommand teamaChestCommand = new ParentCommand("chest");
		teamaChestCommand.addSubCommands(new ChestClaimTeama(), new ChestRemoveTeama(), new ChestRemoveallTeama(),
				new ChestEnableClaims(), new ChestDisableClaims());
		teamaCommand.addSubCommand(teamaChestCommand);

		ParentCommand teamaMetaCommand = new ParentCommand("meta");
		teamaMetaCommand.addSubCommands(new MetaSetTeama(), new MetaGetTeama(), new MetaRemoveTeama());
		teamaCommand.addSubCommand(teamaMetaCommand);

		if (useHolograms) {
			ParentCommand teamaHoloCommand = new ParentCommand("holo");
			teamaHoloCommand.addSubCommands(new CreateHoloTeama(), new RemoveHoloTeama());
			teamaCommand.addSubCommand(teamaHoloCommand);
		}

		if (econ != null) {
			teamCommand.addSubCommands(new DepositCommand(teamCommand), new BalCommand(),
					new WithdrawCommand(teamCommand));
		}

		new BooksawCommand("clanadmin", teamaCommand, "betterteams.admin", "Comandos de clan para el staff",
				getConfig().getStringList("command.clanadmin"));

	}

	public void setupListeners() {
		Main.plugin.getLogger().info("Display team name config value: " + getConfig().getString("displayTeamName"));
		BelowNameType type = BelowNameType.getType(Objects.requireNonNull(getConfig().getString("displayTeamName")));
		Main.plugin.getLogger().info("Loading below name. Type: " + type);
		if (getConfig().getBoolean("useTeams")) {
			if (foliaLib.isFolia()) {
				Bukkit.getLogger().warning("Folia detected: Skipping MCTeamManagement initialization to avoid threading issues.");
			} else if (teamManagement == null) {
				teamManagement = new MCTeamManagement(type);

				Main.plugin.getFoliaLib().getScheduler().runAsync(task -> teamManagement.displayBelowNameForAll());
				getServer().getPluginManager().registerEvents(teamManagement, this);
				Main.plugin.getLogger().info("teamManagement declared: " + teamManagement);
			} else {
				Main.plugin.getLogger().info("Not loading management");
				if (teamManagement != null) {
					Main.plugin.getLogger().log(Level.WARNING, "Restart server for minecraft team changes to apply");
				}
			}
		}


		if (hitosManager != null && hitosManager.isHabilitado()) {
			getServer().getPluginManager().registerEvents(
					new com.booksaw.betterTeams.luniatic.hitos.HitosListener(hitosManager), this);
			// El contador se toca en memoria en cada bloque roto; a disco baja una sola vez
			// cada tantos segundos, y solo si cambio algo. Escribir por bloque seria un
			// archivo entero reescrito varias veces por segundo.
			long periodo = hitosManager.getGuardadoSegundos() * 20L;
			foliaLib.getScheduler().runTimer(task -> hitosManager.guardarSiCambio(), periodo, periodo);
		}

		if (dueloManager != null && dueloManager.isHabilitado()) {
			// La guarda de regiones se crea una sola vez y la comparten todas las reglas del
			// duelo: el destapado de dano, el corte de vuelo, la regla de aparicion y el
			// cartel del scoreboard. Sin WorldGuard queda en null y cada regla que la
			// necesite se apaga sola.
			if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
				dueloManager.setGuardia(new com.booksaw.betterTeams.luniatic.duelo.GuardiaRegion(
						dueloManager.isPisaClaims()));
			}
			// Una sola tarea cada 10 s para los vencimientos: nada por jugador ni por tick.
			foliaLib.getScheduler().runTimer(task -> dueloManager.revisar(), 200L, 200L);
			getServer().getPluginManager().registerEvents(new DueloDeathListener(dueloManager), this);
			// La advertencia y el libro de los que estaban desconectados cuando empezo la
			// duelo. Se registra siempre: puede haber pendientes de una sesion anterior.
			getServer().getPluginManager().registerEvents(
					new com.booksaw.betterTeams.luniatic.duelo.DueloEntradaListener(dueloManager), this);
			if (dueloManager.hayBloqueosDeAparicion()) {
				// Impide volver al lado de una base rival: teletransporte, cama y /sethome.
				getServer().getPluginManager().registerEvents(
						new com.booksaw.betterTeams.luniatic.duelo.DueloAparicionListener(dueloManager), this);
			}
			if (dueloManager.isPisaPvpIndividual()) {
				// Solo el listener de dano. El duelo ya no prende ni bloquea el toggle de
				// PvPManager: destapar el dano entre rivales alcanza, y el /pvp del
				// jugador queda diciendo la verdad sobre lo que el eligio.
				DueloDamageListener dano = new DueloDamageListener(dueloManager);
				getServer().getPluginManager().registerEvents(dano, this);
				if (dueloManager.isDebug()) {
					// El vigilante de la ultima palabra solo sirve para depurar y se cuelga del
					// evento de dano sin ignoreCancelled: registrarlo siempre seria un despacho
					// mas por cada golpe del servidor, granjas incluidas.
					getServer().getPluginManager().registerEvents(dano.vigilante(), this);
				}
			}
			if (dueloManager.hayComandosBloqueados()) {
				// Corta /dback y /back en duelo. Al morir se pierde el tag de combate de
				// PvPManager, asi que sin esto se vuelve al punto de la pelea al instante.
				getServer().getPluginManager().registerEvents(new DueloComandoListener(dueloManager), this);
			}
			if (dueloManager.isBarraActiva()) {
				DueloBossBar barra = new DueloBossBar(dueloManager);
				dueloManager.setBarra(barra);
				getServer().getPluginManager().registerEvents(barra, this);
			}
			// Ultimo, con la barra ya puesta: los duelos que sobrevivieron al reinicio
			// tienen que volver con su barra dibujada, no aparecer recien en la primera
			// pelea.
			dueloManager.cargar();
			getLogger().info("Duelos pactados: activos"
					+ (dueloManager.isPisaPvpIndividual() ? ", con override de PvP." : "."));
		}

		// 🔑 La expansion de placeholders se registra SIEMPRE, y esto estaba adentro del
		// bloque de arriba. O sea que apagar los duelos —que es como se planea abrir— se
		// llevaba puesta la expansion entera, y con ella dos cosas que no son del duelo:
		// la linea de PvP del scoreboard, que la ve todo el servidor, y la etiqueta del
		// clan del nombre flotante. Las dos habrian salido escritas como %placeholder%.
		// La clase ya se banca los duelos apagados: corta en isHabilitado().
		if (placeholderAPI && dueloManager != null) {
			new DueloPlaceholders(dueloManager).register();
		}

		// Icono y color propios para cada clan nuevo, sin que nadie los elija.
		getServer().getPluginManager().registerEvents(
				new com.booksaw.betterTeams.luniatic.ClanIdentidad(clanIconos), this);

		// Permiso luniatic.clan para los que estan en un clan. Es lo que deja gatear las
		// misiones de clan de Quests, que solo sabe pedir permisos.
		com.booksaw.betterTeams.luniatic.ClanPermiso clanPermiso =
				new com.booksaw.betterTeams.luniatic.ClanPermiso(this);
		getServer().getPluginManager().registerEvents(clanPermiso, this);
		clanPermiso.actualizarATodos();

		// Expone los clanes como "party" para que Quests comparta el progreso de una mision
		// entre los miembros. Quests lo busca por este tipo en el ServicesManager.
		//
		// Se avisa por consola a proposito: si esto falla, las misiones de clan vuelven a ser
		// individuales sin ningun error visible. Ver docs/clanes.md 4.1.
		try {
			getServer().getServicesManager().register(
					me.pikamug.unite.api.objects.PartyProvider.class,
					new com.booksaw.betterTeams.luniatic.ClanPartyProvider(this),
					this, org.bukkit.plugin.ServicePriority.Normal);
			getLogger().info("Clanes expuestos como party: las misiones de clan comparten progreso.");
		} catch (Throwable t) {
			getLogger().severe("No se pudo registrar el proveedor de party: las misiones de "
					+ "clan van a contar por jugador y no por clan. Causa: " + t);
		}

		getServer().getPluginManager().registerEvents(new MenuListener(), this);
		// Respuestas por chat cuando el menu pide un texto: nombre, etiqueta, descripcion.
		getServer().getPluginManager().registerEvents(
				new com.booksaw.betterTeams.luniatic.gui.EsperaTexto(), this);

		getServer().getPluginManager().registerEvents((chatManagement = new ChatManagement()), this);
		getServer().getPluginManager().registerEvents(new ScoreManagement(), this);
		getServer().getPluginManager().registerEvents(new AllyManagement(), this);
		getServer().getPluginManager().registerEvents(new MessagesManagement(), this);

		if (getConfig().getBoolean("checkUpdates")) {
			getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);
		}

		// disabling the chest checks (hoppers most importantly) to reduce needless
		// performance cost
		if (teamCommand.isEnabled("chest")) {
			getServer().getPluginManager().registerEvents(new ChestManagement(), this);
		}

		getServer().getPluginManager().registerEvents(new InventoryManagement(), this);
		getServer().getPluginManager().registerEvents(new RankupEvents(), this);
		if (getConfig().getBoolean("anchor.enable")) {
			homeAnchorManagement = new HomeAnchorManagement(this);
			homeAnchorManagement.registerEvent();
		}

		if (getConfig().getBoolean("apollo.teamview.enabled", true)) {
			new ApolloManager();
		}
	}

	public void setupMetrics() {
		if (metrics == null) {
			int pluginId = 7855;
			metrics = new Metrics(this, pluginId);
			metrics.addCustomChart(new SimplePie("language", () -> getConfig().getString("language")));
			metrics.addCustomChart(new SimplePie("storage_type", () -> getConfig().getString("storageType")));
			metrics.addCustomChart(new SimplePie("team_count", () -> {
				if (Team.getTeamManager() instanceof SeparatedYamlStorageManager) {
					return ((((SeparatedYamlStorageManager) Team.getTeamManager()).getTeamNameLookupSize() / 200) * 200) + "+";

				}
				return null;
			}));
			metrics.addCustomChart(new SimplePie("player_count", () -> ((Bukkit.getOnlinePlayers().size() / 20) * 20) + "+"));
		}
	}

	@Override
	public @NotNull FileConfiguration getConfig() {
		return configManager.config;
	}

	public void setupStorage() {
		File f = new File("plugins/BetterTeams/" + YamlStorageManager.TEAMLISTSTORAGELOC + ".yml");

		if (!f.exists()) {
			Main.plugin.saveResource("teams.yml", false);
		}

		YamlConfiguration teamStorage = YamlConfiguration.loadConfiguration(f);
		StorageType from = StorageType.getStorageType(teamStorage.getString("storageType", "FLATFILE"));
		StorageType to = StorageType.getStorageType(getConfig().getString("storageType", ""));

		if (from != to) {
			Converter converter = Converter.getConverter(from, to);

			if (converter == null) {
				Main.plugin.getLogger().info("Cannot convert to the selected storage type (" + to.toString()
						+ "), continuing with preexisting one (" + from.toString() + ")");
				to = from;
			} else {
				converter.convertStorage();
			}
		}

		Team.setupTeamManager(to);
		Team.getTeamManager().loadTeams();
	}

	public void setupExtension() {
		extensionManager = new ExtensionManager(this, new File(getDataFolder(), "extensions"));
		extensionManager.initializeExtensions();

		int enableTick = getConfig().getInt("extension.enableTick", 1);
		if (enableTick <= 0) {
			extensionManager.enableExtensions();
		} else {
			// Run later
			foliaLib.getScheduler().runLater(() -> {
				extensionManager.enableExtensions();
			}, enableTick);
		}
	}
}
