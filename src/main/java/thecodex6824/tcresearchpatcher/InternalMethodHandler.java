package thecodex6824.tcresearchpatcher;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.JsonElement;

import net.minecraft.util.ResourceLocation;
import thaumcraft.api.research.IScanThing;
import thecodex6824.tcresearchpatcher.api.internal.IInternalMethodHandler;
import thecodex6824.tcresearchpatcher.api.scan.IScanParser;

public class InternalMethodHandler implements IInternalMethodHandler {

    protected static class ScanParserEntry {
        
        public ScanParserEntry(IScanParser p, int w) {
            parser = p;
            weight = w;
        }
        
        public final IScanParser parser;
        public final int weight;
        
    }
    
    protected ArrayList<ScanParserEntry> scanParsers;
    protected boolean scanParsersSorted;
    
    public InternalMethodHandler() {
        scanParsers = new ArrayList<>();
    }
    
    @Override
    public void registerScanParser(IScanParser parser) {
        registerScanParser(parser, 0);
    }
    
    @Override
    public void registerScanParser(IScanParser parser, int weight) {
        scanParsers.add(new ScanParserEntry(parser, weight));
        scanParsersSorted = false;
    }
    
    @Override
    public Collection<IScanThing> parseScans(String key, ResourceLocation type, JsonElement data) {
        if (!scanParsersSorted) {
            scanParsers.sort((e1, e2) -> Integer.compare(e1.weight, e2.weight));
            scanParsersSorted = true;
        }
        
        RuntimeException throwLater = new RuntimeException("No parsers were able to load scan of type " + type);
        for (ScanParserEntry e : scanParsers) {
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
