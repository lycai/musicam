package ca.hicai.musicam;

import android.graphics.Bitmap;

public class BitmapSound {
    private static final String TAG = "BitmapSound";
    private static final int NUM_TRACKS = 2;

    private int bmpWidth, bmpHeight;
    private int colourVals[];
    private SoundGenerator soundGenerator;

    public BitmapSound() {
        soundGenerator = new SoundGenerator(NUM_TRACKS);
    }

    public void setBitmap(Bitmap bitmap) {
        // if invalid bitmap, throw some exception
        bmpWidth = bitmap.getWidth();
        bmpHeight = bitmap.getHeight();
        bitmap.getPixels(colourVals, 0, bmpWidth, 0, 0, bmpWidth, bmpHeight);
    }

    public void playImage(int hScan) {

    }
}
