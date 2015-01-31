package ca.hicai.musicam;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by yves on 30/01/15.
 */
public class SoundGenerator {

    private static final String TAG = "SoundGenerator";
    private static final int SAMPLE_RATE = 44100;
    private AudioTrack track;

    public SoundGenerator() {
        int minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBuf, AudioTrack.MODE_STREAM);
        Log.d(TAG, "Creating AudioTrack with minimum buffer " + minBuf);
    }

    public void dispose() {
        track.release();
    }

    public void play(final double freq, final int length) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Playing wave with frequency " + freq + " for " + length + " second(s)");

                // generate five seconds worth of sound, in one-second increments
                short[] buf = new short[SAMPLE_RATE * length];

                for (int i = 0; i < SAMPLE_RATE * length; i++) {
                    double sample = Math.sin(2 * Math.PI * freq * i / SAMPLE_RATE);
                    // clamp sample
                    if (sample > 1) {
                        sample = 1;
                    } else if (sample < -1) {
                        sample = -1;
                    }
                    buf[i] = (short)(sample * 32767);
                }
                track.play();
                Log.d(TAG, "Done writing buffer");
                int status = track.write(buf, 0, buf.length);
                Log.d(TAG, "Write returned with status " + status);
            }
        }).start();
    }

    public void playRandom(int length) {
        int note = (int)(Math.random() * 21) - 10;
        Log.d(TAG, "Playing random tone -- " + note);
        play(Math.pow(2, note / 12.0) * 440, length);
    }
}
