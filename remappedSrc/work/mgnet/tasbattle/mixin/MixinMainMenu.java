package work.mgnet.tasbattle.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.client.InvalidModsScreen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class MixinMainMenu extends Screen {

    @Shadow
    public String splashText;
    @Shadow
    public int copyrightTextWidth;
    @Shadow
    public int copyrightTextX;
    @Shadow
    public static Identifier ACCESSIBILITY_ICON_TEXTURE;

    public List<String> modWhitelist = ImmutableList.of("sodium", "phosphor", "lithium", "opti", "fabricapi", "fabric-api", "fabric_api", "borderless", "tas", "menu", "dynamic", "desktop.ini", "replaymod");

    protected MixinMainMenu(Text title) {
        super(title);
    }

    @Inject(at = @At(value = "HEAD"), cancellable = true, method = "init")
    public void redoinit(CallbackInfo ci) {
        if (this.splashText == null) {
            this.splashText = "Time for TAS Battles";
        }

        this.copyrightTextWidth = this.textRenderer.getWidth("Copyright Mojang AB. Do not distribute!");
        this.copyrightTextX = this.width - this.copyrightTextWidth - 2;
        int j = this.height / 4 + 48;

        this.addButton(new ButtonWidget(this.width / 2 - 100, j, 200, 20, new TranslatableText("Connect to 1.16.5 TASBattle Server"), (buttonWidget) -> {

            List<String> forbiddenMods = new ArrayList<>();
            for (File mod : new File("mods").listFiles()) {
                boolean allowed = false;
                for (String white : modWhitelist) {
                    if (mod.getName().toLowerCase().contains(white.toLowerCase())) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    System.out.println("Mod " + mod.getName() + " is not allowed!!!");
                    forbiddenMods.add(mod.getName());
                }
            }
            if (forbiddenMods.size() != 0) {
                String msg = "Following Mods are forbidden on the TASBattle Server:\r\n";
                for (String mod : forbiddenMods) {
                    msg = msg + "\r\n" + mod;
                }
                MinecraftClient.getInstance().openScreen(new InvalidModsScreen(new LiteralText("Forbidden Mods found!"), new LiteralText(msg)));
            } else {
                client.openScreen(new ConnectScreen(this, client, "mgnet.work", 1164));
            }
        }));
        // DEV
        this.addButton(new ButtonWidget(this.width / 2 - 100, j + 50, 200, 20, new TranslatableText("§bConnect to localhost"), (buttonWidget) -> {
            client.openScreen(new ConnectScreen(this, client, "127.0.0.1", 25565));
        }));
        this.addButton(new TexturedButtonWidget(this.width / 2 - 124, j + 72 + 12, 20, 20, 0, 106, 20, ButtonWidget.WIDGETS_LOCATION, 256, 256, (buttonWidget) -> {
            this.client.openScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager()));
        }, new TranslatableText("narrator.button.language")));
        this.addButton(new ButtonWidget(this.width / 2 - 100, j + 72 + 12, 98, 20, new TranslatableText("menu.options"), (buttonWidget) -> {
            this.client.openScreen(new OptionsScreen(this, this.client.options));
        }));
        this.addButton(new ButtonWidget(this.width / 2 + 2, j + 72 + 12, 98, 20, new TranslatableText("menu.quit"), (buttonWidget) -> {
            this.client.scheduleStop();
        }));
        this.addButton(new TexturedButtonWidget(this.width / 2 + 104, j + 72 + 12, 20, 20, 0, 0, 20, ACCESSIBILITY_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
            this.client.openScreen(new AccessibilityOptionsScreen(this, this.client.options));
        }, new TranslatableText("narrator.button.accessibility")));
        this.client.setConnectedToRealms(false);
        ci.cancel();
    }

}
