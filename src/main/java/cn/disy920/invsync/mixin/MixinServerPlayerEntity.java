package cn.disy920.invsync.mixin;

import cn.disy920.invsync.access.PlayerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.SERVER)
@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements PlayerAccess {

    private boolean waiting = true;

    @Override
    public boolean isWaiting() {
        return this.waiting;
    }

    @Override
    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}
