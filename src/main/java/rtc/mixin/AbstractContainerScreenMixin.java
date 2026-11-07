package rtc.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtc.InputSimulator;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @ModifyArg(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"), index = 3)
    private ClickType rtc$modifyClickType(ClickType type) {
        if (InputSimulator.remoteClick && InputSimulator.swapSlot != -1)
            return ClickType.SWAP;

        if (InputSimulator.remoteClick && InputSimulator.shifted)
            return ClickType.QUICK_MOVE;

        return type;
    }

    @ModifyArg(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"), index = 2)
    private int rtc$modifySwapButton(int button) {
        if (InputSimulator.remoteClick && InputSimulator.swapSlot != -1)
            return InputSimulator.swapSlot;

        return button;
    }
}