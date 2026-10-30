package rtc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.mojang.blaze3d.platform.NativeImage;

import dev.onvoid.webrtc.media.FourCC;
import dev.onvoid.webrtc.media.video.CustomVideoSource;
import dev.onvoid.webrtc.media.video.I420Buffer;
import dev.onvoid.webrtc.media.video.NativeI420Buffer;
import dev.onvoid.webrtc.media.video.VideoBufferConverter;
import dev.onvoid.webrtc.media.video.VideoFrame;

public class VideoStreamer {

    private record VideoFrameData(
            int width,
            int height,
            int[] pixels,
            long time) {
    }

    private static final BlockingQueue<VideoFrameData> frames = new ArrayBlockingQueue<>(1);

    private static Thread thread;
    private static volatile boolean running = false;

    public static synchronized void start() {

        if (running) {
            return;
        }

        running = true;

        thread = new Thread(() -> {

            while (running) {
                try {
                    VideoFrameData frame = frames.take();
                    sendFrame(frame);

                } catch (InterruptedException e) {
                    break;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }, "Minecraft-WebRTC-Video");

        thread.setDaemon(true);
        thread.start();
    }

    public static synchronized void stop() {
        running = false;

        Thread worker = thread;
        if (worker != null) {
            worker.interrupt();

            boolean interrupted = false;
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }

            thread = null;
            if (interrupted)
                Thread.currentThread().interrupt();
        }

        frames.clear();
    }

    public static void submit(NativeImage image, long now) {

        if (!running) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int r = Config.RES.get();
        if (r == 1) {
            frames.poll();
            frames.offer(new VideoFrameData(width, height, image.getPixelsRGBA(), now));
        } else {
            int rwidth = width / r;
            int rheight = height / r;

            int[] src = image.getPixelsRGBA();
            int[] resized = new int[rwidth * rheight];

            for (int y = 0; y < rheight; y++) {
                for (int x = 0; x < rwidth; x++) {
                    resized[y * rwidth + x] = src[(y * r) * width + (x * r)];
                }
            }

            frames.poll();
            frames.offer(new VideoFrameData(rwidth, rheight, resized, now));
        }
    }

    private static void sendFrame(VideoFrameData data) {

        CustomVideoSource source = WebRTCServer.getVideoSource();

        if (source == null) {
            return;
        }

        int width = data.width;
        int height = data.height;

        byte[] rgba = intArrayToBytes(data.pixels);

        I420Buffer buffer = NativeI420Buffer.allocate(width, height);

        VideoFrame frame = null;

        try {

            VideoBufferConverter.convertToI420(
                    rgba,
                    buffer,
                    FourCC.RGBA);

            frame = new VideoFrame(buffer, data.time);

            source.pushFrame(frame);

            // Don't release buffer separately.
            // The VideoFrame owns the buffer.

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (frame != null) {
                frame.release();
            }
        }
    }

    private static byte[] intArrayToBytes(int[] pixels) {

        byte[] result = new byte[pixels.length * 4];

        for (int i = 0; i < pixels.length; i++) {

            int pixel = pixels[i];

            result[i * 4] = (byte) (pixel >> 24); // R
            result[i * 4 + 1] = (byte) (pixel >> 16); // G
            result[i * 4 + 2] = (byte) (pixel >> 8); // B
            result[i * 4 + 3] = (byte) pixel; // A
        }

        return result;
    }
}