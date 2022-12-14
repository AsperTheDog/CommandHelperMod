package net.fabricmc.cmdBlockHelper.mixin;
import net.fabricmc.cmdBlockHelper.CmdIDEScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(targets = "net/minecraft/client/gui/screen/ingame/AbstractCommandBlockScreen")
public class CommandBlockScreenMixin extends Screen {
    protected ButtonWidget IDEButton;

    protected CommandBlockScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method="init")
    private void addIDEButton(CallbackInfo ci) {
        this.IDEButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("IDE"), (button) -> {
            this.client.setScreen(new CmdIDEScreen(this, "test"));
        }).dimensions(this.width / 2 + 160, 50, 26, 20).build());
    }
}
