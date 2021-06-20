package thecodex6824.tcresearchpatcher;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ScanningManager;
import thecodex6824.tcresearchpatcher.api.ThaumcraftResearchPatcherApi;
import thecodex6824.tcresearchpatcher.json.JsonSchemaException;
import thecodex6824.tcresearchpatcher.json.JsonUtils;
import thecodex6824.tcresearchpatcher.parser.ScanParserBlock;
import thecodex6824.tcresearchpatcher.parser.ScanParserEntity;
import thecodex6824.tcresearchpatcher.parser.ScanParserItem;
import thecodex6824.tcresearchpatcher.parser.ScanParserItemExtended;

@Mod(modid = ThaumcraftResearchPatcherApi.MODID, name = ThaumcraftResearchPatcherApi.NAME, version = ThaumcraftResearchPatcherApi.VERSION,
    certificateFingerprint = "@FINGERPRINT@", useMetadata = true)
public class TCResearchPatcherContainer {
    
    @EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event) {
        TCResearchPatcher.getLogger().error("Thaumcraft Research Patcher is expecting signature {}, however there is no signature matching that description", event.getExpectedFingerprint());
    }
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ThaumcraftResearchPatcherApi.registerScanParser(new ScanParserBlock(), 1000);
        ThaumcraftResearchPatcherApi.registerScanParser(new ScanParserItem(), 1000);
        ThaumcraftResearchPatcherApi.registerScanParser(new ScanParserItemExtended(), 1000);
        ThaumcraftResearchPatcherApi.registerScanParser(new ScanParserEntity(), 1000);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        Logger log = TCResearchPatcher.getLogger();
        JsonParser parser = new JsonParser();
        File cats = new File("config/tcresearchpatcher", "categories.json");
        if (cats.isFile()) {
            try (FileInputStream s = new FileInputStream(cats)) {
                String content = IOUtils.toString(s, StandardCharsets.UTF_8);
                JsonElement element = parser.parse(content);
                List<JsonObject> objects = JsonUtils.getObjectOrArrayContainedObjects(element);
                for (JsonObject o : objects) {
                    JsonPrimitive key = JsonUtils.getPrimitiveOrThrow("key", o);
                    JsonPrimitive requirement = JsonUtils.getPrimitiveOrThrow("requirement", o);
                    AspectList list = new AspectList();
                    JsonObject aspects = JsonUtils.getObjectOrThrow("aspects", o);
                    for (Map.Entry<String, JsonElement> pair : aspects.entrySet()) {
                        if (pair.getValue().isJsonPrimitive()) {
                            Aspect aspect = Aspect.getAspect(pair.getKey());
                            if (aspect == null)
                                throw new JsonSchemaException(aspects + ": Invalid aspect entry: invalid aspect tag");
                            
                            int amount = -1;
                            try {
                                amount = pair.getValue().getAsInt();
                            }
                            catch (ClassCastException ex) {
                                throw new JsonSchemaException(aspects + ": Invalid aspect entry: invalid amount");
                            }
                            
                            if (amount > 0)
                                list.add(aspect, amount);
                        }
                    }
                    
                    JsonPrimitive icon = JsonUtils.getPrimitiveOrThrow("icon", o);
                    JsonPrimitive background = JsonUtils.getPrimitiveOrThrow("background", o);
                    JsonPrimitive backgroundOverlay = JsonUtils.getPrimitiveOrThrow("backgroundOverlay", o);
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
        
        File scans = new File("config/tcresearchpatcher", "scans");
        if (scans.isDirectory()) {
            File[] files = scans.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile() && f.getName().endsWith(".json");
                }
            });
            for (File f : files) {
                try (FileInputStream s = new FileInputStream(f)) {
                    String content = IOUtils.toString(s, StandardCharsets.UTF_8);
                    JsonElement element = parser.parse(content);
                    List<JsonObject> objects = JsonUtils.getObjectOrArrayContainedObjects(element);
                    int scanKeys = 0;
                    int totalScans = 0;
                    for (JsonObject o : objects) {
                        String key = JsonUtils.getPrimitiveOrThrow("key", o).getAsString();
                        ResourceLocation type = new ResourceLocation(JsonUtils.getPrimitiveOrThrow("type", o).getAsString());
                        JsonElement obj = JsonUtils.getOrThrow("object", o);
                        for (IScanThing thing : ThaumcraftResearchPatcherApi.parseScans(key, type, obj)) {
                            ScanningManager.addScannableThing(thing);
                            ++totalScans;
                        }
                        
                        ++scanKeys;
                    }
                    
                    log.info("scans/" + f.getName() + ": loaded " + scanKeys + " scan keys with " + totalScans + " scan entries");
                }
                catch (Exception ex) {
                    log.error("scans/" + f.getName() + ": Error reading file: " + ex.getMessage());
                    for (Throwable t : ex.getSuppressed())
                        log.error(t.getMessage());
                }
            }
        }
    }
    
}
