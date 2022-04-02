package com.example.project;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class MainActivity extends AppCompatActivity {


    String selectedImagePath;
    String fileName="androidFlaskk.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 2);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        setContentView(R.layout.activity_main);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Granted. Thanks.", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getApplicationContext(), "Access to Internet Permission Granted. Thanks.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Access to Internet Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }



    public byte[] pathToBase64(String selectedImagePath, Context context) throws IOException  {

        String base64Photo = "";
        Bitmap bitmap = null;
        String filePath = "file://"+ selectedImagePath;
        Uri myUri = Uri.parse(filePath);

        bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), myUri);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte [] imgBytes = byteArrayOutputStream.toByteArray();


        return imgBytes;
    }
    //String selectedImagePath;


    public void connectServer(View v) throws IOException {
        byte[] base = pathToBase64(selectedImagePath, MyApplication.context);

        TextView responseText = findViewById(R.id.responseText);
        String postUrl= "http://54b941024dde.ngrok.io/photo";
        URL url = new URL(postUrl);



        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", fileName, RequestBody.create(base, MediaType.parse("multipart/form-data")))
                .build();


        responseText.setText("Please wait ...");

        postRequest(url, postBodyImage);
    }

    void postRequest(URL url, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();

        Request request = new Request.Builder()
                .url(url)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();
                Log.d("FAIL", e.getMessage());
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        try {
                            responseText.setText("Failed to Connect to Server");
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            TextView responseText = findViewById(R.id.responseText);
                            responseText.setText(response.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
    public void selectImage(View v) {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        try {
            if(resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();

                selectedImagePath = getPath(getApplicationContext(), uri);
                EditText imgPath = findViewById(R.id.imgPath);
                imgPath.setText(selectedImagePath);
                Toast.makeText(getApplicationContext(), selectedImagePath, Toast.LENGTH_LONG).show();
            }
        }
        catch (Exception e) {
            Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }




     // Implementation of the getPath() method and all its requirements is taken from the StackOverflow Paul Burke's answer: https://stackoverflow.com/a/20559175/5426539
     public static String getPath(final Context context, final Uri uri) {

         final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

         // DocumentProvider
         if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
             // ExternalStorageProvider
             if (isExternalStorageDocument(uri)) {
                 final String docId = DocumentsContract.getDocumentId(uri);
                 final String[] split = docId.split(":");
                 final String type = split[0];

                 if ("primary".equalsIgnoreCase(type)) {
                     return Environment.getExternalStorageDirectory() + "/" + split[1];
                 }

                 // TODO handle non-primary volumes
             }
             // DownloadsProvider
             else if (isDownloadsDocument(uri)) {

                 final String id = DocumentsContract.getDocumentId(uri);
                 final Uri contentUri = ContentUris.withAppendedId(
                         Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                 return getDataColumn(context, contentUri, null, null);
             }
             // MediaProvider
             else if (isMediaDocument(uri)) {
                 final String docId = DocumentsContract.getDocumentId(uri);
                 final String[] split = docId.split(":");
                 final String type = split[0];

                 Uri contentUri = null;
                 if ("image".equals(type)) {
                     contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                 } else if ("video".equals(type)) {
                     contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                 } else if ("audio".equals(type)) {
                     contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                 }

                 final String selection = "_id=?";
                 final String[] selectionArgs = new String[] {
                         split[1]
                 };

                 return getDataColumn(context, contentUri, selection, selectionArgs);
             }
         }
         // MediaStore (and general)
         else if ("content".equalsIgnoreCase(uri.getScheme())) {
             return getDataColumn(context, uri, null, null);
         }
         // File
         else if ("file".equalsIgnoreCase(uri.getScheme())) {
             return uri.getPath();
         }

         return null;
     }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}

