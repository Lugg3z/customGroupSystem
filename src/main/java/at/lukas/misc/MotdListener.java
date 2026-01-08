package at.lukas.misc;

import at.lukas.CustomGroupSystem;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MotdListener implements Listener {
    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        String motd = JavaPlugin.getPlugin(CustomGroupSystem.class).getConfig().getString("motd");

        if (motd == null) {
            return;
        }

        Component colorCodedMotd = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(motd);

        event.motd(colorCodedMotd);
    }
}