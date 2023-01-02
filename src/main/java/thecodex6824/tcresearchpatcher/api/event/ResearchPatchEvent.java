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

package thecodex6824.tcresearchpatcher.api.event;

import com.google.gson.JsonObject;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Event superclass for the beginning and end of the research patching process.
 * These events will fire regardless of whether any patches are being applied to that research.
 * Subscribing to this event will notify the callback for all subclasses of this class.
 * @author TheCodex6824
 */
public class ResearchPatchEvent extends Event {

    protected final JsonObject json;
    
    protected ResearchPatchEvent(JsonObject researchJson) {
        json = researchJson;
    }
    
    public JsonObject getResearchJson() {
        return json;
    }
    
    /**
     * Event that fires before any patches are applied to this research (if any).
     * <ul>
     * <li>A result of DENY will stop this research from being loaded.
     * <li>A result of ALLOW will not apply patches, but allow the research to be loaded.
     * <li>A result of DEFAULT will apply patches as normal.
     * </ul><p>
     * Canceling this event has the same effect as returning a result of DENY.
     * @author TheCodex6824
     */
    @HasResult
    @Cancelable
    public static class Pre extends ResearchPatchEvent {
        
        public Pre(JsonObject researchJson) {
            super(researchJson);
        }
        
    }
    
    /**
     * Event that fires after any patches are applied to this research (if any).
     * Canceling this event will stop this research from being loaded.
     * <p>
     * This event will not fire if {@link ResearchPatchEvent.Pre} stopped this research from loading,
     * or the research is otherwise unable to be loaded (i.e. missing a key).
     * @author TheCodex6824
     */
    @Cancelable
    public static class Post extends ResearchPatchEvent {
        
        public Post(JsonObject researchJson) {
            super(researchJson);
        }
        
    }
    
}
