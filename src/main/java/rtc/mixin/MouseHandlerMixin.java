package rtc.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtc.ClientEvents;
import rtc.InputSimulator;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Redirect(method = "grabMouse", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(JIDD)V"))
    private void rtc$grabMouse(
            long window,
            int mode,
            double x,
            double y) {
        if (ClientEvents.superdupershit && ClientEvents.mouseInside && ClientEvents.reallyActive) {
            InputConstants.grabOrReleaseMouse(window, mode, x, y);
        } else {
            ClientEvents.superdupershit = false;
        }
    }

    @Redirect(method = "releaseMouse", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(JIDD)V"))
    private void rtc$releaseMouse(
            long window,
            int mode,
            double x,
            double y) {
        if (ClientEvents.superdupershit && ClientEvents.mouseInside && ClientEvents.reallyActive) {
            InputConstants.grabOrReleaseMouse(window, mode, x, y);
        } else {
            ClientEvents.superdupershit = false;
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().getWindow())
            return;

        if (action == GLFW.GLFW_PRESS && !InputSimulator.remoteClick) {
            ClientEvents.superdupershit = true;
        }
    }
}
