package ca.hicai.musicam;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import me.yvesli.testbed.R;


public class TestActivity extends ActionBarActivity {

    private SoundGenerator soundGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        final Button button = (Button)findViewById(R.id.playbutton);
        final EditText freq = (EditText)findViewById(R.id.freq);
        final EditText len = (EditText)findViewById(R.id.len);

        soundGenerator = new SoundGenerator();

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
    }

    @Override
    protected void onDestroy() {
        soundGenerator.dispose();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
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
