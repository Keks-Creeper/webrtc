package rtc;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

import com.mojang.blaze3d.platform.Window;
import rtc.Config.When;
import rtc.mixin.MouseHandlerAccessor;

@EventBusSubscriber(value = Dist.CLIENT)
public class Overlay {
    private static final int X = 3;
    private static final int Y = 10;
    public static String LINK = null;
    public static ScreenRectangle LINK_BOUNDS;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || damn_is(Config.HIDEIP))
            return;

        boolean hovered = inside(event.getMouseX(), event.getMouseY());
        event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                Component.literal(link()).withStyle(style -> style
                        .withUnderlined(true) // damn color vvv
                        .withColor(WebRTCServer.started.getAcquire()
                                ? (WebRTCServer.OPEN ? (hovered ? 0x00cc00 : 0x005500)
                                        : (hovered ? 0xcc0000 : 0x550000))
                                : 0xaaaaaa)),
                X, Y, 0xFFFFFF, false);

        if (hovered) {
            event.getGuiGraphics().renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("rtc.click"),
                    event.getMouseX(),
                    event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() == 0 && inside(event.getMouseX(), event.getMouseY()) && !InputSimulator.remoteClick) {
            Util.getPlatform().openUri("http://" + link());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void addButtons(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !damn_is(Config.RESTART_BUTTON))
            return;
        link();
        if (LINK_BOUNDS != null)
            event.addListener(Button.builder(
                    Component.translatable(WebRTCServer.started.getAcquire() ? "rtc.restart" : "rtc.start"), button -> {
                        WebRTCServer.sendToAllClients("{\"to\":\" \"}");
                        WebRTCServer.OPEN = false;
                        WebRTCServer.restart();
                    })
                    .bounds(X - 1, LINK_BOUNDS.bottom(), 70, 20).build());
    }

    // helper functions
    public static boolean inside(double x, double y) {
        if (Minecraft.getInstance().screen instanceof TitleScreen && LINK_BOUNDS instanceof ScreenRectangle) {
            return LINK_BOUNDS.containsPoint((int) x, (int) y);
        }
        return false;
    }

    public static boolean inside(MouseHandlerAccessor mouse) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        return inside(
                mouse.rtc$getXpos() * window.getGuiScaledWidth() / window.getScreenWidth(),
                mouse.rtc$getYpos() * window.getGuiScaledHeight() / window.getScreenHeight());
    }

    public static boolean damn_is(EnumValue<When> when) {
        return switch (when.get()) {
            case ALWAYS -> true;
            case CLIENTS_CONNECTED -> WebRTCServer.clients();
            case NO_CLIENTS_CONNECTED -> !WebRTCServer.clients();
            default -> false;
        };
    }

    private static String link() {
        if (LINK instanceof String && LINK_BOUNDS instanceof ScreenRectangle)
            return LINK;
        try {
            String LINK = (Config.PUBLIC.get() ? WebRTCServer.PUBLIC_IP : "127.0.0.1")
                    + (Config.PORT.get() == 80 ? "" : ":" + Config.PORT.get());
            Font font = Minecraft.getInstance().font;
            LINK_BOUNDS = new ScreenRectangle(X, Y, font.width(LINK), font.lineHeight);
            return LINK;
        } catch (Exception e) {
            return "";
        }
    }
}