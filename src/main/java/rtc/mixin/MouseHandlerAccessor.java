package rtc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("xpos")
    double rtc$getXpos();

    @Accessor("xpos")
    void rtc$setXpos(double value);

    @Accessor("ypos")
    double rtc$getYpos();

    @Accessor("ypos")
    void rtc$setYpos(double value);

    @Accessor("accumulatedDX")
    double rtc$getAccumulatedDX();

    @Accessor("accumulatedDX")
    void rtc$setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    double rtc$getAccumulatedDY();

    @Accessor("accumulatedDY")
    void rtc$setAccumulatedDY(double value);

    @Accessor("ignoreFirstMove")
    boolean rtc$getIgnoreFirstMove();

    @Invoker("onScroll")
    void rtc$onScroll(long window, double xOffset, double yOffset);

    @Invoker("onPress")
    void rtc$onPress(
            long window,
            int button,
            int action,
            int modifiers);
}