package ca.hicai.musicam;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundGenerator {
    // Number of samples used to fade between tones.
    private static final int FADE_LEN = 0; //10;

    private static interface Note {
        public int getDuration();
        public double getAmplitude();
        public double getFrequency();
        public int getProgress();
        public int progress();
        public Note reset();
        public Note fade(Note n);
        public double getFadeFreq();
        public double getFadeAmp();
    }

    public static abstract class AbstractNote implements Note {
        protected int duration; // duration in hundredths of a second
        protected int progress; // duration played
        protected double ffreq;
        protected double famp;

        public AbstractNote(int duration) {
            this.duration = duration;
        }

        @Override
        public int getDuration() {
            return duration;
        }

        @Override
        public int progress() {
            return progress++;
        }

        @Override
        public int getProgress() {
            return progress;
        }

        @Override
        public Note reset() {
            progress = 0;
            return this;
        }

        @Override
        public double getFadeFreq() {
            return ffreq;
        }

        @Override
        public double getFadeAmp() {
            return famp;
        }

        @Override
        public Note fade(Note n) {
            ffreq = n.getFrequency();
            famp = n.getAmplitude();
            return reset();
        }
    }
    private static class ConcreteNote extends AbstractNote {
        private int pitch;        // distance from A4 in semitones
        private double amplitude; // amplitude from 0 to 1

        public ConcreteNote(int pitch, int duration, double amplitude) {
            super(duration);
            this.pitch = pitch;
            this.amplitude = amplitude;
        }

        // No amplitude = max volume
        public ConcreteNote(int pitch, int duration) {
            this(pitch, duration, 1.0);
        }

        // No pitch / amplitude = silence
        public ConcreteNote(int duration) {
            this(0, duration, 0);
        }

        public int getPitch() {
            return pitch;
        }

        @Override
        public double getAmplitude() {
            return amplitude;
        }

        @Override
        public double getFrequency() {
            return Math.pow(2, pitch / 12.0) * 440;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }
    }

    private static class Track implements Iterable<Note> {
        private List<Note> data = new LinkedList<>();
        private int totalDuration = 0;

        private static final Note SILENCE = new ConcreteNote(0, 1, 0);

        @Override
        public Iterator<Note> iterator() {
            return new Iterator<Note>() {
                // The progress of the current Note.
                private Iterator<Note> delegate = data.iterator();
                private Note current = delegate.hasNext() ? delegate.next() : SILENCE;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Note next() {
                    if (current == SILENCE) {
                        return SILENCE;
                    }
                    if (current.progress() > current.getDuration() - FADE_LEN) {
                        current = delegate.hasNext() ? delegate.next().fade(current) : SILENCE;
                    }
                    return current;
                }

                @Override
                public void remove() {
                    throw new IllegalStateException("can't remove notes from Track iterator");
                }
            };
        }

        public int getTotalDuration() {
            return totalDuration;
        }

        public void addNote(int pitch, int duration, double amplitude) {
            totalDuration += duration;
            data.add(new ConcreteNote(pitch, duration, amplitude));
        }

        public void addNote(Note note) {
            totalDuration += note.getDuration();
            data.add(note);
        }

        public void clear() {
            totalDuration = 0;
            data.clear();
        }

        public void reset() {
            if (data.size() > 0) {
                data.get(0).reset();
            }
        }
    }

    private static final String TAG = "SoundGenerator";
    private static final int SAMPLE_RATE = 44100;

    private Track[] tracks;

    public SoundGenerator(int numTracks) throws NullPointerException, IllegalArgumentException {
        if (numTracks < 1) {
            throw new IllegalArgumentException("numTracks must be positive");
        }

        tracks = new Track[numTracks];
        for (int i = 0; i < numTracks; i++) {
            tracks[i] = new Track();
        }

        Log.d(TAG, "New SoundGenerator created with " + numTracks + " track(s).");
    }

    public void addNote(int track, int pitch, int duration, double amplitude) throws IllegalArgumentException {
        if (track < 0 || track >= tracks.length) {
            throw new IllegalArgumentException("track out of bounds: got " + track + ", expected [0," + tracks.length + ")");
        }
        // We permit all notes on the piano. There are 48 semitones below A4, and 39 above.
        if (pitch < -48 || pitch > 39) {
            throw new IllegalArgumentException("pitch out of bounds: got " + pitch + ", expected [-48,39]");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("duration out of bounds: got " + duration + ", expected (0, +inf)");
        }
        if (amplitude < 0 || amplitude > 1) {
            throw new IllegalArgumentException("amplitude out of bounds: got " + amplitude + ", expected [0,1]");
        }
//        Log.d(TAG, "add note to track " + track + "; pitch: " + pitch + "; duration: " + duration + "; amplitude: " + amplitude);
        tracks[track].addNote(pitch, duration, amplitude);
    }

    public void addSilence(int track, int duration) {
        addNote(track, 0, duration, 0);
    }

    public void play(/*final double freq, final int length*/) {
        //Log.d(TAG, "Playing wave with frequency " + freq + " for " + length + " second(s)");

        new Thread(new Runnable() {
            final Queue<short[]> buffer = new LinkedList<>();
            AtomicBoolean doneBuffering = new AtomicBoolean(false);

            @Override
            public void run() {
                int bufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufSize, AudioTrack.MODE_STREAM);
//                if (audioTrack == null) {
//                    throw new NullPointerException("failed to create AudioTrack");
//                }

                final Thread bufferWriter = new Thread() {
                    @Override
                    public synchronized void run() {
                        // First, find the total duration and set up the iterators. The total
                        // duration is equal to the maximum of the total durations of all tracks.
                        int totalDuration = 0, numTracks = tracks.length;
                        List<Iterator<Note>> iters = new ArrayList<>(numTracks);
                        for (Track t : tracks) {
                            iters.add(t.iterator());
                            totalDuration = Math.max(totalDuration, t.getTotalDuration());
                            t.reset();
                        }

                        final int FADE_SAMPLES = FADE_LEN * SAMPLE_RATE / 100;

                        for (int time = 0; time < totalDuration; time++) {
                            // Mix the samples. We just add together all the samples across all
                            // tracks, then divide by the number of tracks.
                            double[] samples = new double[SAMPLE_RATE / 100];
                            for (Iterator<Note> it : iters) {
                                Note note = it.next();
                                double amp = note.getAmplitude(), freq = note.getFrequency(),
                                    famp = note.getFadeAmp(), ffreq = note.getFadeFreq();
                                int prog = note.getProgress();
                                for (int i = 0; i < SAMPLE_RATE / 100; i++) {
                                    int sampleNum = time * (SAMPLE_RATE / 100) + i;
                                    double sample = amp * Math.sin(2 * Math.PI * freq * sampleNum / SAMPLE_RATE);
                                    if (prog < FADE_LEN && famp > 0.01) {
                                        sample *= (double)(FADE_SAMPLES - prog * SAMPLE_RATE / 100 - i) / FADE_SAMPLES;
                                        sample += famp * Math.sin(2 * Math.PI * ffreq * sampleNum / SAMPLE_RATE)
                                            * (prog * SAMPLE_RATE / 100 + i) / FADE_SAMPLES;
                                        sample /= 2;
                                     }
                                    samples[i] += sample;
                                }
                            }

                            // Convert the samples into shorts and queue them for writing.
                            short[] buf = new short[SAMPLE_RATE / 100];
                            for (int i = 0; i < buf.length; i++) {
                                buf[i] = (short) (samples[i] / numTracks * 32767);
                            }
                            synchronized (buffer) {
                                buffer.add(buf);
                            }
//                            Log.d(TAG, "wrote sample + notified -- " + time);
                            notify();
                        }
                        Log.d(TAG, "done buffering samples; " + totalDuration + " written");
                        doneBuffering.set(true);
                    }
                };
                bufferWriter.start();

                synchronized (bufferWriter) {
                    audioTrack.play();

                    int written = 0;

                    while (true) {
                        // If our buffer is empty, wait for it to be populated before writing.
                        if (buffer.isEmpty()) {
                            if (doneBuffering.get()) {
                                break;
                            }
                            try {
                                Log.d(TAG, "player waiting for bufferWriter");
                                bufferWriter.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        short[] buf;
                        synchronized (buffer) {
                            buf = buffer.remove();
                        }
                        written += audioTrack.write(buf, 0, buf.length);
                    }
                    Log.d(TAG, written + " total bytes written to audio buffer");
                    audioTrack.stop();
                }
                audioTrack.release();
            }
        }).start();

        /*
        short[] buf = new short[SAMPLE_RATE];
        for (int i = 0; i < SAMPLE_RATE; i++) {
            double sample = Math.sin(2 * Math.PI * 440 * i / SAMPLE_RATE);
            buf[i] = (short) (sample * 32767);
        }
        */
    }

    public void clear() {
        for (Track t : tracks) {
            t.clear();
        }
    }
}
