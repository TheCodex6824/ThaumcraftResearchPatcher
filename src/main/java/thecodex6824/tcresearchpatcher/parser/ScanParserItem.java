package thecodex6824.tcresearchpatcher.parser;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ScanItem;
import thecodex6824.tcresearchpatcher.api.scan.IScanParser;
import thecodex6824.tcresearchpatcher.json.JsonSchemaException;
import thecodex6824.tcresearchpatcher.json.JsonUtils;

public class ScanParserItem implements IScanParser {

    protected IScanThing parseElement(String key, JsonElement e) {
        if (e.isJsonArray())
            throw new JsonSchemaException("Invalid object entry: must be object or primitive");
        
        if (e.isJsonPrimitive()) {
            ResourceLocation loc = new ResourceLocation(e.getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            if (item == null)
                throw new NullPointerException(key + ": Item " + loc + " does not exist");
            
            return new ScanItem(key, new ItemStack(item));
        }
        else {
            ResourceLocation loc = new ResourceLocation(JsonUtils.getPrimitiveOrThrow("name", e.getAsJsonObject()).getAsString());
            JsonPrimitive damage = JsonUtils.tryGetPrimitive("meta", e.getAsJsonObject()).orNull();
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            if (item == null)
                throw new NullPointerException(key + ": Item " + loc + " does not exist");
            
            int meta = OreDictionary.WILDCARD_VALUE;
            if (damage != null) {
                try {
                    meta = damage.getAsInt();
                }
                catch (NumberFormatException ex) {
                    throw new JsonSchemaException(key + ": Meta value " + damage.getAsString() + " is not valid");
                }
            }
            
            return new ScanItem(key, new ItemStack(item, 1, meta));
        }
    }
    
    @Override
    public boolean matches(ResourceLocation type) {
        return type.getNamespace().equals("thaumcraft") && type.getPath().equals("item");
    }
    
    @Override
    public Collection<IScanThing> parseScan(String key, ResourceLocation type, JsonElement input) {
        if (input.isJsonArray()) {
            ArrayList<IScanThing> things = new ArrayList<>();
            for (JsonElement e : input.getAsJsonArray())
                things.add(parseElement(key, e));
            
            return things;
        }
        else
            return Lists.newArrayList(parseElement(key, input));
    }
    
}
