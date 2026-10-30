package rtc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;

public final class IPHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("WebRTC-IPLogger");

    private static final Path FILE = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config/webrtc/ips.log");

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    static {
        try {
            Files.createDirectories(FILE.getParent());

            if (Files.exists(FILE)) {
                SEEN.addAll(Files.readAllLines(
                        FILE, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOGGER.error("Could not load seen IPs", e);
        }
    }

    private IPHandler() {
    }

    public static synchronized void record(String ip) {
        if (ip == null || ip.isBlank())
            return;

        // Remove IPv6 brackets, if present.
        if (ip.startsWith("[") && ip.endsWith("]")) {
            ip = ip.substring(1, ip.length() - 1);
        }

        if (!SEEN.add(ip))
            return;

        try {
            Files.writeString(
                    FILE,
                    ip + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            LOGGER.info("Recorded a new connecting IP");
        } catch (IOException e) {
            SEEN.remove(ip);
            LOGGER.error("Could not save connecting IP", e);
        }
    }
}
