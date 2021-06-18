package thecodex6824.tcresearchpatcher.api;

import java.util.Collection;

import com.google.gson.JsonElement;

import net.minecraft.util.ResourceLocation;
import thaumcraft.api.research.IScanThing;

/**
 * Interface for registering parsers that convert JSON into a Thaumcraft IScanThing.
 * @author TheCodex6824
 */
public interface IScanParser {

    public boolean matches(ResourceLocation type);
    
    public Collection<IScanThing> parseScan(String key, ResourceLocation type, JsonElement input);
    
}
