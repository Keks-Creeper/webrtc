// ============================================================
// F3 STATE
// ============================================================

let f3Enabled = false;

const f3 = document.getElementById("rtcF3");
const f3Text = document.getElementById("rtcF3Text");

const fpsCanvas = document.getElementById("rtcFPSGraph");
const rttCanvas = document.getElementById("rtcRTTGraph");


// ============================================================
// GRAPH HISTORY
// ============================================================

const fpsHistory = [];
const rttHistory = [];

const GRAPH_POINTS = 120;


function addHistory(history, value) {

    if (value == null || !Number.isFinite(value))
        return;

    history.push(value);

    while (history.length > GRAPH_POINTS)
        history.shift();
}


// ============================================================
// DRAW GRAPH
// ============================================================

function drawGraph(canvas, data, label, unit, fixedMax = null) {

    const ctx = canvas.getContext("2d");

    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);


    if (data.length < 2) {

        ctx.fillStyle = "white";
        ctx.font = "12px monospace";

        ctx.fillText(
            label + "  waiting...",
            5,
            14
        );

        return;
    }


    // --------------------------------------------------------
    // Find maximum
    // --------------------------------------------------------

    let max;

    if (fixedMax !== null) {

        max = fixedMax;

    } else {

        max = Math.max(...data);

        // Don't let graph collapse to zero
        max = Math.max(max, 1);

        // Add some headroom
        max *= 1.15;
    }


    // --------------------------------------------------------
    // Grid
    // --------------------------------------------------------

    ctx.strokeStyle = "rgba(255,255,255,0.15)";
    ctx.lineWidth = 1;

    for (let i = 0; i <= 4; i++) {

        const y = (height - 1) * i / 4;

        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
    }


    // --------------------------------------------------------
    // Y-axis labels
    // --------------------------------------------------------

    ctx.fillStyle = "rgba(255,255,255,0.55)";
    ctx.font = "9px monospace";

    for (let i = 0; i <= 2; i++) {

        const value = max * (1 - i / 2);

        const y = (height - 1) * i / 2;

        ctx.fillText(
            value.toFixed(0),
            width - 30,
            Math.max(9, y - 2)
        );
    }


    // --------------------------------------------------------
    // Graph coordinates
    // --------------------------------------------------------

    const points = [];

    for (let i = 0; i < data.length; i++) {

        const x =
            (i / (data.length - 1)) *
            (width - 1);

        const normalized =
            Math.max(0, Math.min(1, data[i] / max));

        const y =
            height -
            normalized * (height - 1);

        points.push({ x, y });
    }


    // --------------------------------------------------------
    // Filled area
    // --------------------------------------------------------

    ctx.beginPath();

    ctx.moveTo(
        points[0].x,
        height
    );

    for (const point of points) {

        ctx.lineTo(
            point.x,
            point.y
        );
    }

    ctx.lineTo(
        points[points.length - 1].x,
        height
    );

    ctx.closePath();

    ctx.fillStyle = "rgba(255,255,255,0.10)";
    ctx.fill();


    // --------------------------------------------------------
    // Line
    // --------------------------------------------------------

    ctx.beginPath();

    for (let i = 0; i < points.length; i++) {

        const point = points[i];

        if (i === 0)
            ctx.moveTo(point.x, point.y);
        else
            ctx.lineTo(point.x, point.y);
    }

    ctx.strokeStyle = "white";
    ctx.lineWidth = 1.5;

    ctx.stroke();


    // --------------------------------------------------------
    // Label
    // --------------------------------------------------------

    const latest =
        data[data.length - 1];

    ctx.fillStyle = "white";
    ctx.font = "11px monospace";

    ctx.fillText(
        `${label}  ${latest.toFixed(1)} ${unit}`,
        5,
        13
    );
}


// ============================================================
// WEBRTC STATS
// ============================================================

async function getRTCStats() {

    if (!pc)
        return null;

    const stats = await pc.getStats();

    let video = null;
    let candidatePair = null;


    stats.forEach(report => {

        // ----------------------------------------------------
        // Incoming video
        // ----------------------------------------------------

        if (
            report.type === "inbound-rtp" &&
            (
                report.kind === "video" ||
                report.mediaType === "video"
            )
        ) {

            video = report;
        }


        // ----------------------------------------------------
        // Network connection
        // ----------------------------------------------------

        if (
            report.type === "candidate-pair" &&
            report.state === "succeeded" &&
            report.currentRoundTripTime != null
        ) {

            candidatePair = report;
        }
    });


    return {
        video,
        candidatePair
    };
}


// ============================================================
// F3 UPDATE
// ============================================================

async function updateF3() {

    if (!f3Enabled)
        return;


    try {

        const result = await getRTCStats();


        if (!result) {

            f3Text.textContent =
                "WebRTC\nWaiting for connection...";

            return;
        }


        const video = result.video;
        const candidatePair = result.candidatePair;


        // ----------------------------------------------------
        // Video stats
        // ----------------------------------------------------

        let fps = 0;
        let width = 0;
        let height = 0;

        let bytesReceived = 0;
        let packetsReceived = 0;
        let packetsLost = 0;
        let framesDropped = 0;
        let jitter = 0;


        if (video) {

            fps =
                video.framesPerSecond ??
                0;

            width =
                video.frameWidth ??
                0;

            height =
                video.frameHeight ??
                0;

            bytesReceived =
                video.bytesReceived ??
                0;

            packetsReceived =
                video.packetsReceived ??
                0;

            packetsLost =
                video.packetsLost ??
                0;

            framesDropped =
                video.framesDropped ??
                0;

            jitter =
                video.jitter ??
                0;
        }


        // ----------------------------------------------------
        // RTT
        // ----------------------------------------------------

        let rtt = null;

        if (candidatePair) {

            rtt =
                candidatePair.currentRoundTripTime *
                1000;
        }


        // ----------------------------------------------------
        // History
        // ----------------------------------------------------

        addHistory(
            fpsHistory,
            fps
        );

        if (rtt !== null) {

            addHistory(
                rttHistory,
                rtt
            );
        }


        // ----------------------------------------------------
        // Video bitrate
        // ----------------------------------------------------

        let bitrate = 0;

        if (
            updateF3.lastBytes !== null &&
            updateF3.lastTime !== null
        ) {

            const now = performance.now();

            const elapsed =
                (now - updateF3.lastTime) / 1000;

            if (elapsed > 0) {

                bitrate =
                    (bytesReceived - updateF3.lastBytes) *
                    8 /
                    elapsed /
                    1000;
            }
        }


        updateF3.lastBytes =
            bytesReceived;

        updateF3.lastTime =
            performance.now();


        // ----------------------------------------------------
        // Text
        // ----------------------------------------------------

        const rttText =
            rtt === null
                ? "--"
                : rtt.toFixed(1) + " ms";


        const jitterText =
            (jitter * 1000).toFixed(1) +
            " ms";


        const lossText =
            packetsLost;


        f3Text.textContent =
            `WebRTC F3

Jitter      ${jitterText}
Resolution  ${width} × ${height}

Bitrate     ${bitrate.toFixed(0)} kbit/s
Received    ${packetsReceived.toLocaleString()} packets
Lost        ${lossText.toLocaleString()}
Dropped     ${framesDropped.toLocaleString()}`;


        // ----------------------------------------------------
        // Graphs
        // ----------------------------------------------------

        drawGraph(
            fpsCanvas,
            fpsHistory,
            "FPS",
            "fps",
            60
        );


        drawGraph(
            rttCanvas,
            rttHistory,
            "RTT",
            "ms"
        );


    } catch (error) {

        console.error(
            "F3 stats error:",
            error
        );
    }
}


updateF3.lastBytes = null;
updateF3.lastTime = null;


// ============================================================
// UPDATE EVERY 500ms
// ============================================================

setInterval(
    updateF3,
    500
);


// ============================================================
// F3 TOGGLE
// ============================================================

document.addEventListener(
    "keydown",
    event => {

        if (event.code !== "F3")
            return;

        event.preventDefault();

        f3Enabled =
            !f3Enabled;

        f3.style.display =
            f3Enabled
                ? "block"
                : "none";

        if (f3Enabled)
            updateF3();
    }
);
