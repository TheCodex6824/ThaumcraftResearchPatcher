/**
 *  Thaumcraft Research Patcher
 *  Copyright (c) 2021 TheCodex6824.
 *
 *  This file is part of Thaumcraft Research Patcher.
 *
 *  Thaumcraft Research Patcher is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Thaumcraft Research Patcher is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Thaumcraft Research Patcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package thecodex6824.tcresearchpatcher;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ScanningManager;
import thecodex6824.tcresearchpatcher.api.ThaumcraftResearchPatcherApi;
import thecodex6824.tcresearchpatcher.json.JsonSchemaException;
import thecodex6824.tcresearchpatcher.json.JsonUtils;
import thecodex6824.tcresearchpatcher.parser.ScanParserBlock;
import thecodex6824.tcresearchpatcher.parser.ScanParserEntity;
import thecodex6824.tcresearchpatcher.parser.ScanParserItem;
import thecodex6824.tcresearchpatcher.parser.ScanParserItemExtended;

@Mod(modid = ThaumcraftResearchPatcherApi.MODID, name = ThaumcraftResearchPatcherApi.NAME, version = "@VERSION@",
    certificateFingerprint = "@FINGERPRINT@", useMetadata = true)
public class TCResearchPatcherContainer {
    
    @EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event) {
        TCResearchPatcher.getLogger().error("Thaumcraft Research Patcher is expecting signature {}, however there is no signature matching that description", event.getExpectedFingerprint());
    }
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ThaumcraftResearchPatcherApi.setInternalMethodHandler(new InternalMethodHandler());
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
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Set<String> toRemove = new HashSet<>();
        for (Entry<String, ResearchCategory> entry : ResearchCategories.researchCategories.entrySet()) {
            if (entry.getValue().research.isEmpty())
                toRemove.add(entry.getKey());
        }
        
        Logger log = TCResearchPatcher.getLogger();
        for (String s : toRemove) {
            log.info("Removing research category " + s + " due to it having no entries");
            ResearchCategories.researchCategories.remove(s);
        }
    }
    
}
