package rtc;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import rtc.mixin.MouseHandlerAccessor;

public final class InputSimulator {

    private InputSimulator() {
    }

    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void tick() {
        Runnable task;
        while ((task = QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("Input task failed:", e);
            }
        }
    }

    private static void execute(Runnable task) {
        QUEUE.offer(task);
    }

    public static boolean shifted = true;
    public static int swapSlot = -1;

    public static void keyDown(String code, String keyy) {
        execute(() -> {
            Minecraft mc = Minecraft.getInstance();

            if (mc.screen != null) {

                if (keyy.length() == 1)
                    mc.screen.charTyped(keyy.charAt(0), 0);

                try {
                    int keyCode = InputConstants.getKey("key.keyboard." + kii(code)).getValue();
                    Integer key = keyFromCode(code);
                    int scanCode = GLFW.glfwGetKeyScancode(key);

                    for (int i = 0; i < 9; i++) {
                        if (mc.options.keyHotbarSlots[i].matches(key, scanCode)) {
                            swapSlot = i;
                            break;
                        }
                    }
                    if (mc.options.keySwapOffhand.matches(key, scanCode))
                        swapSlot = 40;

                    mc.screen.keyPressed(keyCode, scanCode, 0);

                } catch (Exception e) {
                    LOGGER.warn("unknown key: " + code + " (" + keyy + ")");
                }

            }

            if (code.contains("Shift"))
                shifted = true;

            Integer key = keyFromCode(code);
            if (key != -1) {
                int scanCode = GLFW.glfwGetKeyScancode(key);
                KeyboardHandler keyboard = mc.keyboardHandler;
                long window = mc.getWindow().getWindow();

                keyboard.keyPress(
                        window,
                        key,
                        scanCode,
                        GLFW.GLFW_PRESS,
                        0);
            }
        });
    }

    public static void keyUp(String code, String keyy) {
        execute(() -> {
            Integer key = keyFromCode(code);
            if (key != -1) {
                Minecraft mc = Minecraft.getInstance();
                KeyboardHandler keyboard = mc.keyboardHandler;
                long window = mc.getWindow().getWindow();
                int scanCode = GLFW.glfwGetKeyScancode(key);

                if (code.contains("Shift"))
                    shifted = false;

                keyboard.keyPress(
                        window,
                        key,
                        scanCode,
                        GLFW.GLFW_RELEASE,
                        0);
            }
        });
    }

    public static boolean remoteClick = false;

    public static void click(int nottherealbutton, boolean down) {
        execute(() -> {
            int button;
            switch (nottherealbutton) {
                case 0 -> button = 0;
                case 1 -> button = 2;
                default -> button = 1;
            }

            Minecraft mc = Minecraft.getInstance();
            MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;

            remoteClick = true;
            try {
                mouse.rtc$onPress(
                        mc.getWindow().getWindow(),
                        button,
                        down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE,
                        3);
            } finally {
                remoteClick = false;
            }
        });
    }

    public static void mouseMove(double dx, double dy) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
            player.turn(dx, dy);
    }

    public static void mouseSet(double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        MouseHandlerAccessor mouse = (MouseHandlerAccessor) mc.mouseHandler;
        Window window = mc.getWindow();
        mouse.rtc$onMove(window.getWindow(), mx * window.getScreenWidth(), my * window.getScreenHeight());
    }

    public static void escape() {
        execute(() -> {
            Minecraft.getInstance().setScreen(new PauseScreen(true));
        });
    }

    public static void scroll(double dx, double dy) {
        execute(() -> {
            MouseHandler mouse = Minecraft.getInstance().mouseHandler;
            ((MouseHandlerAccessor) mouse).rtc$onScroll(
                    Minecraft.getInstance().getWindow().getWindow(),
                    dx / 20,
                    -dy / 20);
        });
    }

    // damn ass shit
    private static String kii(String kii) {
        return switch (kii.toLowerCase()) {
            case "shiftleft" -> "left.shift";
            case "shiftright" -> "right.shift";
            case "controlleft" -> "left.control";
            case "controlright" -> "right.control";
            case "altleft" -> "left.alt";
            case "altright" -> "right.alt";
            case "metaleft" -> "left.win";
            case "metaright" -> "right.win";
            case "capslock" -> "caps.lock";
            case "backquote" -> "grave.accent";
            default -> kii.toLowerCase()
                    .replace("arrow", "")
                    .replace("page", "");
        };
    }

    // damn ass shit too
    private static Integer keyFromCode(String code) {

        return switch (code) {

            case "KeyA" -> GLFW.GLFW_KEY_A;
            case "KeyB" -> GLFW.GLFW_KEY_B;
            case "KeyC" -> GLFW.GLFW_KEY_C;
            case "KeyD" -> GLFW.GLFW_KEY_D;
            case "KeyE" -> GLFW.GLFW_KEY_E;
            case "KeyF" -> GLFW.GLFW_KEY_F;
            case "KeyG" -> GLFW.GLFW_KEY_G;
            case "KeyH" -> GLFW.GLFW_KEY_H;
            case "KeyI" -> GLFW.GLFW_KEY_I;
            case "KeyJ" -> GLFW.GLFW_KEY_J;
            case "KeyK" -> GLFW.GLFW_KEY_K;
            case "KeyL" -> GLFW.GLFW_KEY_L;
            case "KeyM" -> GLFW.GLFW_KEY_M;
            case "KeyN" -> GLFW.GLFW_KEY_N;
            case "KeyO" -> GLFW.GLFW_KEY_O;
            case "KeyP" -> GLFW.GLFW_KEY_P;
            case "KeyQ" -> GLFW.GLFW_KEY_Q;
            case "KeyR" -> GLFW.GLFW_KEY_R;
            case "KeyS" -> GLFW.GLFW_KEY_S;
            case "KeyT" -> GLFW.GLFW_KEY_T;
            case "KeyU" -> GLFW.GLFW_KEY_U;
            case "KeyV" -> GLFW.GLFW_KEY_V;
            case "KeyW" -> GLFW.GLFW_KEY_W;
            case "KeyX" -> GLFW.GLFW_KEY_X;
            case "KeyY" -> GLFW.GLFW_KEY_Y;
            case "KeyZ" -> GLFW.GLFW_KEY_Z;

            case "Digit0" -> GLFW.GLFW_KEY_0;
            case "Digit1" -> GLFW.GLFW_KEY_1;
            case "Digit2" -> GLFW.GLFW_KEY_2;
            case "Digit3" -> GLFW.GLFW_KEY_3;
            case "Digit4" -> GLFW.GLFW_KEY_4;
            case "Digit5" -> GLFW.GLFW_KEY_5;
            case "Digit6" -> GLFW.GLFW_KEY_6;
            case "Digit7" -> GLFW.GLFW_KEY_7;
            case "Digit8" -> GLFW.GLFW_KEY_8;
            case "Digit9" -> GLFW.GLFW_KEY_9;

            case "F1" -> GLFW.GLFW_KEY_F1;
            case "F2" -> GLFW.GLFW_KEY_F2;
            case "F3" -> GLFW.GLFW_KEY_F3;
            case "F4" -> GLFW.GLFW_KEY_F4;
            case "F5" -> GLFW.GLFW_KEY_F5;
            case "F6" -> GLFW.GLFW_KEY_F6;
            case "F7" -> GLFW.GLFW_KEY_F7;
            case "F8" -> GLFW.GLFW_KEY_F8;
            case "F9" -> GLFW.GLFW_KEY_F9;
            case "F10" -> GLFW.GLFW_KEY_F10;
            case "F11" -> GLFW.GLFW_KEY_F11;
            case "F12" -> GLFW.GLFW_KEY_F12;
            case "F13" -> GLFW.GLFW_KEY_F13;
            case "F14" -> GLFW.GLFW_KEY_F14;
            case "F15" -> GLFW.GLFW_KEY_F15;
            case "F16" -> GLFW.GLFW_KEY_F16;
            case "F17" -> GLFW.GLFW_KEY_F17;
            case "F18" -> GLFW.GLFW_KEY_F18;
            case "F19" -> GLFW.GLFW_KEY_F19;
            case "F20" -> GLFW.GLFW_KEY_F20;
            case "F21" -> GLFW.GLFW_KEY_F21;
            case "F22" -> GLFW.GLFW_KEY_F22;
            case "F23" -> GLFW.GLFW_KEY_F23;
            case "F24" -> GLFW.GLFW_KEY_F24;
            case "F25" -> GLFW.GLFW_KEY_F25;

            case "Numpad0" -> GLFW.GLFW_KEY_KP_0;
            case "Numpad1" -> GLFW.GLFW_KEY_KP_1;
            case "Numpad2" -> GLFW.GLFW_KEY_KP_2;
            case "Numpad3" -> GLFW.GLFW_KEY_KP_3;
            case "Numpad4" -> GLFW.GLFW_KEY_KP_4;
            case "Numpad5" -> GLFW.GLFW_KEY_KP_5;
            case "Numpad6" -> GLFW.GLFW_KEY_KP_6;
            case "Numpad7" -> GLFW.GLFW_KEY_KP_7;
            case "Numpad8" -> GLFW.GLFW_KEY_KP_8;
            case "Numpad9" -> GLFW.GLFW_KEY_KP_9;
            case "NumLock" -> GLFW.GLFW_KEY_NUM_LOCK;
            case "NumpadDecimal" -> GLFW.GLFW_KEY_KP_DECIMAL;
            case "NumpadAdd" -> GLFW.GLFW_KEY_KP_ADD;
            case "NumpadSubtract" -> GLFW.GLFW_KEY_KP_SUBTRACT;
            case "NumpadMultiply" -> GLFW.GLFW_KEY_KP_MULTIPLY;
            case "NumpadDivide" -> GLFW.GLFW_KEY_KP_DIVIDE;
            case "NumpadEnter" -> GLFW.GLFW_KEY_KP_ENTER;
            case "NumpadEqual" -> GLFW.GLFW_KEY_KP_EQUAL;

            case "Space" -> GLFW.GLFW_KEY_SPACE;
            case "Enter" -> GLFW.GLFW_KEY_ENTER;
            case "Escape" -> GLFW.GLFW_KEY_ESCAPE;
            case "Backspace" -> GLFW.GLFW_KEY_BACKSPACE;
            case "Tab" -> GLFW.GLFW_KEY_TAB;

            case "ShiftLeft" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "ShiftRight" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "CapsLock" -> GLFW.GLFW_KEY_CAPS_LOCK;

            case "ControlLeft" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "ControlRight" -> GLFW.GLFW_KEY_RIGHT_CONTROL;

            case "AltLeft" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "AltRight" -> GLFW.GLFW_KEY_RIGHT_ALT;

            case "Slash" -> GLFW.GLFW_KEY_SLASH;
            case "Period" -> GLFW.GLFW_KEY_PERIOD;
            case "Comma" -> GLFW.GLFW_KEY_COMMA;
            case "Backquote" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case "Backslash" -> GLFW.GLFW_KEY_BACKSLASH;
            case "IntlBackslash" -> GLFW.GLFW_KEY_WORLD_1;
            case "BracketRight" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "BracketLeft" -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case "Semicolon" -> GLFW.GLFW_KEY_SEMICOLON;
            case "Quote" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "Equal" -> GLFW.GLFW_KEY_EQUAL;
            case "Minus" -> GLFW.GLFW_KEY_MINUS;

            case "ArrowUp" -> GLFW.GLFW_KEY_UP;
            case "ArrowDown" -> GLFW.GLFW_KEY_DOWN;
            case "ArrowLeft" -> GLFW.GLFW_KEY_LEFT;
            case "ArrowRight" -> GLFW.GLFW_KEY_RIGHT;

            case "Insert" -> GLFW.GLFW_KEY_INSERT;
            case "Delete" -> GLFW.GLFW_KEY_DELETE;
            case "Home" -> GLFW.GLFW_KEY_HOME;
            case "End" -> GLFW.GLFW_KEY_END;
            case "PageUp" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PageDown" -> GLFW.GLFW_KEY_PAGE_DOWN;

            default -> -1;
        };
    }
}