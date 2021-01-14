package thecodex6824.tcresearchpatcher;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;

@Mod(modid = "tcresearchpatcher", name = "Thaumcraft Research Patcher", version = "@VERSION@",
    certificateFingerprint = "@FINGERPRINT@", useMetadata = true)
public class TCResearchPatcherContainer {

    @EventHandler
    public static void onFingerPrintViolation(FMLFingerprintViolationEvent event) {
        TCResearchPatcher.getLogger().error("Thaumcraft Research Patcher is expecting signature {}, however there is no signature matching that description", event.getExpectedFingerprint());
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        Logger log = TCResearchPatcher.getLogger();
        File cats = new File("config/tcresearchpatcher", "categories.json");
        if (cats.isFile()) {
            try (FileInputStream s = new FileInputStream(cats)) {
                String content = IOUtils.toString(s, StandardCharsets.UTF_8);
                JsonElement element = new JsonParser().parse(content);
                ArrayList<JsonObject> objects = new ArrayList<>();
                if (element.isJsonArray()) {
                    for (JsonElement e : element.getAsJsonArray()) {
                        if (e.isJsonObject())
                            objects.add(e.getAsJsonObject());
                        else
                            log.error("categories.json: " + e + ": Invalid json entry: array entry not an object");
                    }
                }
                else if (element.isJsonObject())
                    objects.add(element.getAsJsonObject());
                
                for (JsonObject o : objects) {
                    JsonElement key = o.get("key");
                    if (key == null || !key.isJsonPrimitive()) {
                        log.error("categories.json: " + o + ": Invalid category entry: invalid/missing key");
                        continue;
                    }
                    
                    JsonElement requirement = o.get("requirement");
                    if (requirement == null || !requirement.isJsonPrimitive()) {
                        log.error("categories.json: " + o + ": Invalid category entry: invalid/missing requirement");
                        continue;
                    }
                
                    AspectList list = new AspectList();
                    JsonElement aspects = o.get("aspects");
                    if (aspects != null && aspects.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> pair : aspects.getAsJsonObject().entrySet()) {
                            if (pair.getValue().isJsonPrimitive()) {
                                Aspect aspect = Aspect.getAspect(pair.getKey());
                                if (aspect == null)
                                    log.error("categories.json: " + aspects + ": Invalid aspect entry: invalid aspect tag");
                                else {
                                    int amount = -1;
                                    try {
                                        amount = pair.getValue().getAsInt();
                                    }
                                    catch (ClassCastException ex) {
                                        log.error("categories.json: " + aspects + ": Invalid aspect entry: invalid amount");
                                    }
                                    if (amount > 0)
                                        list.add(aspect, amount);
                                }
                            }
                        }
                    }
                    
                    JsonElement icon = o.get("icon");
                    if (icon == null || !icon.isJsonPrimitive()) {
                        log.error("categories.json: " + o + ": Invalid category entry: invalid/missing icon");
                        continue;
                    }
                    
                    JsonElement background = o.get("background");
                    if (background == null || !background.isJsonPrimitive()) {
                        log.error("categories.json: " + o + ": Invalid category entry: invalid/missing background");
                        continue;
                    }
                    
                    JsonElement backgroundOverlay = o.get("backgroundOverlay");
                    if (backgroundOverlay == null || !backgroundOverlay.isJsonPrimitive()) {
                        log.error("categories.json: " + o + ": Invalid category entry: invalid/missing backgroundOverlay");
                        continue;
                    }
                    
                    ResearchCategories.registerCategory(key.getAsString(), requirement.getAsString(), list,
                            new ResourceLocation(icon.getAsString()), new ResourceLocation(background.getAsString()),
                            new ResourceLocation(backgroundOverlay.getAsString())
                    );
                }
            }
            catch (Exception ex) {
                log.error("categories.json: Error reading file: " + ex.getMessage());
            }
        }
    }
    
}
