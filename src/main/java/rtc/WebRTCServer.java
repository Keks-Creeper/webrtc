package rtc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelInit;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCDataChannelState;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtpEncodingParameters;
import dev.onvoid.webrtc.RTCRtpSendParameters;
import dev.onvoid.webrtc.RTCRtpSender;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.video.CustomVideoSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class WebRTCServer {

    public static String PUBLIC_IP;
    public static boolean OPEN;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static PeerConnectionFactory factory;
    private static final Map<Channel, ClientConnection> clients = new ConcurrentHashMap<>();
    private static CustomVideoSource videoSource;
    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;
    private static Channel serverChannel;
    public static final AtomicBoolean started = new AtomicBoolean(false);

    private WebRTCServer() {
    }

    public static boolean clients() {
        return !clients.isEmpty();
    }

    //Client connection
    private static final class ClientConnection {

        final Channel browserChannel;

        RTCPeerConnection peer;

        RTCDataChannel controlChannel;
        RTCDataChannel mouseChannel;

        VideoTrack videoTrack;
        AudioTrack audioTrack;

        private final AtomicBoolean closing = new AtomicBoolean(false);

        ClientConnection(Channel browserChannel) {
            this.browserChannel = browserChannel;
        }

        void close() {
            if (!closing.compareAndSet(false, true))
                return;

            RTCDataChannel control = controlChannel;
            RTCDataChannel mouse = mouseChannel;
            RTCPeerConnection connection = peer;

            controlChannel = null;
            mouseChannel = null;
            peer = null;
            videoTrack = null;
            audioTrack = null;

            if (control != null) {
                try {
                    control.close();
                    control.dispose();
                } catch (Exception e) {
                    LOGGER.warn("Failed to close control data channel.", e);
                }
            }

            if (mouse != null) {
                try {
                    mouse.close();
                    mouse.dispose();
                } catch (Exception e) {
                    LOGGER.warn("Failed to close mouse data channel.", e);
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    LOGGER.warn("Failed to close peer connection.", e);
                }
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * Public API
     * ------------------------------------------------------------
     */

    public static CustomVideoSource getVideoSource() {
        return videoSource;
    }

    /*
     * ------------------------------------------------------------
     * Start
     * ------------------------------------------------------------
     */

    public static synchronized void start() {

        if (!started.compareAndSet(false, true)) {
            LOGGER.info("Server already started.");
            return;
        }

        Minecraft mc = Minecraft.getInstance(); // all this shit just to add toast holy moly
        Thread.ofPlatform().daemon().start(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            mc.execute(() -> {
                mc.getToasts().addToast(
                        SystemToast.multiline(
                                mc,
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.translatable("rtc.webrtc"),
                                Component.translatable("rtc.started")));
            });
        });

        Thread.ofPlatform()
                .name("WebRTC-Server")
                .daemon(true)
                .start(WebRTCServer::run);
    }

    private static void run() {
        try {
            try {
                initWebRTC();
                startWebServer();
                getPublicIp();
                IsOpen();

                LOGGER.info("WebRTC server started!");
                String PORN = Config.PORT.get() == 80 ? "" : ":" + Config.PORT.get();
                LOGGER.info("Local-IP:  http://127.0.0.1" + PORN);
                if (PUBLIC_IP instanceof String) {
                    LOGGER.info("Public-IP: http://" + PUBLIC_IP + PORN);
                }
                LOGGER.info("Port: " + (OPEN ? "OPEN" : "CLOSED"));

            } catch (Exception e) {
                started.set(false);
                LOGGER.error("Failed to start:");
                e.printStackTrace();
                stop();
            }

        } catch (Throwable e) {

            e.printStackTrace();
        }
    }

    private static void getPublicIp() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.ipify.org")).GET().build();
            PUBLIC_IP = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            PUBLIC_IP = null;
        }
    }

    private static void IsOpen() {
        if (PUBLIC_IP instanceof String)
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(PUBLIC_IP, Config.PORT.get()), 1000);
                OPEN = true;
            } catch (IOException e) {
                OPEN = false;
            }
    }

    // Webrtc init
    private static void initWebRTC() {
        factory = new PeerConnectionFactory();
        videoSource = new CustomVideoSource();
        VideoStreamer.start();
        LOGGER.info("WebRTC factory initialized.");
    }

    /*private static ClientConnection getClient(
            Channel channel) {
    
        return clients.get(channel);
    }*/

    // create pee connection
    private static ClientConnection createPeerConnection(Channel browserChannel) {

        ClientConnection client = clients.get(browserChannel);

        if (client == null) {
            client = new ClientConnection(browserChannel);
            clients.put(browserChannel, client);
        }

        if (client.peer != null) {
            try {
                client.peer.close();
            } catch (Exception ignored) {
            }
            client.peer = null;
        }

        RTCConfiguration config = new RTCConfiguration();
        RTCIceServer stun = new RTCIceServer();
        stun.urls.add(Config.STUN_URL.get());
        config.iceServers.add(stun);
        PeerObserver observer = new PeerObserver(client);
        client.peer = factory.createPeerConnection(config, observer);

        if (client.peer == null) {
            throw new IllegalStateException("Failed to create RTCPeerConnection.");
        }

        RTCDataChannelInit controlInit = new RTCDataChannelInit();
        controlInit.ordered = true;
        client.controlChannel = client.peer.createDataChannel("control", controlInit);
        client.controlChannel.registerObserver(new DataObserver("control", client.controlChannel));

        RTCDataChannelInit mouseInit = new RTCDataChannelInit();
        mouseInit.ordered = false;
        mouseInit.maxRetransmits = 0;
        client.mouseChannel = client.peer.createDataChannel("mouse", mouseInit);
        client.mouseChannel.registerObserver(new DataObserver("mouse", client.mouseChannel));

        List<String> streamIds = List.of("minecraft");

        if (Config.VIDEOSEND.get()) {
            client.videoTrack = factory.createVideoTrack(
                    "minecraft-video-" + Integer.toHexString(System.identityHashCode(client)),
                    videoSource);

            RTCRtpSender videoSender = client.peer.addTrack(client.videoTrack, streamIds);
            RTCRtpSendParameters params = videoSender.getParameters();
            for (RTCRtpEncodingParameters encoding : params.encodings)
                encoding.maxBitrate = Config.BITRATE.get();
            videoSender.setParameters(params);
        }

        if (Config.AUDIOSEND.get()) {
            client.audioTrack = factory.createAudioTrack(
                    "minecraft-audio" + Integer.toHexString(System.identityHashCode(client)),
                    AudioStreamer.getSource());
            client.peer.addTrack(client.audioTrack, streamIds);
        }

        LOGGER.info("New PeerConnection created for " + browserChannel.remoteAddress());
        return client;
    }

    private static void createOffer(ClientConnection client) {

        if (client == null || client.peer == null) {
            LOGGER.error("Cannot create offer: client/peer is null.");
            return;
        }

        RTCPeerConnection peer = client.peer;

        peer.createOffer(
                new RTCOfferOptions(),
                new CreateSessionDescriptionObserver() {

                    @Override
                    public void onSuccess(
                            RTCSessionDescription description) {
                        if (client.peer != peer)
                            return;

                        peer.setLocalDescription(
                                description,

                                new SetSessionDescriptionObserver() {

                                    @Override
                                    public void onSuccess() {
                                        JsonObject message = new JsonObject();
                                        message.addProperty("type", "offer");
                                        message.addProperty("sdp", description.sdp);
                                        sendSignal(client, message.toString());
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        LOGGER.error("setLocalDescription: " + error);
                                    }

                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        LOGGER.error("createOffer: " + error);
                    }
                });
    }

    // this shit is spinning!!!
    private static void handleSignal(Channel browserChannel, String text) {
        try {

            ClientConnection client = clients.get(browserChannel);
            if (client == null) {
                LOGGER.warn("Received signal from unknown client.");
                return;
            }

            JsonObject message = JsonParser.parseString(text).getAsJsonObject();
            if (!message.has("type"))
                return;

            String type = message.get("type").getAsString();
            switch (type) {

                case "start" -> {
                    client = createPeerConnection(browserChannel);
                    createOffer(client);
                }

                case "answer" -> {
                    if (client.peer == null) {
                        LOGGER.warn("Answer received without PeerConnection.");
                        return;
                    }

                    RTCPeerConnection peer = client.peer;
                    RTCSessionDescription answer = new RTCSessionDescription(RTCSdpType.ANSWER,
                            message.get("sdp").getAsString());

                    peer.setRemoteDescription(answer,
                            new SetSessionDescriptionObserver() {
                                @Override
                                public void onSuccess() {
                                    LOGGER.info("Browser answer applied for " + browserChannel.remoteAddress());
                                }

                                @Override
                                public void onFailure(String error) {
                                    LOGGER.error("Remote SDP failed: " + error);
                                }
                            });
                }

                case "ice" -> {
                    if (client.peer == null)
                        return;

                    String candidate = message.get("candidate").getAsString();
                    if (candidate.isBlank())
                        return;

                    String sdpMid = null;
                    if (message.has("sdpMid") && !message.get("sdpMid").isJsonNull())
                        sdpMid = message.get("sdpMid").getAsString();

                    client.peer.addIceCandidate(new RTCIceCandidate(
                            sdpMid,
                            message.get("sdpMLineIndex").getAsInt(),
                            candidate));
                }

                default ->
                    LOGGER.info("Unknown signal: " + type);
            }

        } catch (Exception e) {
            LOGGER.error("Invalid signaling message:");
            e.printStackTrace();
        }
    }

    private static void sendIce(ClientConnection client, RTCIceCandidate candidate) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "ice");
        message.addProperty("candidate", candidate.sdp);
        if (candidate.sdpMid == null) // why this? idk but when i remove it - crash
            message.add("sdpMid", null);
        else
            message.addProperty("sdpMid", candidate.sdpMid);
        message.addProperty("sdpMLineIndex", candidate.sdpMLineIndex);
        sendSignal(client, message.toString());
    }

    // Pee Observer (me) https://rule34.xxx?page=post&s=list&tags=peeing
    private static final class PeerObserver implements PeerConnectionObserver {

        private final ClientConnection client;

        PeerObserver(ClientConnection client) {
            this.client = client;
        }

        @Override
        public void onDataChannel(RTCDataChannel channel) {
            LOGGER.info("Incoming DataChannel [" + client.browserChannel.remoteAddress() + "]: " + channel.getLabel());
        }

        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            sendIce(client, candidate);
        }

        @Override
        public void onIceConnectionChange(RTCIceConnectionState state) {
            LOGGER.info("ICE [" + client.browserChannel.remoteAddress() + "]: " + state);
        }

        @Override
        public void onConnectionChange(RTCPeerConnectionState state) {
            LOGGER.info("PeerConnection [" + client.browserChannel.remoteAddress() + "]: " + state);

            /* The signaling WebSocket may already be closed because the browser closes it after WebRTC connects.
             * important shit dude...
             * Therefore cleanup must happen from WebRTC state!
             */
            if (state == RTCPeerConnectionState.FAILED || state == RTCPeerConnectionState.CLOSED) {
                removeClient(client);
            }
        }
    }

    // Data-chan observer (me) のノの
    private static final class DataObserver implements RTCDataChannelObserver {

        private final String name;
        private final RTCDataChannel channel;

        DataObserver(String name, RTCDataChannel channel) {
            this.name = name;
            this.channel = channel;
        }

        @Override
        public void onBufferedAmountChange(long previousAmount) {
        }

        @Override
        public void onStateChange() {
            LOGGER.info("DataChannel \"" + name + "\": " + channel.getState());
        }

        @Override
        public void onMessage(RTCDataChannelBuffer buffer) {

            ByteBuffer data = buffer.data.asReadOnlyBuffer();
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            String message = new String(bytes, StandardCharsets.UTF_8);

            // this got annoying...
            //LOGGER.debug(">>> " + name + " says: " + message);
            try {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                if (name.equals("control")) {
                    String type = json.get("t").getAsString();
                    switch (type) {

                        case "kd" -> {
                            InputSimulator.keyDown(json.get("c").getAsString(), json.get("k").getAsString());
                        }

                        case "ku" -> {
                            InputSimulator.keyUp(json.get("c").getAsString(), json.get("k").getAsString());
                        }

                        case "md" -> {
                            InputSimulator.click(json.get("b").getAsInt(), true);
                        }
                        case "mu" -> {
                            InputSimulator.click(json.get("b").getAsInt(), false);
                        }
                        case "esc" -> {
                            InputSimulator.escape();
                        }
                        case "ms" -> {
                            InputSimulator.scroll(json.get("dx").getAsInt(), json.get("dy").getAsInt());
                        }

                        default -> {
                            LOGGER.warn("Unknown control input: " + message);
                        }
                    }
                } else if (name.equals("mouse")) {
                    try {
                        InputSimulator.mouseMove(
                                json.get("dx").getAsDouble(),
                                json.get("dy").getAsDouble());

                    } catch (Exception e) {
                        InputSimulator.mouseSet(
                                json.get("mx").getAsDouble(),
                                json.get("my").getAsDouble());
                    }
                }

            } catch (Exception e) {
                LOGGER.warn("Invalid input message:" + message);
            }
        }
    }

    /*
     * ------------------------------------------------------------
     * HTTP + WebSocket server
     * ------------------------------------------------------------
     */

    private static void startWebServer() throws InterruptedException {

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler( //https://preview.redd.it/0i7csabnr7uh1.gif?width=640&format=mp4&s=b6f5d4e96e171d3493bdd387537344193f5bd6bd
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                pipeline.addLast(new WebSocketServerProtocolHandler("/signal", null, true));
                                pipeline.addLast(new HttpHandler());
                                pipeline.addLast(new SignalHandler());
                            }
                        });

        serverChannel = bootstrap.bind(Config.PUBLIC.get() ? "0.0.0.0" : "127.0.0.1", Config.PORT.get()).sync()
                .channel();
        LOGGER.info("HTTP/WebSocket server started.");
    }

    /*
     * ------------------------------------------------------------
     * HTTP handler
     * ------------------------------------------------------------
     */

    private static final class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String uri = request.uri();
            LOGGER.info("HTTP REQUEST: {}", request.uri());
            if (!uri.equals("/") && !uri.equals("/index.html")) {
                send404(ctx);
                return;
            }

            try

            {
                String html = new String(WebRTCServer.class.getResourceAsStream("/web/index.html").readAllBytes(),
                        StandardCharsets.UTF_8);

                html = html.replace(
                        "<!--LOGO-->",
                        "<link rel=\"icon\" type=\"image/png\" href=\"data:image/png;base64,"
                                + java.util.Base64.getEncoder().encodeToString(
                                        WebRTCServer.class.getResourceAsStream("/logo.png").readAllBytes())
                                + "\">");

                html = html.replace(
                        "/*--F3--*/",
                        new String(WebRTCServer.class
                                .getResourceAsStream("/web/F3.js")
                                .readAllBytes(), StandardCharsets.UTF_8));

                html = html.replace(
                        "/*--SCRIPT--*/",
                        new String(WebRTCServer.class
                                .getResourceAsStream("/web/script.js")
                                .readAllBytes(), StandardCharsets.UTF_8));

                html = html.replace("/*--STUN--*/", Config.STUN_URL.get());
                html = html.replace("<!--TITLE-->", Config.TITLE.get().replace("*", "Minecraft"));
                byte[] data = html.getBytes(StandardCharsets.UTF_8);

                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(data));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);
                ctx.writeAndFlush(response);
            } catch (Exception e) {
                LOGGER.error("Failed to serve index.html:");
                e.printStackTrace();
                send404(ctx);
            }
        }

        private void send404(ChannelHandlerContext ctx) {
            byte[] data = "\udead\ubeef".getBytes(StandardCharsets.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NOT_FOUND,
                    Unpooled.wrappedBuffer(data));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);
            ctx.writeAndFlush(response);
        }
    }

    /*
     * ------------------------------------------------------------
     * WebSocket signaling handler
     * ------------------------------------------------------------
     */

    private static final class SignalHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
            if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                Channel channel = ctx.channel();
                ClientConnection client = new ClientConnection(channel);
                clients.put(channel, client);
                LOGGER.info("Browser WebSocket connected: " + channel.remoteAddress());

                JsonObject message = new JsonObject();
                message.addProperty("type", "maus");
                message.addProperty("mausms", "" + Config.MAUSMS.get());
                sendSignal(client, message.toString());
            }
            super.userEventTriggered(ctx, event);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
            handleSignal(ctx.channel(), frame.text());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            /*IfigConTetPORM..g()ANT:*
             * DO NTO rrnove D ClientConnection. hrer
             * Yr bwresr D cls snglng eWbSct
             * after WTCebR BcomS cmect. ed*
             * D wEBrtc cmect- cncetiNUE essn.g*/
            LOGGER.info("Browser WebSocket disconnected: " + ctx.channel().remoteAddress());
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("WebSocket error:");
            cause.printStackTrace();
            ctx.close();
        }

    }

    // deadass
    private static void sendSignal(ClientConnection client, String message) {
        if (client == null)
            return;
        Channel channel = client.browserChannel;
        if (channel != null && channel.isActive())
            channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    public static void sendToAllClients(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        for (ClientConnection client : clients.values()) {

            RTCDataChannel channel = client.controlChannel;
            if (channel == null || channel.getState() != RTCDataChannelState.OPEN)
                continue;

            try {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                channel.send(new RTCDataChannelBuffer(buffer, false));
            } catch (Exception e) {
                LOGGER.warn("Failed to send message to client: " + client.browserChannel.remoteAddress(), e);
            }
        }
    }

    private static void removeClient(ClientConnection client) { // IM GONNA KILL YOU
        if (client == null)
            return;

        // Only remove this exact client.
        boolean removed = clients.remove(client.browserChannel, client);

        if (!removed)
            return;

        LOGGER.info("Removing WebRTC client: " + client.browserChannel.remoteAddress());
        client.close();
    }

    public static synchronized void stop() {
        if (Config.RELOAD_ON_STOP.get())
            sendToAllClients("{\"reload\":0}");

        VideoStreamer.stop();

        if (serverChannel != null) {
            try {
                serverChannel.close().syncUninterruptibly();
            } finally {
                serverChannel = null;
            }
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }

        List<ClientConnection> clientsToClose = List.copyOf(clients.values());
        clients.clear();
        for (ClientConnection client : clientsToClose) {
            client.close();
        }

        if (factory != null) {
            try {
                factory.dispose();
            } catch (Exception ignored) {
            }
            factory = null;
        }
        videoSource = null;
        started.set(false);
        LOGGER.info("WebRTC server stopped.");
    }

    private static final AtomicBoolean restarting = new AtomicBoolean(false);

    public static void restart() {
        if (!restarting.compareAndSet(false, true))
            return;

        Thread.ofPlatform().name("WebRTC-Restart").start(() -> {
            try {
                stop();
                start();
            } catch (Exception e) {
                LOGGER.error("Failed to restart WebRTC server", e);
            } finally {
                restarting.set(false);
            }
        });
    }
}