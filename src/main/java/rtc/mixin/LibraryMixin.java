package rtc.mixin;

import java.nio.IntBuffer;

import com.mojang.blaze3d.audio.Library;

import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import rtc.AudioCapture;
import rtc.AudioLoopback;
import rtc.AudioStreamer;
import rtc.Config;

@Mixin(Library.class)
public class LibraryMixin {

    @Redirect(method = "tryOpenDevice", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcOpenDevice(Ljava/lang/CharSequence;)J"))
    private static long rtc$openLoopbackDevice(CharSequence deviceName) {
        return AudioLoopback.openDevice();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Library;setHrtf(Z)V"))
    private void rtc$disableHrtf(Library instance, boolean enable) {
        // Loopback devices don't need Minecraft's HRTF reset.
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcCreateContext(JLjava/nio/IntBuffer;)J"))
    private long rtc$createLoopbackContext(long device, IntBuffer ignoredAttributes) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer attrs = stack.mallocInt(7);

            attrs.put(SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT);
            attrs.put(SOFTLoopback.ALC_STEREO_SOFT);

            attrs.put(SOFTLoopback.ALC_FORMAT_TYPE_SOFT);
            attrs.put(SOFTLoopback.ALC_SHORT_SOFT);

            attrs.put(ALC10.ALC_FREQUENCY);
            attrs.put(48000);

            attrs.put(0);
            attrs.flip();

            long context = ALC10.alcCreateContext(device, attrs);

            AudioLoopback.LOGGER.info("CREATED LOOPBACK CONTEXT = {}", context);

            return context;
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rtc$debugOpenAL(String deviceSpecifier, boolean enableHrtf, CallbackInfo ci) {
        if (Config.AUDIOGET.get()) {
            AudioCapture.start();
        }
    }

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void rtc$stopAudioCapture(CallbackInfo ci) {
        if (Config.AUDIOGET.get()) {
            AudioCapture.stop();
            AudioStreamer.stop();
        }
    }
}