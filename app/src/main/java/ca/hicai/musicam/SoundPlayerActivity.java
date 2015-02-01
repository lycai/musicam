package ca.hicai.musicam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;
import android.widget.RatingBar;


public class SoundPlayerActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_player);

        RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(0).setColorFilter(Color.rgb(48, 48, 48), PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(1).setColorFilter(Color.rgb(48, 48, 48), PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(2).setColorFilter(Color.rgb(192, 192, 192), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        Bitmap image = null;
        if (extras != null) {
            image = BitmapFactory.decodeFile(extras.getString(MainActivity.EXTRA_BITMAP));
        }

        if (image == null) {
            // crash!
            throw new IllegalStateException("no bitmap supplied!");
        }

        BitmapSound bitmapSound = new BitmapSound();
        bitmapSound.setBitmap(image);
        bitmapSound.playImage();

        ((ImageView) findViewById(R.id.imageView)).setImageBitmap(image);
    }
}
