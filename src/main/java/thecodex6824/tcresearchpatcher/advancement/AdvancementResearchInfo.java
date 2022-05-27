/**
 *  Thaumcraft Research Patcher
 *  Copyright (c) 2022 TheCodex6824.
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

package thecodex6824.tcresearchpatcher.advancement;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.ResourceLocation;
import thecodex6824.tcresearchpatcher.json.JsonSchemaException;
import thecodex6824.tcresearchpatcher.json.JsonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AdvancementResearchInfo {

    protected ResourceLocation advancementKey;
    protected List<String> researchKeys;
    protected Optional<String> researchMessage;

    // backup plan for people that know what they are doing
    // (if you do use this, let me know so I can promote it to the API)
    protected AdvancementResearchInfo() {}

    public AdvancementResearchInfo(JsonObject input) throws JsonSchemaException {
        advancementKey = new ResourceLocation(JsonUtils.getOrThrow("key", input).getAsString());
        JsonElement research = JsonUtils.getOrThrow("research", input, advancementKey.toString());
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (research.isJsonObject())
            throw new JsonSchemaException("Research must be an array or single entry");
        else if (research.isJsonArray()) {
            for (JsonElement e : research.getAsJsonArray()) {
                if (!e.isJsonPrimitive())
                    throw new JsonSchemaException("Research array element must be a string");

                builder.add(e.getAsString());
            }
        }
        else
            builder.add(research.getAsString());

        researchKeys = builder.build();
        JsonElement message = JsonUtils.tryGet("message", input).orNull();
        if (message != null) {
            if (!message.isJsonPrimitive())
                throw new JsonSchemaException("Research message must be a string");

            researchMessage = Optional.of(message.getAsString());
        }
    }

    public ResourceLocation getAdvancementKey() {
        return advancementKey;
    }

    public Collection<String> getResearchKeys() {
        return researchKeys;
    }

    public Optional<String> getResearchMessage() {
        return researchMessage;
    }

}
