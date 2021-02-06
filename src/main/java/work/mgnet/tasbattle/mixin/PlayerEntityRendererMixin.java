package work.mgnet.tasbattle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.client.CapeRenderer;
import work.mgnet.tasbattle.client.TASBattleClient;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin  extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public PlayerEntityRendererMixin(EntityRenderDispatcher dispatcher,
                                     PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(dispatcher, model, shadowRadius);
    }

    @Inject(method = "renderLabelIfPresent", at = @At("RETURN"), cancellable = true)
    public void injectrenderLabelIfPresent(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text22, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {

        if (!TASBattleClient.titles.containsKey(abstractClientPlayerEntity.getDisplayName().getString())) return;
        Text text = new LiteralText(TASBattleClient.titles.get(abstractClientPlayerEntity.getDisplayName().getString())); //Prefix

        double d = this.dispatcher.getSquaredDistanceToCamera(abstractClientPlayerEntity);
        matrixStack.push();
        if (!(d > 4096.0D)) {
            boolean bl = !abstractClientPlayerEntity.isSneaky();
            float f = abstractClientPlayerEntity.getHeight() + 0.5F;
            matrixStack.push();
            matrixStack.translate(0.0D, (double)f, 0.0D);
            matrixStack.multiply(this.dispatcher.getRotation());
            matrixStack.scale(-0.015F, -0.015F, 0.015F);
            Matrix4f matrix4f = matrixStack.peek().getModel();
            float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
            int j = (int)(g * 255.0F) << 24;
            TextRenderer textRenderer = this.getFontRenderer();
            float h = (float)(-textRenderer.getWidth(text) / 2);
            textRenderer.draw(text, h, 15, 553648127, false, matrix4f, vertexConsumerProvider, bl, j, i);
            if (bl) {
                textRenderer.draw(text, h, 15, -1, false, matrix4f, vertexConsumerProvider, false, 0, i);
            }

            matrixStack.pop();
        }
        matrixStack.pop();
    }

    @Inject(method = {"<init>(Lnet/minecraft/client/render/entity/EntityRenderDispatcher;Z)V"}, at = @At("RETURN"))
    private void ConstructorMixinPlayerEntityRenderer(EntityRenderDispatcher dispatcher, boolean bl, CallbackInfo info) {
        this.addFeature(new CapeRenderer(this));
    }
}