package org.Azkerash.riftfall.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** Epic Fight 21.17.3.1 shares mutable math scratch between rendering and server collision. */
@Mixin(value = OpenMatrix4f.class, remap = false)
public abstract class EpicFightMatrixScratchMixin {
    @Unique
    private static final ThreadLocal<Vec3f> riftfall$vector = ThreadLocal.withInitial(Vec3f::new);

    @Unique
    private static final ThreadLocal<Vec4f> riftfall$vector4 = ThreadLocal.withInitial(Vec4f::new);

    @Unique
    private static final ThreadLocal<OpenMatrix4f> riftfall$matrix =
            ThreadLocal.withInitial(OpenMatrix4f::new);

    @Unique
    private static final ThreadLocal<FloatBuffer> riftfall$buffer =
            ThreadLocal.withInitial(
                    () ->
                            ByteBuffer.allocateDirect(64)
                                    .order(ByteOrder.nativeOrder())
                                    .asFloatBuffer());

    // Match field reads only, leaving the dependency's static initialization intact.
    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lyesman/epicfight/api/utils/math/OpenMatrix4f;VECTOR_STORAGE:Lyesman/epicfight/api/utils/math/Vec3f;",
                            opcode = Opcodes.GETSTATIC),
            require = 16)
    private static Vec3f riftfall$threadVector() {
        return riftfall$vector.get();
    }

    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lyesman/epicfight/api/utils/math/OpenMatrix4f;VEC4_STORAGE:Lyesman/epicfight/api/utils/math/Vec4f;",
                            opcode = Opcodes.GETSTATIC),
            require = 2)
    private static Vec4f riftfall$threadVector4() {
        return riftfall$vector4.get();
    }

    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lyesman/epicfight/api/utils/math/OpenMatrix4f;MATRIX_STORAGE:Lyesman/epicfight/api/utils/math/OpenMatrix4f;",
                            opcode = Opcodes.GETSTATIC),
            require = 71)
    private static OpenMatrix4f riftfall$threadMatrix() {
        return riftfall$matrix.get();
    }

    @Redirect(
            method = "*",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lyesman/epicfight/api/utils/math/OpenMatrix4f;MATRIX_TRANSFORMER:Ljava/nio/FloatBuffer;",
                            opcode = Opcodes.GETSTATIC),
            require = 7)
    private static FloatBuffer riftfall$threadBuffer() {
        return riftfall$buffer.get();
    }
}
