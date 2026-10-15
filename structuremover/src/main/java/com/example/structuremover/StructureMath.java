package com.example.structuremover;

import net.minecraft.world.level.block.Rotation;

public final class StructureMath {
    /** Rotates an x/z offset clockwise (seen from above) in 90 degree steps. */
    public static int[] rotate(int dx, int dz, int steps) {
        for (int i = 0; i < (steps & 3); i++) {
            int nx = -dz;
            int nz = dx;
            dx = nx;
            dz = nz;
        }
        return new int[] { dx, dz };
    }

    public static Rotation toRotation(int steps) {
        return switch (steps & 3) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private StructureMath() {}
}
