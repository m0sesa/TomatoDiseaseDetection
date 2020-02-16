package com.lola.tomatodiseasedetection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static int OPEN_IMAGE_REQUEST_CODE = 101;

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

        analyseImage = findViewById(R.id.btn_analyze);
        analyseImage.setOnClickListener((v -> {
            analyzeImage();
        }));
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
