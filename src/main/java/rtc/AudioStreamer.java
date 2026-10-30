package rtc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import dev.onvoid.webrtc.media.audio.CustomAudioSource;

public final class AudioStreamer {

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final BlockingQueue<AudioPacket> QUEUE = new ArrayBlockingQueue<>(Config.AUDIOQUEUE.get());
    private static volatile boolean running = false;
    private static Thread worker;

    private AudioStreamer() {
    }

    private static CustomAudioSource source;

    public static synchronized CustomAudioSource getSource() {
        if (source == null) {
            source = new CustomAudioSource();
        }

        return source;
    }

    /**
     * Start the single audio worker.
     *
     * Safe to call multiple times.
     */
    public static synchronized void start() {
        if (running) {
            return;
        }

        running = true;

        worker = new Thread(
                AudioStreamer::runWorker,
                "WebRTC Audio Worker");

        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Stop the audio worker and clear queued audio.
     */
    public static synchronized void stop() {

        if (!running) {
            return;
        }

        running = false;

        if (worker != null) {
            worker.interrupt();
            worker = null;
        }

        QUEUE.clear();
    }

    /**
     * Called by AudioCapture.
     *
     * IMPORTANT:
     * This method does NOT call CustomAudioSource directly.
     * It only puts the PCM into the small real-time queue.
     */
    public static void pushAudio(
            byte[] pcm,
            int sampleRate,
            int channels) {

        if (!running)
            start();

        int frameCount = pcm.length / (2 * channels);

        byte[] copy = pcm.clone();

        AudioPacket packet = new AudioPacket(
                copy,
                sampleRate,
                channels,
                frameCount);

        if (!QUEUE.offer(packet)) {
            QUEUE.poll();
            if (!QUEUE.offer(packet))
                return;
        }
    }

    private static void runWorker() {
        LOGGER.info("Audio Streamer started");

        while (running) {
            long start = System.nanoTime();

            try {
                AudioPacket packet = QUEUE.poll(100, TimeUnit.MILLISECONDS);

                if (packet == null)
                    continue;

                if (source != null)
                    source.pushAudio(
                            packet.pcm,
                            16,
                            packet.sampleRate,
                            packet.channels,
                            packet.frameCount);

                long elapsed = System.nanoTime() - start;
                long remaining = 10_000_000L - elapsed;

                if (remaining > 0) {
                    Thread.sleep(
                            remaining / 1_000_000L,
                            (int) (remaining % 1_000_000L));
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                t.printStackTrace();
                break;
            }
        }

        LOGGER.info("Audio Streamer stopped");
    }

    private static final class AudioPacket {

        final byte[] pcm;
        final int sampleRate;
        final int channels;
        final int frameCount;

        AudioPacket(
                byte[] pcm,
                int sampleRate,
                int channels,
                int frameCount) {

            this.pcm = pcm;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.frameCount = frameCount;
        }
    }
}