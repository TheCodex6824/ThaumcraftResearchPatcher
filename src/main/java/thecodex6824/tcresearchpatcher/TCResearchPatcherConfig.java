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

package thecodex6824.tcresearchpatcher;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RequiresMcRestart;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thecodex6824.tcresearchpatcher.api.ThaumcraftResearchPatcherApi;

@EventBusSubscriber
@Config(modid = ThaumcraftResearchPatcherApi.MODID)
public final class TCResearchPatcherConfig {

    private TCResearchPatcherConfig() {}
    
    @Name("CategoriesToRemove")
    @Comment({
        "The keys of categories that should be deleted.",
        "Note that categories are not automatically deleted (even when having no entries) due to addon incompatibilities."
    })
    @RequiresMcRestart
    public static String[] removedCategories = {};
    
    @SubscribeEvent
    public static void onConfigReload(ConfigChangedEvent event) {
        ConfigManager.sync(ThaumcraftResearchPatcherApi.MODID, Type.INSTANCE);
    }
    
}
