package at.lukas.misc;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MotdListener implements Listener {
    private final String motd;

    public MotdListener(FileConfiguration config) {
        motd = config.getString("motd");
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        if (motd == null) {
            return;
        }

        Component colorCodedMotd = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(motd);

        event.motd(colorCodedMotd);
    }
}