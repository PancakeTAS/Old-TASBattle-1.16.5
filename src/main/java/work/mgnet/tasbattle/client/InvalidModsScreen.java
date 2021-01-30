package work.mgnet.tasbattle.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class InvalidModsScreen extends Screen {

    private final Text message;

    public InvalidModsScreen(Text title, Text text) {
        super(title);
        this.message = text;
    }

    protected void init() {
        super.init();
        this.addButton(new ButtonWidget(this.width / 2 - 100, 200, 200, 20, ScreenTexts.CANCEL, (buttonWidget) -> {
            this.client.openScreen((net.minecraft.client.gui.screen.Screen)null);
        }));
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.fillGradient(matrices, 0, 0, this.width, this.height, -12574688, -11530224);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 30, 16777215);
        int y = 50;
        for (String line : message.getString().split("\r\n")) {
            drawCenteredText(matrices, this.textRenderer, new LiteralText(line), this.width / 2, y, 16777215);
            y+=10;
        }
        drawCenteredText(matrices, this.textRenderer, new LiteralText("If you think those Mods are okay, tag me in Discord (MCPfannkuchenYT#9745)"), this.width / 2, 180, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

}
