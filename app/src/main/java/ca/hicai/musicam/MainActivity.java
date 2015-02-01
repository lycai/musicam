package ca.hicai.musicam;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";

    public static final String EXTRA_BITMAP = "ca.hicai.musicam.EXTRA.BITMAP";

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_FROM_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout cameraPane = (LinearLayout) findViewById(R.id.cameraPane);
        LinearLayout galleryPane = (LinearLayout) findViewById(R.id.galleryPane);

        /*galleryPane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked galleryPane");
                Intent intent = new Intent(getApplicationContext(), SoundGeneratorTestActivity.class);
                startActivity(intent);
            }
        });*/
    }

    private String imagePath;
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "musicam_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagePath = image.getAbsolutePath();
        Log.d(TAG, "created temp file: " + imagePath);
        return image;
    }
    private void addImageToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(new File(imagePath)));
        this.sendBroadcast(mediaScanIntent);
    }

    public void cameraClick(View v) {
        Log.d(TAG, "Clicked cameraPane");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "Your camera is disabled!", Toast.LENGTH_LONG).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app is available!", Toast.LENGTH_LONG).show();
            return;
        }

        File imageFile = null;
        try {
            imageFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (imageFile == null) {
            Toast.makeText(this, "Unable to allocate space for image!", Toast.LENGTH_LONG).show();
            return;
        }

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    public void galleryClick(View v) {
        //Intent selectImageIntent = new Intent();
        //selectImageIntent.setType("image/*");
        //selectImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        //startActivityForResult(Intent.createChooser(selectImageIntent, "Select Picture"), REQUEST_IMAGE_FROM_GALLERY);
        startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            REQUEST_IMAGE_FROM_GALLERY);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String filePath = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                addImageToGallery();
                filePath = imagePath;
            } else if (requestCode == REQUEST_IMAGE_FROM_GALLERY) {
                Uri uri = data.getData();
                Log.d(TAG, "URI: " + uri);

                if (uri == null) {
                    Toast.makeText(this, "An unknown error occurred.", Toast.LENGTH_LONG);
                    return;
                }

                Cursor cursor = getContentResolver().query(uri, new String[] { MediaStore.Images.Media.DATA },
                        null, null, null);
                Log.d(TAG, "count: " + cursor.getCount());
                cursor.moveToFirst();
                Log.d(TAG, "columns: " + cursor.getColumnNames());
                Log.d(TAG, "type: " + cursor.getType(0));
                filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                cursor.close();
            }

            Log.d(TAG, "filePath: " + filePath);

            if (filePath == null) {
                Toast.makeText(this, "An unknown error occurred.", Toast.LENGTH_LONG);
            }

            Intent soundPlayerIntent = new Intent(this, SoundPlayerActivity.class);
            soundPlayerIntent.putExtra(EXTRA_BITMAP, filePath);
            startActivity(soundPlayerIntent);
        }
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
    */
}
