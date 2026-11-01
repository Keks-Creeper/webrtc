package rtc;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);

    public enum When {
        CLIENTS_CONNECTED, NO_CLIENTS_CONNECTED, ALWAYS, NEVER
    }

    public static final ModConfigSpec.BooleanValue PUBLIC;
    public static final ModConfigSpec.IntValue PORT;
    public static final ModConfigSpec.ConfigValue<String> TITLE;
    public static final ModConfigSpec.ConfigValue<String> STUN_URL;
    public static final ModConfigSpec.ConfigValue<String> QUIT_GAME_PAGE;
    //public static final ModConfigSpec.ConfigValue<String> E403_MESSAGE;
    public static final ModConfigSpec.BooleanValue AUTO_START;

    public static final ModConfigSpec.BooleanValue VIDEOGET;
    public static final ModConfigSpec.BooleanValue VIDEOSEND;
    public static final ModConfigSpec.IntValue FPS;
    public static final ModConfigSpec.IntValue RES;
    public static final ModConfigSpec.IntValue BITRATE;

    public static final ModConfigSpec.BooleanValue AUDIOGET;
    public static final ModConfigSpec.BooleanValue AUDIOSEND;
    public static final ModConfigSpec.IntValue AUDIOQUEUE;

    public static final ModConfigSpec.IntValue MAUSMS;
    public static final ModConfigSpec.EnumValue<When> HIDEIP;
    public static final ModConfigSpec.EnumValue<When> RESTART_BUTTON;

    static {
        PUBLIC = BUILDER.define("public", true);
        PORT = BUILDER.defineInRange("port", os.contains("linux") ? 8080 : 80, 1, 65535);
        STUN_URL = BUILDER.define("stun_url", "stun:stun.l.google.com:19302");
        TITLE = BUILDER.define("title", "WebRTC *");
        QUIT_GAME_PAGE = BUILDER.define("quit_page", " ");
        //E403_MESSAGE = BUILDER.define("e403_msg", "You are not whitelisted!");
        AUTO_START = BUILDER.define("autostart", true);

        BUILDER.push("video");
        VIDEOGET = BUILDER.define("videoget", true);
        VIDEOSEND = BUILDER.define("videosend", true);
        FPS = BUILDER.defineInRange("fps", 60, 0, 240);
        RES = BUILDER.defineInRange("res", 1, 1, 16);
        BITRATE = BUILDER.defineInRange("bitrate", 6_000_000, 0, 50_000_000);
        BUILDER.pop();

        BUILDER.push("audio");
        AUDIOGET = BUILDER.define("audioget", true);
        AUDIOSEND = BUILDER.define("audiosend", true);
        AUDIOQUEUE = BUILDER.defineInRange("audioqueue", 5, 1, 20);
        BUILDER.pop();

        BUILDER.push("input");
        MAUSMS = BUILDER.defineInRange("mausms", 12, 0, 100);
        HIDEIP = BUILDER.defineEnum("hideip", When.CLIENTS_CONNECTED);
        RESTART_BUTTON = BUILDER.defineEnum("restartbutton", When.ALWAYS);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}