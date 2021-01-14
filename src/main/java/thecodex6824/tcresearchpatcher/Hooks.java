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
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.lib.research.ResearchManager;

public final class Hooks {

    private Hooks() {}
    
    private static final HashMap<String, ArrayList<ArrayList<JSONPatch>>> PATCHES = new HashMap<>();
    
    // gson exposes deep copy in a later version than forge ships in 1.12
    private static final Method JSON_DEEP_COPY;
    private static final Method PARSE_RESEARCH_JSON;
    private static final Method ADD_RESEARCH_TO_CATEGORY;
    
    private static final Logger LOG;
    
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
        
        LOG = TCResearchPatcher.getLogger();
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
                    ArrayList<JsonObject> objects = new ArrayList<>();
                    if (element.isJsonArray()) {
                        for (JsonElement e : element.getAsJsonArray()) {
                            if (e.isJsonObject())
                                objects.add(e.getAsJsonObject());
                            else
                                log.error(f.getName() + ": " + e + ": Invalid json entry: array entry not an object");
                        }
                    }
                    else if (element.isJsonObject())
                        objects.add(element.getAsJsonObject());
                    else
                        log.error(f.getName() + ": " + element + ": Invalid json entry: top level not an array or object");
                
                    for (JsonObject o : objects) {
                        JsonElement key = o.get("key");
                        if (key == null || !key.isJsonPrimitive())
                            log.error(f.getName() + ": Invalid json entry: missing/invalid key");
                        else {
                            JsonElement ops = o.get("ops");
                            if (ops == null || !ops.isJsonArray())
                                log.error(f.getName() + ": Invalid json entry: missing/invalid ops");
                            else {
                                ArrayList<JSONPatch> insertTo = new ArrayList<>();
                                for (JsonElement e : ops.getAsJsonArray()) {
                                    if (!e.isJsonObject())
                                        log.error(f.getName() + ": " + e + ": Invalid patch entry: not an object");
                                    else {
                                        JsonObject check = e.getAsJsonObject();
                                        JsonElement path = check.get("path");
                                        if (path == null || !path.isJsonPrimitive()) {
                                            log.error(f.getName() + ": Invalid patch entry: invalid/missing path");
                                            continue;
                                        }
                                        
                                        JsonElement op = check.get("op");
                                        if (op == null || !op.isJsonPrimitive()) {
                                            log.error(f.getName() + ": Invalid patch entry: invalid/missing op");
                                            continue;
                                        }
                                    
                                        JsonElement meta = null;
                                        switch (op.getAsString()) {
                                            case "add":
                                            case "replace":
                                            case "test":
                                                meta = check.get("value");
                                                if (meta == null) {
                                                    log.error(f.getName() + ": Invalid patch entry: missing value");
                                                    continue;
                                                }
                                                break;
                                            case "copy":
                                            case "move":
                                                meta = check.get("from");
                                                if (meta == null) {
                                                    log.error(f.getName() + ": Invalid patch entry: missing from");
                                                    continue;
                                                }
                                                break;
                                            case "remove": break;
                                            default:
                                                log.error(f.getName() + ": " + op + ": Invalid patch entry: invalid op");
                                                continue;
                                        }
                                        
                                        insertTo.add(new JSONPatch(JSONPatch.PatchOp.fromString(op.getAsString()), path.getAsString(), meta != null ? meta : JsonNull.INSTANCE));
                                    }
                                }
                                
                                ArrayList<ArrayList<JSONPatch>> list = PATCHES.get(key.getAsString());
                                if (list == null) {
                                    list = new ArrayList<>();
                                    PATCHES.put(key.getAsString(), list);
                                }
                                
                                list.add(insertTo);
                            }
                        }
                    }
                }
                catch (Exception ex) {
                    log.error(f.getName() + ": Error reading file: " + ex.getMessage());
                }
            }
        }
        else if (!patchFolder.exists())
            patchFolder.mkdirs();
        else
            log.warn("Not loading any patches, folder(s) missing");
        
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
                        log.error(f.getName() + ": " + element + ": Invalid json entry: top level not an object");
                    else {
                        JsonElement entries = element.getAsJsonObject().get("entries");
                        if (entries == null || !entries.isJsonArray())
                            log.error(f.getName() + ": " + entries + ": Invalid json entry: entries is missing or not an array");
                        else {
                            int i = 0;
                            for (JsonElement e : entries.getAsJsonArray()) {
                                if (e.isJsonObject()) {
                                    JsonObject entry = e.getAsJsonObject();
                                    try {
                                        ResearchEntry researchEntry = parseResearchJson(entry);
                                        addResearchToCategory(researchEntry);
                                        ++i;
                                    }
                                    catch (Exception ex) {
                                        log.error(f.getName() + ": " + e + ": Invalid research entry: error parsing entry");
                                        ex.printStackTrace();
                                    }
                                }
                                else
                                    log.error(f.getName() + ": " + e + ": Invalid research entry: not an object");
                              
                            }
                            
                            log.info(f.getName() + ": loaded " + i + " entries");
                        }
                    }
                }
                catch (Exception ex) {
                    log.error(f.getName() + ": Error reading file: " + ex.getMessage());
                }
            }
        }
        else if (!entryFolder.exists())
            entryFolder.mkdirs();
        else
            log.warn("Not loading any entries, folder(s) missing");
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
    
    private static JsonElement getValue(JsonElement parent, String child) {
        if (parent.isJsonObject())
            return parent.getAsJsonObject().get(child);
        else if (parent.isJsonArray()) {
            JsonArray array = parent.getAsJsonArray();
            int index = -1;
            if (child.equals("-")) {
                LOG.error("Path component - is not a valid array index here");
                return null;
            }
            else {
                try {
                    index = Integer.parseInt(child);
                }
                catch (NumberFormatException ex) {
                    LOG.error("Path component " + child + " is not a valid array index");
                    return null;
                }
                
                if (index < 0 || index >= array.size()) {
                    LOG.error("Path component " + child + " is not a valid array index");
                    return null;
                }
            }
            
            return array.get(index);
        }
        
        return null;
    }
    
    private static void patchAdd(JsonElement parent, String child, JsonElement meta) {
        Logger log = TCResearchPatcher.getLogger();
        if (parent.isJsonObject())
            parent.getAsJsonObject().add(child, meta);
        else if (parent.isJsonArray()) {
            JsonArray array = parent.getAsJsonArray();
            int index = -1;
            if (child.equals("-"))
                index = array.size();
            else {
                try {
                    index = Integer.parseInt(child);
                }
                catch (NumberFormatException ex) {
                    log.error("Path component " + child + " is not a valid array index");
                    return;
                }
                
                if (index < 0 || index > array.size()) {
                    log.error("Path component " + child + " is not a valid array index");
                    return;
                }
            }
            
            if (index == array.size())
                array.add(meta);
            else {
                JsonElement[] temp = new JsonElement[array.size() - index];
                for (int i = index; i < array.size(); ++i)
                    temp[i - index] = array.get(i);
                
                array.set(index, meta);
                for (int i = index + 1; i < array.size(); ++i)
                    array.set(i, temp[i - index - 1]);
                
                array.add(temp[temp.length - 1]);
            }
        }
    }
    
    @Nullable
    private static JsonElement patchRemove(JsonElement parent, String child) {
        Logger log = TCResearchPatcher.getLogger();
        if (parent.isJsonObject())
            return parent.getAsJsonObject().remove(child);
        else if (parent.isJsonArray()) {
            JsonArray array = parent.getAsJsonArray();
            int index = -1;
            if (child.equals("-")) {
                log.error("Path component - is not a valid array index for remove operations");
                return null;
            }
            else {
                try {
                    index = Integer.parseInt(child);
                }
                catch (NumberFormatException ex) {
                    log.error("Path component " + child + " is not a valid array index");
                    return null;
                }
                
                if (index < 0 || index >= array.size()) {
                    log.error("Path component " + child + " is not a valid array index");
                    return null;
                }
            }
            
            return array.remove(index);
        }
        
        return null;
    }
    
    @Nullable
    private static JsonElement patchTest(JsonElement parent, String child) {
        if (parent.isJsonObject())
            return parent.getAsJsonObject().get(child);
        else if (parent.isJsonArray()) {
            Logger log = TCResearchPatcher.getLogger();
            JsonArray array = parent.getAsJsonArray();
            int index = -1;
            if (child.equals("-")) {
                log.error("Path component - is not a valid array index for test operations");
                return null;
            }
            else {
                try {
                    index = Integer.parseInt(child);
                }
                catch (NumberFormatException ex) {
                    log.error("Path component " + child + " is not a valid array index");
                    return null;
                }
                
                if (index < 0 || index >= array.size()) {
                    log.error("Path component " + child + " is not a valid array index");
                    return null;
                }
            }
            
            return array.get(index);
        }
        
        return null;
    }
    
    @Nullable
    private static Pair<JsonElement, String> parsePath(JsonElement top, String fullPath) {
        JsonElement parent = top;
        String[] path = fullPath.split("/");
        for (int i = 0; i < path.length; ++i) {
            path[i] = path[i].replace("~1", "/").replace("~0", "~");
            if (i < path.length - 1) {
                if (parent.isJsonObject()) {
                    parent = parent.getAsJsonObject().get(path[i]);
                    if (parent == null) {
                        LOG.error("Path component " + path[i] + " not found");
                        return null;
                    }
                }
                else if (parent.isJsonArray()) {
                    int index = -1;
                    if (path[i].equals("-")) {
                        LOG.error("Path component " + path[i] + " is not allowed here");
                        return null;
                    }
                    else {
                        try {
                            index = Integer.parseInt(path[i]);
                        }
                        catch (NumberFormatException ex) {
                            LOG.error("Path component " + path[i] + " is not a valid array index");
                            return null;
                        }
                    }
                    
                    parent = parent.getAsJsonArray().get(index);
                    if (parent == null) {
                        LOG.error("Path component " + path[i] + " not found");
                        return null;
                    }
                }
                else {
                    LOG.error("Path component " + path[i] + " is not an object or array");
                    return null;
                }
            }
        }
        
        return Pair.of(parent, path[path.length - 1]);
    }
    
    public static void patchResearchJSON(JsonObject json) {
        Logger log = TCResearchPatcher.getLogger();
        JsonElement key = json.get("key");
        if (key != null && key.isJsonPrimitive()) {
            ArrayList<ArrayList<JSONPatch>> apply = PATCHES.get(key.getAsString());
            if (apply != null) {
                for (ArrayList<JSONPatch> patchList : apply) {
                    if (!patchList.isEmpty()) {
                        boolean applyChanges = true;
                        JsonObject working = deepCopy(json);
                        for (JSONPatch p : patchList) {
                            Pair<JsonElement, String> path = parsePath(working, p.path);
                            if (path == null) {
                                applyChanges = false;
                                break;
                            }
                            
                            if (applyChanges) {
                                switch (p.op) {
                                    case ADD:
                                        patchAdd(path.getLeft(), path.getRight(), p.meta);
                                        break;
                                    case REMOVE:
                                        if (patchRemove(path.getLeft(), path.getRight()) == null)
                                            log.warn("Key " + path.getRight() + " was supposed to be removed, but already did not exist");
                                        
                                        break;
                                    case COPY:
                                        Pair<JsonElement, String> from = parsePath(working, p.meta.getAsString());
                                        if (from == null)
                                            applyChanges = false;
                                        else {
                                            JsonElement val = getValue(from.getLeft(), from.getRight());
                                            if (val == null)
                                                applyChanges = false;
                                            else
                                                patchAdd(path.getLeft(), path.getRight(), val);
                                        }
                                        
                                        break;
                                    case MOVE:
                                        from = parsePath(working, p.meta.getAsString());
                                        if (from == null)
                                            applyChanges = false;
                                        else {
                                            JsonElement val = patchRemove(from.getLeft(), from.getRight());
                                            if (val == null)
                                                applyChanges = false;
                                            else
                                                patchAdd(path.getLeft(), path.getRight(), val);
                                        }
                                        
                                        break;
                                    case REPLACE:
                                        if (patchRemove(path.getLeft(), path.getRight()) == null)
                                            log.warn("Key " + path.getRight() + " did not exist for replace");
                                        
                                        patchAdd(path.getLeft(), path.getRight(), p.meta);
                                        break;
                                    case TEST:
                                        JsonElement val = patchTest(path.getLeft(), path.getRight());
                                        if (val == null)
                                            log.warn("Key " + path.getRight() + " did not exist for test");
                                        else if (!val.equals(p.meta))
                                            applyChanges = false;
                                        
                                        break;
                                    default: break;
                                }
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
