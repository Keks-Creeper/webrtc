package rtc;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import com.mojang.logging.LogUtils;

import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public final class AudioCapture {

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 2;
    private static final int FRAMES = 480;
    public static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean running;
    private static Thread thread;

    private AudioCapture() {
    }

    public static synchronized void start() {
        if (running) {
            return;
        }

        running = true;

        thread = new Thread(
                AudioCapture::run,
                "Minecraft Audio Capture");

        thread.setDaemon(true);
        thread.start();
    }

    public static synchronized void stop() {
        LOGGER.info("Audio Capture stopped");
        running = false;
        thread = null;
    }

    private static void run() {
        long device = AudioLoopback.getDevice();

        if (device == 0L) {
            LOGGER.error("Audio capture started BEFORE loopback device was opened!");
            return;
        } else {
            LOGGER.info("Audio Capture started");
        }

        ByteBuffer raw = MemoryUtil.memAlloc(FRAMES * CHANNELS * 2);
        ShortBuffer buffer = raw.asShortBuffer();

        byte[] pcm = new byte[FRAMES * CHANNELS * 2];

        final long frameNanos = (long) ((1_000_000_000.0 * FRAMES) / SAMPLE_RATE);

        long next = System.nanoTime();
        try {
            while (running) {
                buffer.clear();

                SOFTLoopback.alcRenderSamplesSOFT(
                        device,
                        buffer,
                        FRAMES);

                buffer.position(FRAMES * CHANNELS);
                buffer.flip();

                int pos = 0;

                while (buffer.hasRemaining()) {
                    short sample = buffer.get();
                    pcm[pos++] = (byte) (sample & 0xFF);
                    pcm[pos++] = (byte) ((sample >>> 8) & 0xFF);
                }

                AudioStreamer.pushAudio(
                        pcm,
                        SAMPLE_RATE,
                        CHANNELS);

                next += frameNanos;

                long sleep = next - System.nanoTime();

                if (sleep > 0) {
                    try {
                        Thread.sleep(
                                sleep / 1_000_000,
                                (int) (sleep % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // We fell behind. Don't accumulate infinite delay.
                    next = System.nanoTime();
                }
            }
        } finally {
            MemoryUtil.memFree(raw);
        }
    }
}