package rtc.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtc.ClientEvents;
import rtc.Config;

@Mixin(Window.class)
public class WindowMixin {

    private static boolean rtc$cursorCallbackRegistered = false;

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void rtc$onTitleChanged(String title, CallbackInfo ci) {
        ClientEvents.title = Config.TITLE.get().replace("*", title);
    }

    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void rtc$registerCursorCallback(CallbackInfo ci) {
        if (rtc$cursorCallbackRegistered)
            return;

        @SuppressWarnings("resource")
        Window window = (Window) (Object) this;

        GLFW.glfwSetCursorEnterCallback(window.getWindow(), (win, entered) -> {
            ClientEvents.mouseInside = entered;
        });

        rtc$cursorCallbackRegistered = true;
    }
}