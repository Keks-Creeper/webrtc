package rtc;

import com.mojang.logging.LogUtils;

import org.lwjgl.openal.SOFTLoopback;
import org.slf4j.Logger;

public final class AudioLoopback {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static long device;

    private AudioLoopback() {
    }

    public static long openDevice() {
        device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((String) null);
        LOGGER.info("OPENED LOOPBACK DEVICE = {}", device);
        if (device == 0L)
            throw new IllegalStateException("Failed to open OpenAL loopback device");
        return device;
    }

    public static long getDevice() {
        return device;
    }
}