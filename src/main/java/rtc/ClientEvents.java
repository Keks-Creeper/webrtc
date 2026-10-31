package rtc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;

@EventBusSubscriber(modid = WebRTCMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    public static boolean mouseInside = false;
    public static boolean superdupershit = false;
    public static boolean reallyActive = false;

    private static long videoframett = 0;

    @SubscribeEvent
    public static void onRender(RenderFrameEvent.Post event) {
        if (Config.VIDEOGET.get()) {

            long now = System.nanoTime();

            if (Config.FPS.get() != 0 && now - videoframett < 1_000_000_000L / Config.FPS.get()) {
                return;
            }

            videoframett = now;

            NativeImage image = Screenshot.takeScreenshot(
                    Minecraft.getInstance().getMainRenderTarget());

            VideoStreamer.submit(image, now);

            image.close();
        }
    }

    private static boolean ingamett = false;
    private static int mausmstt = -1;
    public static String title = "";
    private static String titlett = "";

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        InputSimulator.tick();
        JsonObject json = new JsonObject();

        if (Config.MAUSMS.get() != mausmstt) {
            mausmstt = Config.MAUSMS.get();
            json.addProperty("mausms", Config.MAUSMS.get());
        }

        Minecraft mc = Minecraft.getInstance();
        boolean ingame = mc.screen == null;
        if (ingame != ingamett) {
            ingamett = ingame;
            json.addProperty("ingame", ingame);
        }

        if (!title.equals(titlett)) {
            titlett = title;
            json.addProperty("title", title);
        }

        if (!json.isEmpty()) {
            WebRTCServer.sendToAllClients(json.toString());
        }
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
        if (Config.WAIT_FOR_CLOSE.get())
            WebRTCServer.stop();
        else if (Config.RELOAD_ON_STOP.get())
            WebRTCServer.sendToAllClients("{\"reload\":0}");
    }
}