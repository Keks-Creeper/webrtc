package rtc.mixin;

import com.mojang.blaze3d.platform.InputConstants;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.MouseHandler;
import rtc.WebRTCServer;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Redirect(method = "grabMouse", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(JIDD)V"))
    private void rtc$grabMouse(
            long window,
            int mode,
            double x,
            double y) {
        if (!WebRTCServer.clients()) {
            InputConstants.grabOrReleaseMouse(window, mode, x, y);
        }
    }

    @Redirect(method = "releaseMouse", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(JIDD)V"))
    private void rtc$releaseMouse(
            long window,
            int mode,
            double x,
            double y) {
        if (!WebRTCServer.clients()) {
            InputConstants.grabOrReleaseMouse(window, mode, x, y);
        }
    }
}
