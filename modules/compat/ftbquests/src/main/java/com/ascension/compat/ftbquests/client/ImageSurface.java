package com.ascension.compat.ftbquests.client;

import com.mojang.blaze3d.platform.NativeImage;

/** Lets {@link MarkerPainter} draw into a texture's own pixels. */
record ImageSurface(NativeImage image) implements MarkerPainter.Surface {

    @Override
    public int size() {
        return image.getWidth();
    }

    @Override
    public void blend(int x, int y, int argb) {
        image.setPixelRGBA(x, y, sourceOver(argb, image.getPixelRGBA(x, y)));
    }

    @Override
    public void clear(int x, int y, int width, int height) {
        image.fillRect(x, y, width, height, 0);
    }

    /**
     * Source-over of an {@code 0xAARRGGBB} colour on a native pixel (which is stored as {@code 0xAABBGGRR}),
     * returning a native pixel.
     */
    static int sourceOver(int argb, int nativeBelow) {
        int sa = argb >>> 24;
        int sr = (argb >> 16) & 0xFF;
        int sg = (argb >> 8) & 0xFF;
        int sb = argb & 0xFF;

        int da = nativeBelow >>> 24;
        int db = (nativeBelow >> 16) & 0xFF;
        int dg = (nativeBelow >> 8) & 0xFF;
        int dr = nativeBelow & 0xFF;

        double a = sa / 255.0;
        double b = da / 255.0;
        double outA = a + b * (1.0 - a);
        if (outA <= 0.0) {
            return 0;
        }
        int r = (int) Math.round((sr * a + dr * b * (1.0 - a)) / outA);
        int g = (int) Math.round((sg * a + dg * b * (1.0 - a)) / outA);
        int bl = (int) Math.round((sb * a + db * b * (1.0 - a)) / outA);
        int oa = (int) Math.round(outA * 255.0);
        return (oa << 24) | (bl << 16) | (g << 8) | r;
    }
}
