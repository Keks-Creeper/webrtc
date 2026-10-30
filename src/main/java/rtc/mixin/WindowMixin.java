package rtc.mixin;

import com.mojang.blaze3d.platform.Window;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import rtc.ClientEvents;
import rtc.Config;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void rtc$onTitleChanged(String title, CallbackInfo ci) {
        ClientEvents.title = Config.TITLE.get().replace("*", title);
    }
}