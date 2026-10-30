package rtc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    // damn what a honor to put a mixin directly into Minecraft class
    @Inject(method = "setWindowActive", at = @At("HEAD"), cancellable = true)
    private void preventFocusLoss(boolean active, CallbackInfo ci) {
        if (!active) {
            ci.cancel();
        }
    }
}