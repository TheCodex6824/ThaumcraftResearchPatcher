package thecodex6824.tcresearchpatcher.extended;

import javax.annotation.Nullable;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.research.IScanThing;

public class ScanItemExtended implements IScanThing {

    protected String research;
    protected ItemStack ref;
    
    @Nullable
    protected NBTTagCompound caps;
    
    public ScanItemExtended(String research, ItemStack stack) {
        this(research, stack, null);
    }
    
    public ScanItemExtended(String research, ItemStack stack, @Nullable NBTTagCompound capabilities) {
        this.research = research;
        ref = stack;
        caps = capabilities;
    }
    
    @Override
    public boolean checkThing(EntityPlayer player, Object thing) {
        if (thing == null)
            return false;
        
        ItemStack stack = null;
        if (thing instanceof EntityItem)
            stack = ((EntityItem) thing).getItem();
        else if (thing instanceof ItemStack)
            stack = (ItemStack) thing;
        
        if (stack == null || stack.isEmpty() ||
                !ThaumcraftInvHelper.areItemStacksEqualForCrafting(stack, ref))
            return false;
        
        // can't use ItemStack#areCapsCompatible directly because we want to allow extra capabilities
        NBTTagCompound stackTag = stack.serializeNBT();
        boolean refCaps = caps != null;
        boolean stackCaps = stackTag.hasKey("ForgeCaps", NBT.TAG_COMPOUND);
        if (!refCaps && !stackCaps)
            return true;
        else if (refCaps ^ stackCaps)
            return false;
        
        // copy over any extra caps not present in the json
        // this ensures that they will match in areCapsCompatible
        NBTTagCompound capsRef = caps.copy();
        NBTTagCompound capsStack = stackTag.getCompoundTag("ForgeCaps");
        for (String key : capsStack.getKeySet()) {
            if (!capsRef.hasKey(key, capsStack.getTagId(key)))
                capsRef.setTag(key, capsStack.getTag(key));
        }
        
        NBTTagCompound refTag = ref.serializeNBT();
        refTag.setTag("ForgeCaps", capsRef);
        return new ItemStack(stackTag).areCapsCompatible(new ItemStack(refTag));
    }
    
    @Override
    public String getResearchKey(EntityPlayer player, Object thing) {
        return research;
    }
    
}
