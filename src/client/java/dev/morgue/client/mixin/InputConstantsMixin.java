package dev.morgue.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards the SDL-backed input path against unbound key mappings.
 *
 * In 26.3 an unassigned mapping is represented by key code -1. Buffered
 * input implementations may index their key-state buffer directly, so
 * polling that value can throw IndexOutOfBoundsException during mouse capture.
 */
@Mixin(InputConstants.class)
public abstract class InputConstantsMixin {
    @Inject(method = "isKeyDown(I)Z", at = @At("HEAD"), cancellable = true)
    private static void morgue$ignoreUnboundKey(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key < 0) {
            cir.setReturnValue(false);
        }
    }
}
