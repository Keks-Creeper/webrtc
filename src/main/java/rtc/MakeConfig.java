package rtc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WebRTCMod.MODID, dist = Dist.CLIENT)
//@EventBusSubscriber(modid = RtcMod.MODID, value = Dist.CLIENT)
public class MakeConfig {
    public MakeConfig(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
