package com.example.structuremover.client;

/** Client-side copy of the selected building's outer blocks, used to draw the transparent placement preview. */
public final class ClientSelectionCache {
    private static volatile long[] shell = new long[0];

    public static long[] get() { return shell; }
    public static void set(long[] positions) { shell = positions; }

    private ClientSelectionCache() {}
}
