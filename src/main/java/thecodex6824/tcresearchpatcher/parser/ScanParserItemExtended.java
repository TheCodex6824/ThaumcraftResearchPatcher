package thecodex6824.tcresearchpatcher.parser;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ScanItem;
import thecodex6824.tcresearchpatcher.api.scan.IScanParser;
import thecodex6824.tcresearchpatcher.extended.ScanItemExtended;
import thecodex6824.tcresearchpatcher.json.JsonSchemaException;
import thecodex6824.tcresearchpatcher.json.JsonUtils;

public class ScanParserItemExtended implements IScanParser {

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
            JsonElement nbt = JsonUtils.tryGet("nbt", e.getAsJsonObject()).orNull();
            if (nbt != null && nbt.isJsonArray())
                throw new JsonSchemaException(key + ": nbt must be object or string");
            JsonElement caps = JsonUtils.tryGet("caps", e.getAsJsonObject()).orNull();
            if (caps != null && caps.isJsonArray())
                throw new JsonSchemaException(key + ": caps must be object or string");
            
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            if (item == null)
                throw new NullPointerException(key + ": Item " + loc + " does not exist");
            
            int meta = 0;
            if (damage != null) {
                try {
                    meta = damage.getAsInt();
                }
                catch (NumberFormatException ex) {
                    throw new JsonSchemaException(key + ": Meta value " + damage.getAsString() + " is not valid");
                }
            }
            
            NBTTagCompound capNbt = null;
            if (caps != null) {
                try {
                    capNbt = JsonToNBT.getTagFromJson(caps.isJsonObject() ?
                            new Gson().toJson(caps.getAsJsonObject()) : caps.getAsString());
                }
                catch (NBTException ex) {
                    throw new JsonSchemaException(key + ": Invalid caps nbt: " + ex);
                }
            }
            
            ItemStack stack = new ItemStack(item, 1, meta);
            if (nbt != null) {
                try {
                    stack.setTagCompound(JsonToNBT.getTagFromJson(nbt.isJsonObject() ?
                            new Gson().toJson(nbt.getAsJsonObject()) : nbt.getAsString()));
                }
                catch (NBTException ex) {
                    throw new JsonSchemaException(key + ": Invalid nbt: " + ex);
                }
            }
            
            return new ScanItemExtended(key, stack, capNbt);
        }
    }
    
    @Override
    public boolean matches(ResourceLocation type) {
        return type.getNamespace().equals("tcresearchpatcher") && type.getPath().equals("item_extended");
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
