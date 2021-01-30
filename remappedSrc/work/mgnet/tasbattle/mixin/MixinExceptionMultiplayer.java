package work.mgnet.tasbattle.mixin;

import net.minecraft.class_5489;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class MixinExceptionMultiplayer extends Screen {

    @Shadow private class_5489 reasonFormatted;

    @Shadow @Final private Text reason;

    @Shadow private int reasonHeight;

    @Shadow @Final private Screen parent;

    protected MixinExceptionMultiplayer(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void redoinit(CallbackInfo ci) {
        this.reasonFormatted = class_5489.method_30890(this.textRenderer, this.reason, this.width - 50);
        int var10001 = this.reasonFormatted.method_30887();
        this.textRenderer.getClass();
        this.reasonHeight = var10001 * 9;
        int var10003 = this.width / 2 - 100;
        int var10004 = this.height / 2 + this.reasonHeight / 2;
        this.textRenderer.getClass();
        this.addButton(new ButtonWidget(var10003, Math.min(var10004 + 9, this.height - 30), 200, 20, new TranslatableText("Back to Main Menu"), (buttonWidget) -> {
            this.client.openScreen(this.parent);
        }));
        ci.cancel();
    }

}
