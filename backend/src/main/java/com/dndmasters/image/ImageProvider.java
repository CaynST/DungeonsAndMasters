package com.dndmasters.image;

/** Abstraction over scene image generation. A real provider can be plugged in later. */
public interface ImageProvider {

    String id();

    /** Returns an image URL for the given prompt (a local placeholder by default). */
    String imageFor(String prompt, String fallback);
}
