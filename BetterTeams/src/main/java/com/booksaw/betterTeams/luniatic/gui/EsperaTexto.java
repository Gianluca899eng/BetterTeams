package com.booksaw.betterTeams.luniatic.gui;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.luniatic.Texto;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Pide un texto por el chat y devuelve la respuesta.
 *
 * <p><b>Por chat y no por yunque, a proposito.</b> Los menus tipo yunque son de lo que
 * peor funciona por Geyser, y Bedrock es una parte real del publico. Es el mismo
 * criterio que ya tenia el buscador de clanes, que pide el texto como argumento del
 * comando en vez de abrir una pantalla de escritura.
 *
 * <p>El menu se cierra antes de preguntar: dejarlo abierto tapa el chat justo cuando el
 * jugador tiene que leer la consigna.
 *
 * <p>Se cancela sola a los 60 segundos y cuando el jugador se desconecta, para que una
 * espera olvidada no se coma el proximo mensaje que escriba.
 */
public final class EsperaTexto implements Listener {

    private static final int SEGUNDOS = 60;
    private static final String CANCELAR = "cancelar";

    private static final String MARCA = "&#9235FF";
    private static final String CUERPO = "&#E4D9FF";
    private static final String ETIQUETA = "&#7162FF";

    private static final Map<UUID, Pendiente> PENDIENTES = new ConcurrentHashMap<>();

    private static final class Pendiente {
        private final Consumer<String> alRecibir;
        private final Consumer<Player> alCancelar;
        private volatile boolean vencida;

        private Pendiente(Consumer<String> alRecibir, Consumer<Player> alCancelar) {
            this.alRecibir = alRecibir;
            this.alCancelar = alCancelar;
        }
    }

    /**
     * Cierra el menu y espera una linea de chat.
     *
     * @param consigna   que tiene que escribir, en una linea
     * @param alRecibir  corre en el hilo principal, con lo que escribio
     * @param alCancelar corre en el hilo principal si cancela o se vence
     */
    public static void pedir(Player jugador, String consigna, Consumer<String> alRecibir,
                             Consumer<Player> alCancelar) {
        jugador.closeInventory();
        PENDIENTES.remove(jugador.getUniqueId());

        Pendiente pendiente = new Pendiente(alRecibir, alCancelar);
        PENDIENTES.put(jugador.getUniqueId(), pendiente);

        // FoliaLib y no el scheduler de Bukkit, que es lo que usa el resto del plugin.
        Main.plugin.getFoliaLib().getScheduler().runLater(task -> {
            if (!pendiente.vencida && PENDIENTES.remove(jugador.getUniqueId()) == pendiente) {
                aviso(jugador, CUERPO + "Se acabo el tiempo para escribir.");
                alCancelar.accept(jugador);
            }
        }, SEGUNDOS * 20L);

        aviso(jugador, CUERPO + consigna);
        aviso(jugador, ETIQUETA + "Escribi " + MARCA + CANCELAR + ETIQUETA + " para dejarlo como esta.");
    }

    // El evento de chat es async, asi que todo lo que abra un inventario o toque el
    // mundo vuelve al hilo principal.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void alEscribir(AsyncPlayerChatEvent evento) {
        Player jugador = evento.getPlayer();
        Pendiente pendiente = PENDIENTES.remove(jugador.getUniqueId());
        if (pendiente == null) {
            return;
        }
        // Que no salga al chat: es una respuesta al menu, no un mensaje.
        evento.setCancelled(true);
        pendiente.vencida = true;

        String texto = evento.getMessage().trim();
        Main.plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            if (texto.isEmpty() || texto.equalsIgnoreCase(CANCELAR)) {
                aviso(jugador, CUERPO + "Listo, no se cambio nada.");
                pendiente.alCancelar.accept(jugador);
            } else {
                pendiente.alRecibir.accept(texto);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void alSalir(PlayerQuitEvent evento) {
        Pendiente pendiente = PENDIENTES.remove(evento.getPlayer().getUniqueId());
        if (pendiente != null) {
            pendiente.vencida = true;
        }
    }

    private static void aviso(Player jugador, String texto) {
        jugador.sendMessage(Texto.col(ETIQUETA + "[Clanes] " + texto));
    }
}
