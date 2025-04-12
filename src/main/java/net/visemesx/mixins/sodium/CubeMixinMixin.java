package net.visemesx.mixins.sodium;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.visemesx.render.CustomCuboid;
@Pseudo
@Mixin(Cuboid.class)
public class CubeMixinMixin {

    @Dynamic
    @TargetHandler(
            mixin = "net.caffeinemc.mods.sodium.mixin.features.render.entity.CubeMixin",
            name = "onCompile",
            prefix = "handler"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void helloSodium(Entry pose, VertexConsumer buffer, int light, int overlay, int color, CallbackInfo a, CallbackInfo b) {
        Cuboid cuboid = (Cuboid) (Object) this;
        if (!(cuboid instanceof CustomCuboid)) {
            return;
        }
        b.cancel();
    }
}
