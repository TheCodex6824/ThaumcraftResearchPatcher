package thecodex6824.tcresearchpatcher.json;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonUtils {

    public static Optional<JsonElement> tryGet(String key, JsonObject obj) {
        JsonElement e = obj.get(key);
        if (e == null)
            return Optional.absent();
        
        return Optional.of(e);
    }
    
    public static JsonElement getOrThrow(String key, JsonObject obj) throws JsonSchemaException {
        JsonElement e = obj.get(key);
        if (e == null)
            throw new JsonSchemaException("Key " + key + " is missing");
        
        return e;
    }
    
    public static Optional<JsonPrimitive> tryGetPrimitive(String key, JsonObject obj) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonPrimitive())
            return Optional.absent();
        
        return Optional.of(e.getAsJsonPrimitive());
    }
    
    public static JsonPrimitive getPrimitiveOrThrow(String key, JsonObject obj) throws JsonSchemaException {
        JsonElement e = obj.get(key);
        if (e == null)
            throw new JsonSchemaException("Key " + key + " is missing");
        else if (!e.isJsonPrimitive())
            throw new JsonSchemaException("Key " + key + " does not have a primitive value");
        
        return e.getAsJsonPrimitive();
    }
    
    public static Optional<JsonArray> tryGetArray(String key, JsonObject obj) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonArray())
            return Optional.absent();
        
        return Optional.of(e.getAsJsonArray());
    }
    
    public static JsonArray getArrayOrThrow(String key, JsonObject obj) throws JsonSchemaException {
        JsonElement e = obj.get(key);
        if (e == null)
            throw new JsonSchemaException("Key " + key + " is missing");
        else if (!e.isJsonArray())
            throw new JsonSchemaException("Key " + key + " does not have an array value");
        
        return e.getAsJsonArray();
    }
    
    public static Optional<JsonObject> tryGetObject(String key, JsonObject obj) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonObject())
            return Optional.absent();
        
        return Optional.of(e.getAsJsonObject());
    }
    
    public static JsonObject getObjectOrThrow(String key, JsonObject obj) throws JsonSchemaException {
        JsonElement e = obj.get(key);
        if (e == null)
            throw new JsonSchemaException("Key " + key + " is missing");
        else if (!e.isJsonObject())
            throw new JsonSchemaException("Key " + key + " does not have an object value");
        
        return e.getAsJsonObject();
    }
    
    public static List<JsonObject> getObjectOrArrayContainedObjects(JsonElement element) throws JsonSchemaException {
        return getObjectOrArrayContainedObjects(element, false);
    }
    
    public static List<JsonObject> getObjectOrArrayContainedObjects(JsonElement element, boolean allowSkipInvalid) throws JsonSchemaException {
        if (element.isJsonPrimitive())
            throw new JsonSchemaException("Value must be an object or array");
        else if (element.isJsonObject())
            return Lists.newArrayList(element.getAsJsonObject());
        else {
            ArrayList<JsonObject> objects = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                if (e.isJsonObject())
                    objects.add(e.getAsJsonObject());
                else if (!allowSkipInvalid)
                    throw new JsonSchemaException("Array must only contain objects");
            }
            
            return objects;
        }
    }
    
}
