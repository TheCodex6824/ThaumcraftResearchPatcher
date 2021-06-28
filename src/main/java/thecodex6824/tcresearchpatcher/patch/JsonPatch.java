package thecodex6824.tcresearchpatcher.patch;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;

public class JsonPatch {

    public static enum PatchOp {
        
        ADD("add"),
        REMOVE("remove"),
        REPLACE("replace"),
        COPY("copy"),
        MOVE("move"),
        TEST("test");
        
        private String internalName;
        
        private PatchOp(String name) {
            internalName = name;
        }
        
        @Nullable
        public static PatchOp fromString(String s) {
            for (PatchOp o : values()) {
                if (o.internalName.equals(s))
                    return o;
            }
            
            return null;
        }
        
    }
    
    public final PatchOp op;
    public final String path;
    public final JsonElement meta;
    
    public JsonPatch(PatchOp o, String p, JsonElement m) {
        op = o;
        path = p;
        meta = m;
    }
    
}
