package ca.hicai.musicam;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class SoundGeneratorTestActivity extends ActionBarActivity {

    private static final String TAG = "SoundPlayerActivity";
    private static final int NUM_TRACKS = 2;

    private SoundGenerator soundGenerator;
    private boolean wasSong = false;
    private Button addButton;
    private Button playButton;
    private EditText track;
    private EditText pitch;
    private EditText duration;
    private EditText amplitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soundgenerator_tester);

        soundGenerator = new SoundGenerator(NUM_TRACKS);

        addButton = (Button) findViewById(R.id.addButton);
        playButton = (Button) findViewById(R.id.playbutton);
        track = (EditText) findViewById(R.id.track);
        pitch = (EditText) findViewById(R.id.pitch);
        duration = (EditText) findViewById(R.id.duration);
        amplitude = (EditText) findViewById(R.id.amplitude);
        /*
        button.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("TestActivity", "button clicked. freq: " + freq.getText().toString() + "; len: " + len.getText().toString());
                    int n = 0, dur = 1;
                    try {
                        n = Integer.parseInt(freq.getText().toString());
                    } catch (Exception e) {
                        // ignore
                    }
                    try {
                        dur = Integer.parseInt(len.getText().toString());
                    } catch (Exception e) {
                        // ignore
                    }
                    if (n > 26 && n < 4000) {
                        soundGenerator.play(n, dur);
                    } else {
                        soundGenerator.playRandom(dur);
                    }
                }
            }
        );
        */
    }

    public void addNote(View v) {
        Log.d(TAG, "AddNote clicked");
        int t, p, d;
        double a;
        try {
            t = Integer.parseInt(track.getText().toString());
            p = Integer.parseInt(pitch.getText().toString());
            d = Integer.parseInt(duration.getText().toString());
            a = Double.parseDouble(amplitude.getText().toString());

            Log.d(TAG, "Parsed values: " + p + ", " + d + ", " + a);

            if (wasSong) {
                wasSong = false;
                soundGenerator.clear();
            }
            soundGenerator.addNote(t, p, d, a);
        } catch (IllegalArgumentException ex) {
            Toast.makeText(getApplicationContext(), "Invalid input!", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    public void playSong(View v) {
        soundGenerator.play();
    }

    public void makeSong(View v) {
        soundGenerator.clear();
        int[] notes = { 3, 5, 7, 8, 10, 12, 14, 15 };
        for (int i = 0; i < notes.length; i++) {
            soundGenerator.addNote(0, notes[i], 50, 0.6 + 0.4 * i / 8);
            soundGenerator.addNote(1, notes[notes.length - i - 1], 50, 0.6 + 0.4 * i / 8);
        }
        // soundGenerator.addSilence(0, 100);
        for (int i = notes.length - 1; i >= 0; i--) {
            soundGenerator.addNote(0, notes[i], 50, 0.6 + 0.4 * i / 8);
            soundGenerator.addNote(1, notes[notes.length - i - 1], 50, 0.6 + 0.4 * i / 8);
        }
        wasSong = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_soundplayer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
