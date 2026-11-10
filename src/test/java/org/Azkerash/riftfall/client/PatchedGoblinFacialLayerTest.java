package org.Azkerash.riftfall.client;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

class PatchedGoblinFacialLayerTest {
    @Test void forwardsEpicFightsFourthFloatPartialTicksRatherThanItsThirdFloatHeadPitch() throws Exception {
        List<Integer> forwarded=new ArrayList<>();
        try(var stream=getClass().getResourceAsStream("/org/Azkerash/riftfall/client/PatchedGoblinFacialLayer.class")) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        int lastFloatSlot=-1;
                        @Override public void visitVarInsn(int opcode,int slot){if(opcode==Opcodes.FLOAD)lastFloatSlot=slot;}
                        @Override public void visitMethodInsn(int opcode,String owner,String method,String desc,boolean isInterface) {
                            if(owner.endsWith("/GoblinFacialLayer") && method.equals("renderFace"))forwarded.add(lastFloatSlot);
                        }
                    };
                }
            },ClassReader.SKIP_FRAMES);
        }
        // Instance + seven reference/int arguments, then bob=8, yRot=9, xRot=10, partialTicks=11.
        assertEquals(List.of(11),forwarded);
    }
}
