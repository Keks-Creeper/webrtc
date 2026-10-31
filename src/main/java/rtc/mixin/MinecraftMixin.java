package rtc.mixin;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtc.ClientEvents;

// damn what a honor to put a mixin directly into Minecraft class
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setWindowActive", at = @At("HEAD"), cancellable = true)
    private void preventFocusLoss(boolean active, CallbackInfo ci) {
        ClientEvents.reallyActive = active;
        if (!active) {
            ci.cancel();
        }
    }
}