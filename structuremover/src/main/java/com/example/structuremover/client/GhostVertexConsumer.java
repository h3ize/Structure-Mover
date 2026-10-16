package com.example.structuremover.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Forwards everything to another consumer but scales the alpha, which turns normal block quads into see-through ones. */
final class GhostVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;

    GhostVertexConsumer(VertexConsumer delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, (int) (a * alpha)); return this; }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
}
