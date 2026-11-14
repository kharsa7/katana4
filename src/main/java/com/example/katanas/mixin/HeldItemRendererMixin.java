package com.example.katanas.mixin;

import com.example.katanas.client.KatanaAnimator;
import com.example.katanas.item.KatanaItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes over first-person rendering for katanas only; every other item is untouched.
 * require = 0 + "required": false in the mixin config: if a Minecraft update changes the target, the mod
 * still loads (katanas just fall back to the vanilla swing).
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode,
                                    boolean leftHanded, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void elementalkatanas$renderKatana(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                               Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                               MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                               int light, CallbackInfo ci) {
        if (!(item.getItem() instanceof KatanaItem katana)) {
            return;
        }
        boolean mainHand = hand == Hand.MAIN_HAND;
        Arm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == Arm.RIGHT;

        KatanaAnimator.transform(matrices, katana, right, mainHand, swingProgress, equipProgress,
                player.age + tickDelta);

        this.renderItem(player, item,
                right ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                !right, matrices, vertexConsumers, light);
        ci.cancel();
    }
}
