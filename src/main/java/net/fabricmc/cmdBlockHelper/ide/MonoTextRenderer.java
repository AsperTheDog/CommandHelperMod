package net.fabricmc.cmdBlockHelper.ide;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.FontType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.ArrayList;
import java.util.List;

public class MonoTextRenderer {
    private static final String id = "monotextrenderer";
    // Creates a textRenderer that will use the fixedsys font, which is monospace
    // The IDE needs monospace so that the columns are vertially aligned
    // How in the world is minecraft's font not monospace???
    public static TextRenderer generateTextRenderer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Font> list = new ArrayList<>();
        // TODO: This probably should not be hardcoded...
        //TODO: The font has to be edited so that stuff is at the center, a lot of objects are shifted to the right
        // we should use this, exported fonts work perfectly: https://www.glyphrstudio.com/online/
        String json = """
                {
                  "providers": [
                    {
                      "type": "ttf",
                      "file": "minecraft:fixedsys.ttf",
                      "shift": [0, 1],
                      "size": 12.0,
                      "oversample": 16.0
                    }
                  ]
                }""";
        // Json black magic. Source code: https://www.reddit.com/r/fabricmc/comments/qfzixs/how_can_i_load_and_draw_ttf_fonts/
        JsonArray jsonArray = JsonHelper.getArray(JsonHelper.deserialize(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(), json, JsonObject.class), "providers");
        for(int i = jsonArray.size() - 1; i >= 0; --i) {
            JsonObject jsonObject = JsonHelper.asObject(jsonArray.get(i), "providers[" + i + "]");
            try {
                String stringType = JsonHelper.getString(jsonObject, "type");
                FontType fontType = FontType.byId(stringType);
                Font font = fontType.createLoader(jsonObject).load(mc.getResourceManager());
                if (font != null)
                    list.add(font);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        // Not sure exactly what this id is, but it seems that it can be anything
        FontStorage storage = new FontStorage(mc.getTextureManager(), new Identifier(id));
        storage.setFonts(list);
        return new TextRenderer((id) -> storage, false);
    }

}
