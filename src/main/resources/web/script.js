const video = document.getElementById("screen");
const audio = document.getElementById("audio");

let pc = null;
let controlChannel = null;
let mouseChannel = null;
let signaling = null;
let plock = false;

function startWebRTC() {
    pc = new RTCPeerConnection({
        iceServers: [
            {
                urls: "/*--STUN--*/"
            }
        ]
    });

    /* --------------------------------------------------------
       Receive Minecraft-created DataChannels
       -------------------------------------------------------- */

    pc.ondatachannel = event => {

        const channel = event.channel;

        console.log("DataChannel received: " + channel.label);


        if (channel.label === "control") {
            controlChannel = channel;
            channel.onopen = () => {
                console.log("control OPEN");
            };
            channel.onclose = () => {
                console.log("control CLOSED");
                if (controlChannel === channel)
                    controlChannel = null;
            };
            channel.onerror = error => {
                console.error("control error:", error);
            };
            channel.onmessage = event => {
                try {
                    const data = JSON.parse(event.data);
                    console.log("<< " + event.data)
                    if ("ingame" in data)
                        plock = data.ingame;
                    if ("mausms" in data)
                        mausms = data.mausms;
                    if ("title" in data)
                        document.title = data.title;
                    if ("to" in data)
                        if (data.to.trim().length === 0)
                            setTimeout(() => {
                                window.location.href = window.location.pathname;
                            }, 1000);
                        else
                            window.location.href = data.to.includes("://") ? data.to : "https://" + data.to;
                } catch {
                }
            }
        }


        else if (channel.label === "mouse") {
            mouseChannel = channel;
            channel.onopen = () => {
                console.log("mouse OPEN");
            };
            channel.onclose = () => {
                console.log("mouse CLOSED");
                if (mouseChannel === channel)
                    mouseChannel = null;
            };
            channel.onerror = error => {
                console.error("mouse error:", error);
            };
            channel.onmessage = event => {
            };
        }


        else {
            console.log("Unknown DataChannel: " + channel.label);
        }
    };

    // video displaying
    pc.ontrack = async event => {
        document.getElementById("screen").style.display = "block";
        document.querySelector('link[rel="icon"]').href = "<!--LOGO2-->";

        console.log(
            "TRACK ARRIVED:",
            event.track.kind,
            "id =", event.track.id,
            "streams =", event.streams.length
        );

        if (event.track.kind === "audio") {
            audio.srcObject = event.streams[0];
            try {
                await audio.play();
            } catch {
            }
        }

        if (event.track.kind === "video") {
            video.srcObject = event.streams[0];
            try {
                await video.play();
            } catch {
                console.error(
                    "Video play FAILED:",
                    error
                );
            }
        }
    };

    // ice
    pc.onicecandidate = event => {

        if (!event.candidate)
            return;

        if (!signaling || signaling.readyState !== WebSocket.OPEN)
            return;

        signaling.send(JSON.stringify({
            type: "ice",
            candidate: event.candidate.candidate,
            sdpMid: event.candidate.sdpMid,
            sdpMLineIndex: event.candidate.sdpMLineIndex
        }));
    };
    pc.oniceconnectionstatechange = () => {
        console.log("ICE: " + pc.iceConnectionState);
    };

    // connection checking
    pc.onconnectionstatechange = () => {
        console.log("PeerConnection: " + pc.connectionState);
        if (pc.connectionState === "connected") {
            console.log("WebRTC connected!");
            if (signaling && signaling.readyState === WebSocket.OPEN) {
                signaling.close();
            }
        }
    };
}


/* ============================================================
   SIGNALING
   ============================================================ */
function connectSignaling() {
    signaling = new WebSocket("ws://" + location.host + "/signal");

    signaling.onopen = () => {
        console.log("Signaling WebSocket connected.");
    };

    signaling.onclose = () => {
        console.log("Signaling WebSocket closed.");
    };

    signaling.onerror = error => {
        console.error("Signaling error:", error);
    };

    function sxndthxs(jsom) {
        console.log(">>> " + JSON.stringify(jsom));
        signaling.send(JSON.stringify(jsom));
    }

    signaling.onmessage = async event => {
        const message = JSON.parse(event.data);
        console.log("<<< " + event.data);

        switch (message.type) {

            case "maus":
                console.log("Poll rate: " + message.mausms);
                mausms = message.mausms;
                sxndthxs({ type: "start" });
                break;

            case "offer":
                console.log(">>> " + JSON.stringify({ type: "offer", sdp: message.sdp }));
                await pc.setRemoteDescription({ type: "offer", sdp: message.sdp });
                const answer = await pc.createAnswer();
                await pc.setLocalDescription(answer);
                sxndthxs({ type: "answer", sdp: answer.sdp });
                break;


            case "ice":
                if (!message.candidate)
                    break;
                await pc.addIceCandidate({
                    candidate: message.candidate,
                    sdpMid: message.sdpMid,
                    sdpMLineIndex: message.sdpMLineIndex
                });
                break;

            default:
                break;
            //console.log("Unknown signal: " + message.type);
        }
    };
}

// logging in
document.getElementById("loginForm").addEventListener("submit", async event => {
    event.preventDefault();
    const password = document.getElementById("password").value;
    document.getElementById("password").style.display = "none";
    signaling.send(JSON.stringify({
        "p": password
    }));
});


//keyboard stuff
function sxndnxdes(data) {
    if (!controlChannel || controlChannel.readyState !== "open")
        return false;
    controlChannel.send(JSON.stringify(data));
    return true;
}

document.addEventListener("keydown", event => {
    if (event.repeat)
        return;
    if (sxndnxdes({
        t: "kd",
        c: event.code,
        k: event.key
    }) && event.key !== "F12")
        event.preventDefault();
});

document.addEventListener("keyup", event => {
    if (sxndnxdes({
        t: "ku",
        c: event.code,
        k: event.key
    }) && event.key !== "F12")
        event.preventDefault();
});

// mouse stuff
function maus(data) {
    if (!mouseChannel || mouseChannel.readyState !== "open")
        return false;
    mouseChannel.send(JSON.stringify(data));
    try {
        if (plock)
            video.requestPointerLock();
        else
            document.exitPointerLock();
    }
    catch { }
    pley();
    return true;
}

async function pley() {
    try {
        audio.muted = false;
        await audio.play();
    }
    catch { }
}


function mouse(w, event) {
    const rect = document.getElementById("screen").getBoundingClientRect();

    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const normalizedX = x / rect.width;
    const normalizedY = y / rect.height;

    return sxndnxdes({
        t: w,
        b: event.button,
        x: normalizedX,
        y: normalizedY
    });
}
document.addEventListener("mousedown", (event) => {
    if (mouse("md", event)) event.preventDefault();
});
document.addEventListener("contextmenu", (event) => {
    event.preventDefault();
});
document.addEventListener("mouseup", (event) => {
    if (mouse("mu", event)) event.preventDefault();
});
document.addEventListener("wheel", (event) => {
    sxndnxdes({
        t: "ms",
        dx: event.deltaX,
        dy: event.deltaY
    });
});

let pendx = 0;
let pendy = 0;
let lastMoveTime = 0;
let mausms = 100;
document.addEventListener("pointermove", (event) => {
    const rect = document.getElementById("screen").getBoundingClientRect();

    const now = performance.now();

    if (document.pointerLockElement) {
        pendx += event.movementX;
        pendy += event.movementY;
        if (now - lastMoveTime >= mausms) {
            lastMoveTime = now;
            maus({
                dx: pendx,
                dy: pendy
            });
            pendx = 0;
            pendy = 0;
        }
        return;
    }

    if (now - lastMoveTime >= mausms) {
        lastMoveTime = now;
        maus({
            mx: (event.clientX - rect.left) / rect.width,
            my: (event.clientY - rect.top) / rect.height
        });
    }
});

document.addEventListener('pointerlockchange', () => {
    if (document.pointerLockElement !== video && plock) {
        plock = false;
        sxndnxdes({
            t: "esc"
        });
    }
});

startWebRTC();
connectSignaling();
