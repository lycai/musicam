package ca.hicai.musicam;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SoundPlayerActivity extends ActionBarActivity {
    public static interface Callback {
        public void call(double d);
    }

    private static final String TAG = "SoundPlayerActivity";

    ProgressBar progressBar;
    RatingBar ratingBar;
    SharedPreferences pref;
    SharedPreferences.Editor prefe;
    String md5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_player);

        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(0).setColorFilter(Color.rgb(48, 48, 48), PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(1).setColorFilter(Color.rgb(48, 48, 48), PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(2).setColorFilter(Color.rgb(192, 192, 192), PorterDuff.Mode.SRC_ATOP);

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (md5 != null && pref != null) {
                    Log.d(TAG, "rated image " + rating);
                    prefe.putFloat(md5, rating);
                    prefe.commit();
                }
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.bringToFront();

        pref = getPreferences(Context.MODE_PRIVATE);
        prefe = pref.edit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        Bitmap image = null;

        if (extras != null) {
            image = BitmapFactory.decodeFile(extras.getString(MainActivity.EXTRA_BITMAP));

            if (image == null) {
                Uri uri = (Uri) extras.get(MainActivity.EXTRA_BITMAP_URI);
                if (uri != null) {
                    try {
                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    } catch (Exception e) {
                        Log.w(TAG, "can't get image...", e);
                    }
                }
            }
        }

        if (image == null) {
            // crash!
            throw new IllegalStateException("no bitmap supplied!");
        }

        final Activity self = this;

        // get rating!
        int w = image.getWidth(), h = image.getHeight();
        int[] data = new int[w * h];
        image.getPixels(data, 0, w, 0, 0, w, h);

        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);
        byte[] bdata = byteBuffer.array();

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md5 = new BigInteger(1, md.digest()).toString(16);
            float f = pref.getFloat(md5, 0);
            ratingBar.setRating(f);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BitmapSound bitmapSound = new BitmapSound();
        bitmapSound.setBitmap(image);
        bitmapSound.playImage(new Callback() {
            @Override
            public void call(final double d) {
                self.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress((int)(d * progressBar.getMax()));
                    }
                });
            }
        });

        ((ImageView) findViewById(R.id.imageView)).setImageBitmap(image);
    }
}
