package com.lola.tomatodiseasedetection;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_AND_STORAGE = 100;
    public static int OPEN_IMAGE_REQUEST_CODE = 101;
    public static int REQUEST_IMAGE_CAPTURE = 102;
    private String currentPhotoPath;

    ImageView tbImage;
    Button analyseImage;
    TextView resultTxtView;

    ProgressDialog analyzeProgressDialog;

    TomatoDiseaseClassifier tomatoDiseaseClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tomatoDiseaseClassifier = new TomatoDiseaseClassifier(this);

        resultTxtView = findViewById(R.id.result);
        tbImage = findViewById(R.id.tb_image);
        tbImage.setOnClickListener((v)->getImage());
        tbImage.setOnLongClickListener((v) -> {
            checkCameraPermission();
            return true;
        });

        analyseImage = findViewById(R.id.btn_analyze);
        analyseImage.setOnClickListener((v -> analyzeImage()));
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CAMERA_AND_STORAGE);
    }

    private void checkCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (getIfUserDoNotWantPermissionToBeRequestedAnymore()){
                showSettingsAlert();
            // Should we show an explanation?
            }else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                    ||ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                showAlert();
            // No explanation needed, we can request the permission.
            }else{
                requestPermission();
            }
        }else{
            getCameraImage();
        }
    }

    private void showAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage("App needs to access the camera and store images")
                .setNegativeButton("Don't Allow", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    //finish();
                })
                .setPositiveButton("Allow", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    requestPermission();
                })
                .create()
                .show();
    }

    private void showSettingsAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage("App needs to access the camera and store images")
                .setNegativeButton("Don't Allow", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    //finish();
                })
                .setPositiveButton("Allow", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    startInstalledAppDetailsActivity(this);
                })
                .create()
                .show();
    }

    public static void startInstalledAppDetailsActivity(final Context context) {
        if (context == null) {
            return;
        }

        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    private boolean getIfUserDoNotWantPermissionToBeRequestedAnymore() {
        return getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
                .getBoolean(getString(R.string.user_dont_want_permission_to_be_requested), false);
    }

    private void setIfUserDoNotWantPermissionToBeRequestedAnymore() {
        getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
                .edit()
                .putBoolean(getString(R.string.user_dont_want_permission_to_be_requested), true)
                .apply();
    }

    private void getCameraImage() {
        if (currentPhotoPath != null){
            new File(currentPhotoPath).delete();
        }
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }catch (IOException e){
                if (e.getMessage() != null) {Log.e(TAG, e.getMessage());}
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.lola.tomatodiseasedetection.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException{
        // Create an image file name
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                "image",  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void getImage(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,OPEN_IMAGE_REQUEST_CODE);
    }

    private void showProgressDialog(){
        analyzeProgressDialog = new ProgressDialog(this);
        analyzeProgressDialog.setMessage("Checking image");
        analyzeProgressDialog.setCancelable(false);
        analyzeProgressDialog.show();
    }

    private void hideProgressDialog(){
        analyzeProgressDialog.cancel();
    }

    private void analyzeImage() {
        Bitmap bitmap = ((BitmapDrawable) tbImage.getDrawable()).getBitmap();

        // check if image is the same as placeholder image
        if (bitmap.getWidth() == 403 && bitmap.getHeight() == 450){
            Toast.makeText(this,"Please select an image",Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog();
        new ClassifierAsyncTask().execute(bitmap);
    }

    private void displayResult(TomatoDiseaseResults tomatoDiseaseResults){
        resultTxtView.setText(
                reformatResultAndMakeItMoreHuman(tomatoDiseaseResults.name())
        );
    }

    private String reformatResultAndMakeItMoreHuman(String result){
        return result.replace("_", " ");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode==RESULT_OK){
            if (requestCode==OPEN_IMAGE_REQUEST_CODE){
                if(data!=null){
                    Glide.with(MainActivity.this).load(data.getData()).override(400,400).into(tbImage);
                }
            }
            if (requestCode==REQUEST_IMAGE_CAPTURE){
                Glide.with(MainActivity.this).load(currentPhotoPath).into(tbImage);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();

        getSharedPreferences(LoginActivity.TB_PREF,MODE_PRIVATE).edit()
                .putBoolean(LoginActivity.LOGIN_STATUS,false).apply();

        return super.onOptionsItemSelected(item);
    }

    class ClassifierAsyncTask extends AsyncTask<Bitmap, Void, TomatoDiseaseResults>{

        @Override
        protected TomatoDiseaseResults doInBackground(Bitmap... bitmaps) {

            TomatoDiseaseResults tomatoDiseaseResults;

            tomatoDiseaseResults = tomatoDiseaseClassifier.getResult(bitmaps[0]);
            return tomatoDiseaseResults;
        }

        @Override
        protected void onPostExecute(TomatoDiseaseResults tomatoDiseaseResults) {
            super.onPostExecute(tomatoDiseaseResults);
            displayResult(tomatoDiseaseResults);
            hideProgressDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_AND_STORAGE){
            if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    showAlert();
                }else{
                    // user denied flagging NEVER ASK AGAIN
                    // you can either enable some fall back,
                    // disable features of your app
                    // or open another dialog explaining
                    // again the permission and directing to
                    // the app setting
                    setIfUserDoNotWantPermissionToBeRequestedAnymore();
                }
            }else{
                getCameraImage();
            }
        }
    }
}
