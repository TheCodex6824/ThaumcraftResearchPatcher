package thecodex6824.tcresearchpatcher.api;

import java.util.Collection;

import com.google.gson.JsonElement;

import net.minecraft.util.ResourceLocation;
import thaumcraft.api.research.IScanThing;
import thecodex6824.tcresearchpatcher.api.internal.IInternalMethodHandler;
import thecodex6824.tcresearchpatcher.api.scan.IScanParser;

public final class ThaumcraftResearchPatcherApi {

    private ThaumcraftResearchPatcherApi() {}
    
    public static final String MODID = "tcresearchpatcher";
    public static final String NAME = "Thaumcraft Research Patcher";
    public static final String PROVIDES = MODID + "api";
    public static final String API_VERSION = "@APIVERSION@";
    
    private static IInternalMethodHandler handler;
    
    public static void setInternalMethodHandler(IInternalMethodHandler h) {
        handler = h;
    }
    
    public static void registerScanParser(IScanParser parser) {
        handler.registerScanParser(parser, 0);
    }
    
    public static void registerScanParser(IScanParser parser, int weight) {
        handler.registerScanParser(parser, weight);
    }
    
    public static Collection<IScanThing> parseScans(String key, ResourceLocation type, JsonElement data) {
        return handler.parseScans(key, type, data);
    }
    
}
