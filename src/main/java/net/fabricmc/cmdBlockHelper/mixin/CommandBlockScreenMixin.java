package net.fabricmc.cmdBlockHelper.mixin;
import net.fabricmc.cmdBlockHelper.CmdIDEScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/gui/screen/ingame/AbstractCommandBlockScreen")
public abstract class CommandBlockScreenMixin extends Screen {
    protected ButtonWidget IDEButton;

    protected CommandBlockScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method="init")
    private void addIDEButton(CallbackInfo ci) {
        AbstractCommandBlockScreen target = ((AbstractCommandBlockScreen)(Object)this);
        this.IDEButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("IDE"), (button) ->
                this.client.setScreen(new CmdIDEScreen(target, "test"))
        ).dimensions(this.width / 2 + 160, 50, 26, 20).build());
    }
}
