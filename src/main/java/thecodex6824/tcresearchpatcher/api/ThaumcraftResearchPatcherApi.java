package thecodex6824.tcresearchpatcher.api;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.JsonElement;

import net.minecraft.util.ResourceLocation;
import thaumcraft.api.research.IScanThing;

public final class ThaumcraftResearchPatcherApi {

    private ThaumcraftResearchPatcherApi() {}
    
    public static final String MODID = "tcresearchpatcher";
    public static final String NAME = "Thaumcraft Research Patcher";
    public static final String VERSION = "@VERSION@";
    public static final String PROVIDES = MODID + "api";
    public static final String API_VERSION = "@APIVERSION@";
    
    private static class ScanParserEntry {
        
        public ScanParserEntry(IScanParser p, int w) {
            parser = p;
            weight = w;
        }
        
        public final IScanParser parser;
        public final int weight;
        
    }
    
    private static final ArrayList<ScanParserEntry> SCAN_PARSERS = new ArrayList<>();
    private static boolean scanParsersSorted = false;
    
    public static void registerScanParser(IScanParser parser) {
        registerScanParser(parser, 0);
    }
    
    public static void registerScanParser(IScanParser parser, int weight) {
        SCAN_PARSERS.add(new ScanParserEntry(parser, weight));
        scanParsersSorted = false;
    }
    
    public static Collection<IScanThing> parseScans(String key, ResourceLocation type, JsonElement data) {
        if (!scanParsersSorted) {
            SCAN_PARSERS.sort((e1, e2) -> Integer.compare(e1.weight, e2.weight));
            scanParsersSorted = true;
        }
        
        RuntimeException throwLater = new RuntimeException("No parsers were able to load scan of type " + type);
        for (ScanParserEntry e : SCAN_PARSERS) {
            IScanParser parser = e.parser;
            if (parser.matches(type)) {
                try {
                    return parser.parseScan(key, type, data);
                }
                catch (Exception ex) {
                    throwLater.addSuppressed(ex);
                }
            }
        }
        
        throw throwLater;
    }
    
}
