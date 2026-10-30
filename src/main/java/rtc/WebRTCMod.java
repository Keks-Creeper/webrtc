package rtc;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = WebRTCMod.MODID, dist = Dist.CLIENT)
public class WebRTCMod {
    public static final String MODID = "webrtc";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WebRTCMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //NeoForge.EVENT_BUS.register(this);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, Config.SPEC,
                "webrtc/client.toml");
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modEventBus.addListener(this::onConfigLoading);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (FMLEnvironment.dist == Dist.CLIENT && Config.AUTO_START.get()) {
            LOGGER.info("Starting the RTC");
            WebRTCServer.start();
        }
    }
}
