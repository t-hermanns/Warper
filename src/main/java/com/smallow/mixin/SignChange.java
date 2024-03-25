package com.smallow.mixin;

import com.smallow.Warper;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SignBlockEntity.class)
public class SignChange {

    @Inject(method = "setText", at = @At("HEAD"), cancellable = true)
    private void set(SignText text, boolean front, CallbackInfoReturnable<Boolean> ci){
        if(this.abortSignChange(text, front))
            ci.cancel();
    }

    /**
     * called when a player tries to change the text on a sign
     * @param text the new text
     * @param front whether the front text is being changed
     * @return whether the sign change should be aborted
     */
    @Unique
    private boolean abortSignChange(SignText text, boolean front) {
        return Warper.singChangeCancelled(text, front, (SignBlockEntity) (Object)this);
    }
}