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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.lib.research.ResearchManager;
import thecodex6824.tcresearchpatcher.json.JsonSchemaException;
import thecodex6824.tcresearchpatcher.json.JsonUtils;
import thecodex6824.tcresearchpatcher.patch.JSONPatch;
import thecodex6824.tcresearchpatcher.patch.PatchHelper;

public final class Hooks {

    private Hooks() {}
    
    private static final HashMap<String, ArrayList<ArrayList<JSONPatch>>> PATCHES = new HashMap<>();
    
    // gson exposes deep copy in a later version than forge ships in 1.12
    private static final Method JSON_DEEP_COPY;
    private static final Method PARSE_RESEARCH_JSON;
    private static final Method ADD_RESEARCH_TO_CATEGORY;
    
    static {
        Method temp = null;
        try {
            temp = JsonElement.class.getDeclaredMethod("deepCopy");
            temp.setAccessible(true);
        }
        catch (Exception ex) {
            TCResearchPatcher.getLogger().error("Could not access JsonElement#deepCopy");
        }
        JSON_DEEP_COPY = temp;
        
        try {
            temp = ResearchManager.class.getDeclaredMethod("parseResearchJson", JsonObject.class);
            temp.setAccessible(true);
        }
        catch (Exception ex) {
            TCResearchPatcher.getLogger().error("Could not access ResearchManager#parseResearchJson");
        }
        PARSE_RESEARCH_JSON = temp;
        
        try {
            temp = ResearchManager.class.getDeclaredMethod("addResearchToCategory", ResearchEntry.class);
            temp.setAccessible(true);
        }
        catch (Exception ex) {
            TCResearchPatcher.getLogger().error("Could not access ResearchManager#parseResearchJson");
        }
        ADD_RESEARCH_TO_CATEGORY = temp;
    }
    
    private static ResearchEntry parseResearchJson(JsonObject entry) {
        try {
            return (ResearchEntry) PARSE_RESEARCH_JSON.invoke(null, entry);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static void addResearchToCategory(ResearchEntry entry) {
        try {
            ADD_RESEARCH_TO_CATEGORY.invoke(null, entry);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static void onEnterParseResearch() {
        // in case we are reloading and something broke the exit patch
        PATCHES.clear();
        Logger log = TCResearchPatcher.getLogger();
        JsonParser parser = new JsonParser();
        File patchFolder = new File("config/tcresearchpatcher", "patches");
        if (patchFolder.isDirectory()) {
            File[] files = patchFolder.listFiles(new FileFilter() {
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
                    for (JsonObject o : objects) {
                        JsonPrimitive key = JsonUtils.getPrimitiveOrThrow("key", o);
                        JsonArray ops = JsonUtils.getArrayOrThrow("ops", o);
                        ArrayList<JSONPatch> insertTo = new ArrayList<>();
                        for (JsonElement e : ops.getAsJsonArray()) {
                            if (!e.isJsonObject())
                                throw new JsonSchemaException(e + ": Patch entry not an object");
                            
                            insertTo.add(PatchHelper.parsePatch(e.getAsJsonObject()));
                        }
                        
                        ArrayList<ArrayList<JSONPatch>> list = PATCHES.get(key.getAsString());
                        if (list == null) {
                            list = new ArrayList<>();
                            PATCHES.put(key.getAsString(), list);
                        }
                        
                        list.add(insertTo);
                    }
                }
                catch (Exception ex) {
                    log.error("patches/" + f.getName() + ": Error reading file: " + ex.getMessage());
                }
            }
        }
        
        File entryFolder = new File("config/tcresearchpatcher", "entries");
        if (entryFolder.isDirectory()) {
            File[] files = entryFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile() && f.getName().endsWith(".json");
                }
            });
            for (File f : files) {
                try (FileInputStream s = new FileInputStream(f)) {
                    String content = IOUtils.toString(s, StandardCharsets.UTF_8);
                    JsonElement element = parser.parse(content);
                    if (!element.isJsonObject())
                        throw new JsonSchemaException("top level not an object");
                    
                    JsonArray entries = JsonUtils.getArrayOrThrow("entries", element.getAsJsonObject());
                    int i = 0;
                    for (JsonElement e : entries.getAsJsonArray()) {
                        if (!e.isJsonObject())
                            throw new JsonSchemaException(e + ": not an object");
                        
                        JsonObject entry = e.getAsJsonObject();
                        ResearchEntry researchEntry = parseResearchJson(entry);
                        addResearchToCategory(researchEntry);
                        ++i;
                    }
                    
                    log.info("entries/" + f.getName() + ": loaded " + i + " entries");
                }
                catch (Exception ex) {
                    log.error("entries/" + f.getName() + ": Error reading file: " + ex.getMessage());
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends JsonElement> T deepCopy(T element) {
        try {
            return (T) JSON_DEEP_COPY.invoke(element);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static void patchResearchJSON(JsonObject json) {
        Logger log = TCResearchPatcher.getLogger();
        JsonElement key = json.get("key");
        if (key == null || !key.isJsonPrimitive())
            log.error("Research entry key is missing, this should never happen!");
        else {
            ArrayList<ArrayList<JSONPatch>> apply = PATCHES.get(key.getAsString());
            if (apply != null) {
                for (ArrayList<JSONPatch> patchList : apply) {
                    if (!patchList.isEmpty()) {
                        boolean applyChanges = true;
                        JsonObject working = deepCopy(json);
                        for (JSONPatch p : patchList) {
                            try {
                                applyChanges &= PatchHelper.applyPatch(working, p);
                            }
                            catch (JsonSchemaException ex) {
                                log.warn(ex.getMessage());
                                applyChanges = false;
                            }
                            
                            if (!applyChanges)
                                break;
                        }
                        
                        if (applyChanges) {
                            // removing directly results in CME
                            HashSet<String> toRemove = new HashSet<>();
                            for (Map.Entry<String, JsonElement> entry : json.entrySet())
                                toRemove.add(entry.getKey());
                            
                            for (String s : toRemove)
                                json.remove(s);
                            
                            for (Map.Entry<String, JsonElement> entry : working.entrySet())
                                json.add(entry.getKey(), deepCopy(entry.getValue()));
                        }
                    }
                }
            }
        }
    }
    
    public static void onExitParseResearch() {
        // no need to waste memory on holding patches
        PATCHES.clear();
    }
    
}
