package cn.disy920.invsync.mixin;

import cn.disy920.invsync.access.PlayerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(ItemEntity.class)
public class MixinItemEntity {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    public void injectOnPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        if (((PlayerAccess)player).isWaiting()) {
            ci.cancel();
        }
    }
}
