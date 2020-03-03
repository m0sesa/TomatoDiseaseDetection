package com.lola.tomatodiseasedetection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

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
            getCameraImage();
            return true;
        });

        analyseImage = findViewById(R.id.btn_analyze);
        analyseImage.setOnClickListener((v -> {
            analyzeImage();
        }));
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
        Random random = new Random();
        Bitmap bitmap = ((BitmapDrawable) tbImage.getDrawable()).getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minDim = (width>height)?height:width;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.logout:
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();

                getSharedPreferences(LoginActivity.TB_PREF,MODE_PRIVATE).edit()
                        .putBoolean(LoginActivity.LOGIN_STATUS,false).apply();
        }
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

}
