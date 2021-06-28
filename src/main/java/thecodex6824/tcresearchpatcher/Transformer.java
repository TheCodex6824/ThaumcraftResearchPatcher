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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class Transformer implements IClassTransformer {

    private void patchEntry(MethodNode node) {
        node.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                TransformUtil.HOOKS,
                "onEnterParseResearch",
                "()V",
                false
        ));
    }
    
    private void patchParse(MethodNode node) {
        int i = TransformUtil.findFirstInstanceOfMethodCall(node, 0,
                "parseResearchJson",
                "(Lcom/google/gson/JsonObject;)Lthaumcraft/api/research/ResearchEntry;",
                "thaumcraft/common/lib/research/ResearchManager"
        );
        if (i != -1) {
            Label label = new Label();
            InsnList insns = new InsnList();
            insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                TransformUtil.HOOKS,
                "patchResearchJSON",
                "(Lcom/google/gson/JsonObject;)Z",
                false
            ));
            insns.add(new JumpInsnNode(Opcodes.IFEQ, new LabelNode(label)));
            insns.add(new IincInsnNode(8, 1));
            insns.add(new VarInsnNode(Opcodes.ALOAD, 11));
            node.instructions.insert(node.instructions.get(i).getPrevious(), insns);
            i += 5;
            
            i = TransformUtil.findFirstInstanceOfMethodCall(node, i,
                "addResearchToCategory",
                "(Lthaumcraft/api/research/ResearchEntry;)V",
                "thaumcraft/common/lib/research/ResearchManager"
            );
            insns = new InsnList();
            insns.add(new LabelNode(label));
            insns.add(new IincInsnNode(8, -1));
            node.instructions.insert(node.instructions.get(i).getNext(), insns);
        }
    }
    
    private void patchExit(MethodNode node) {
        int i = 0;
        while ((i = TransformUtil.findFirstInstanceOfOpcode(node, i, Opcodes.RETURN)) != -1) {
            node.instructions.insert(node.instructions.get(i).getPrevious(), new MethodInsnNode(Opcodes.INVOKESTATIC,
                    TransformUtil.HOOKS,
                    "onExitParseResearch",
                    "()V",
                    false
            ));
            
            i += 2;
        }
    }
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("thaumcraft.common.lib.research.ResearchManager")) {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(basicClass);
            reader.accept(node, 0);
            MethodNode parse = TransformUtil.findMethod(node, "parseAllResearch");
            if (parse == null)
                throw new RuntimeException("Could not locate ResearchManager#parseAllResearch");
            
            patchEntry(parse);
            patchParse(parse);
            patchExit(parse);
            
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    return "java/lang/Object";
                }
            };
            node.accept(writer);
            return writer.toByteArray();
        }
        
        return basicClass;
    }
    
}
