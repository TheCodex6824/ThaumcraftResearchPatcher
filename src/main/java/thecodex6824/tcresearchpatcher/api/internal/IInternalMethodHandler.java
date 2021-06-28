package thecodex6824.tcresearchpatcher.api.internal;

import java.util.Collection;

import com.google.gson.JsonElement;

import net.minecraft.util.ResourceLocation;
import thaumcraft.api.research.IScanThing;
import thecodex6824.tcresearchpatcher.api.scan.IScanParser;

public interface IInternalMethodHandler {

    public void registerScanParser(IScanParser parser);
    
    public void registerScanParser(IScanParser parser, int weight);
    
    public Collection<IScanThing> parseScans(String key, ResourceLocation type, JsonElement data);
    
}
