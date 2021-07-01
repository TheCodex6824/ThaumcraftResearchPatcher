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
