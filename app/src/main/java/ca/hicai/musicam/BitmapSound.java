package ca.hicai.musicam;

import android.graphics.Bitmap;
import android.util.Log;

public class BitmapSound {
    private static final String TAG = "BitmapSound";
    private static final int NUM_TRACKS = 3;

    private int bmpWidth, bmpHeight;
    private int colourVals[];
    private int pixelArray[];
    private SoundGenerator soundGenerator;

    private int pmax(int pixel) {
        int max = -1;
        for (int l0 = 0; l0 < 3; l0++) {
            int cur = pixel % 256;
            if (cur > max) {
                max = cur;
            }
            pixel = pixel >> 8;
        }
        return max;
    }

    private int pmin(int pixel) {
        int min = 257;
        for (int l0 = 0; l0 < 3; l0++) {
            int cur = pixel % 256;
            if (cur < min) {
                min = cur;
            }
            pixel = pixel >> 8;
        }
        return min;
    }

    private int pavg(int pixel) {
        int tot = 0;
        for (int l0 = 0; l0 < 3; l0++) {
            tot += pixel % 256;
            pixel = pixel >> 8;
        }
        return tot / 3;
    }

    private void parseImg(int hScan) {
        int blockWidth = 10;            // number of continuous horizontal pixels to average
        if (hScan < 0) {                // blockWidth can't be too high or else you'll overflow!
            hScan = bmpHeight / 2;
        }
        pixelArray = new int[bmpWidth / blockWidth];
        for (int l0 = 0; l0 < bmpWidth / blockWidth; l0++) {
            int pixel = 0;
            for (int l1 = 0; l1 < blockWidth; l1++) {
                pixel += colourVals[hScan * bmpWidth + l0 + l1] & 0xFFFFFF;
            }
            pixelArray[l0] = pixel / blockWidth;
            Log.d(TAG, "pixelArray " + l0 + " - " + pixelArray[l0]);
        }
    }

    private void synthSounds(int channel) {
        int diff = 0;
        int length = 7;         // in hundredths of a second
        int blockWidth = 1;
        int pixel = 0;

        for (int l0 = 0; l0 < pixelArray.length; l0++) {
            if (l0 % blockWidth != 0) {
                continue;
            }
            if (channel == 0) {         // soprano
                blockWidth = 2;
                diff = 7;
                pixel = pmax(pixelArray[0]);
                for (int l1 = 1; l1 < blockWidth; l1++) {
                    int tmp = pmax(pixelArray[l1]);
                    if (tmp > pixel) {
                        pixel = tmp;
                    }
                }
            } else if (channel == 1) {  // alto/tenor
                blockWidth = 4;
                diff = 0;
                pixel = pavg(pixelArray[0]);
                for (int l1 = 1; l1 < blockWidth; l1++) {
                    pixel += pavg(pixelArray[l1]);
                }
            } else if (channel == 2) {  // bass
                blockWidth = 8;
                diff = -7;
                pixel = pmin(pixelArray[0]);
                for (int l1 = 1; l1 < blockWidth; l1++) {
                    int tmp = pmin(pixelArray[l1]);
                    if (tmp < pixel) {
                        pixel = tmp;
                    }
                }
            }
            diff = (int)Math.round((pixel - 127) / 10.0) + diff;
            if (diff == -72) {
                Log.d(TAG, "pixel: " + pixel + "; pa0: " + pixelArray[0]);
            }
            // prevent static notes
            soundGenerator.addNote(channel, diff, length * blockWidth, 1.0);
        }
    }

    public BitmapSound() {
        soundGenerator = new SoundGenerator(NUM_TRACKS);
    }

    public void setBitmap(Bitmap bitmap) {
        // if invalid bitmap, throw some exception
        bmpWidth = bitmap.getWidth();
        bmpHeight = bitmap.getHeight();

        colourVals = new int[bmpWidth * bmpHeight];

        bitmap.setHasAlpha(true);
        bitmap.getPixels(colourVals, 0, bmpWidth, 0, 0, bmpWidth, bmpHeight);
    }

    public void playImage(int hScan) {
        parseImg(hScan);

        for (int l0 = 0; l0 < NUM_TRACKS; l0++) {
            synthSounds(l0);
        }

        soundGenerator.play();
    }

    public void playImage() {
        playImage(-1);
    }
}
