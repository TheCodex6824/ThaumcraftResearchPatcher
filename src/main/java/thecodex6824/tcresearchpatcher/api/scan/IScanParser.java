/*
 *  Thaumcraft Research Patcher
 *  Copyright (c) 2023 TheCodex6824.
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

package thecodex6824.tcresearchpatcher.api.scan;

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
