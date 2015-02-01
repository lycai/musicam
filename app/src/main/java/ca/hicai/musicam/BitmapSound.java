package ca.hicai.musicam;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class BitmapSound {
    private static final String TAG = "BitmapSound";
    private static final int NUM_TRACKS = 3;
    private static final int PARSE_BLOCK_WIDTH = 15;
    private static final int[] REDUCE_BLOCK_WIDTHS = { 2, 4, 8 };
    private static final boolean MINOR_KEY = false;

    private int bmpWidth, bmpHeight;
    private int colourVals[];
    private int pixelArray[][];
    private SoundGenerator soundGenerator;

    private int bindScale(int deltaTones, int offset, boolean isMinor) {
        deltaTones += offset;
        int semitones = 12 * (deltaTones / 7);
        int remainder = deltaTones % 7;
        if (remainder < 0) {
            semitones -= 12;
            remainder += 7;
        }
        semitones += remainder * 2;
        if (remainder >= (isMinor ? 2 : 3)) {
            semitones -= 1;
        }
        if (isMinor && remainder == 5) {
            semitones -= 1;
        }
        return semitones;
    }

    private int bindScale(int semitone, int offset) {
        return bindScale(semitone, offset, MINOR_KEY);
    }

    private int bindScale(int semitone) {
        return bindScale(semitone, 0);
    }

    private void parseImg(int hScan) {
        if (hScan < 0) {
            hScan = bmpHeight / 2;
        }
        pixelArray = new int[bmpWidth / PARSE_BLOCK_WIDTH][3];
        for (int l0 = 0; l0 < bmpWidth / PARSE_BLOCK_WIDTH; l0++) {
            int[] pixel = { 0, 0, 0 }; // note: BGR !!
            for (int l1 = 0; l1 < PARSE_BLOCK_WIDTH; l1++) {
                int packed = colourVals[hScan * bmpWidth + l0 * PARSE_BLOCK_WIDTH + l1];
                pixel[0] += Color.red(packed);
                pixel[1] += Color.green(packed);
                pixel[2] += Color.blue(packed);
            }
            String log = "colourVals " + l0 + " -";
            for (int l2 = 0; l2 < 3; l2++) {
                pixelArray[l0][l2] = pixel[l2] / PARSE_BLOCK_WIDTH;
                log += " " + pixelArray[l0][l2];
            }
            //Log.d(TAG, log);
        }
    }

    private interface MapFn {
        public int map(int[] x);
    }
    private interface RedFn {
        public int reduce(int x, int y);
    }

    private int mapReduce(int[][] arr, int idx, int num, MapFn map, RedFn reduce) {
        int ret = 0;
        for (int i = idx; i <= idx + num && i < arr.length; i++) {
            if (i == idx) {
                ret = map.map(arr[i]);
            } else {
                ret = reduce.reduce(ret, map.map(arr[idx]));
            }
        }
        return ret;
    }

    private void synthSounds(int channel) {
        int diff = 0;
        int length = 7;         // in hundredths of a second
        int pixel;

        for (int l0 = 0; l0 < pixelArray.length; l0++) {
            if (l0 % REDUCE_BLOCK_WIDTHS[channel] != 0) {
                continue;
            }
            MapFn fmap = null;
            RedFn fred = null;
            if (channel == 0) {         // soprano
                diff = 7;
                fmap = new MapFn() {
                    @Override
                    public int map(int[] x) {
                        return Math.max(Math.max(x[0], x[1]), x[2]);
                    }
                };
                fred = new RedFn() {
                    @Override
                    public int reduce(int x, int y) {
                        return Math.max(x, y);
                    }
                };
            } else if (channel == 1) {  // alto/tenor
                diff = 0;
                fmap = new MapFn() {
                    @Override
                    public int map(int[] x) {
                        return (x[0] + x[1] + x[2]) / 3 / 4;
                        // divide again by 4 because we can't take average after with M-R
                    }
                };
                fred = new RedFn() {
                    @Override
                    public int reduce(int x, int y) {
                        return x + y;
                    }
                };
            } else if (channel == 2) {  // bass
                diff = -7;
                fmap = new MapFn() {
                    @Override
                    public int map(int[] x) {
                        return Math.min(Math.min(x[0], x[1]), x[2]);
                    }
                };
                fred = new RedFn() {
                    @Override
                    public int reduce(int x, int y) {
                        return Math.min(x, y);
                    }
                };
            }
            pixel = mapReduce(pixelArray, l0, REDUCE_BLOCK_WIDTHS[channel], fmap, fred);
            diff = (int)Math.round((pixel / REDUCE_BLOCK_WIDTHS[channel] - 127) / 10.0) + diff;
            if (diff == 54) {
                Log.d(TAG, "pixel: " + pixel + "; pa0: " + pixelArray[0] + "; channel: " + channel);
            }
            // prevent static notes
            soundGenerator.addNote(channel, bindScale(diff), length * REDUCE_BLOCK_WIDTHS[channel], 1.0);
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
