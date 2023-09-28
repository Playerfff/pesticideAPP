package com.example.pesticideapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    /**
     * 外部存储权限请求码
     */
    public static final int REQUEST_EXTERNAL_STORAGE_CODE = 9527;
    /**
     * 图片剪裁请求码
     */
    public static final int PICTURE_CROPPING_CODE = 200;

    private static final int REQUEST_IMAGE_PICK = 2;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String SERVER_URL = "http://rlijc5ubeejz.ngrok.xiaomiqiu123.top/upload";
    private OkHttpClient client;
    private String mCurrentPhotoPath;
    private Bitmap myimage;
    private Uri selectUri;
    private String res;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnTakePhoto = findViewById(R.id.buttonTakePhoto);
        Button btnFile = findViewById(R.id.buttonFile);
        Button btnScanQRCode = findViewById(R.id.buttonScanQRCode);

        client = new OkHttpClient();

        requestPermission();

        myimage = null;
        res = null;

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        btnFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        btnScanQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, QRCodeActivity.class);
                startActivity(intent);
            }
        });
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_IMAGE_PICK);
    }

    @AfterPermissionGranted(REQUEST_EXTERNAL_STORAGE_CODE)
    private void requestPermission() {
        String[] param = {android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, param)) {
            //已有权限
            showMsg("已获得权限");

        } else {
            //无权限 则进行权限请求
            EasyPermissions.requestPermissions(this, "请求权限", REQUEST_EXTERNAL_STORAGE_CODE, param);
        }
    }

    /**
     * Toast提示
     *
     * @param msg 内容
     */
    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Permission.checkPermission(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Permission.isPermissionGranted(this)) {
            Log.i("PERMISSION", "请求权限成功");
        }
    }


    //拍摄照片,并将拍摄后的照片保存到指定的目录文件中
    private void openCamera() {
        //点击拍照
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = fileCreate();
        Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                "com.example.alertdialog.fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    //创建照片所保存的文件目录和文件名
    private File fileCreate() {
        //定义解析日期的格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        //获取现在的日期
        String format = sdf.format(new Date());

        //创建文件目录和具体的文件名和后缀
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), format + ".png");
        mCurrentPhotoPath = file.getAbsolutePath();
        //初始化imageUri
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            imageUri = FileProvider.getUriForFile(this, "com.example.alertdialog.fileprovider", file);
        } else {
            imageUri = Uri.fromFile(file);
        }


        return file;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Permission.REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permission", "授权失败！");
                    // 授权失败，退出应用
                    this.finish();
                    return;
                }
            }
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE_CODE) {
            // 将结果转发给 EasyPermissions
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            //因为相机应用会将图片保存到自己定义的文件目录中,所以返回的这个data为空
//            if (data == null) {
//                //BitmapFactory.decodeFile() 方法将文件路径解码为位图
//                Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
//                imageView.setImageBitmap(bitmap);
//                sendImageToServer(bitmap);
//            }

            // 获取图片文件
            File photoFile = new File(mCurrentPhotoPath);

            // 创建FileProvider的content Uri
            final Uri imageUri = FileProvider.getUriForFile(this, "com.example.alertdialog.fileprovider", photoFile);
            //图片剪裁
            pictureCropping(imageUri);
        } else if (requestCode == PICTURE_CROPPING_CODE && data != null) {
            //图片剪裁返回
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                //在这里获得了剪裁后的Bitmap对象，可以用于上传
                myimage = bundle.getParcelable("data");

                sendImageToServerAndToNextActivity(myimage);
            }

        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            // 获取选择的图片的URI
            Uri selectedImageUri = data.getData();
//            pictureCropping(selectedImageUri);

//            selectUri = selectedImageUri;
            fileCreate();
            pictureCropping2(selectedImageUri);


//            try {
//                // 将URI转换为Bitmap
//                myimage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
//                sendImageToServerAndToNextActivity(myimage);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }


    private void sendImageAndResToNextActivity() {
//        Intent intent = new Intent();
//        intent.setClass(MainActivity.this, AnalysisActivity.class);
//        startActivity(intent);
        Intent intent = new Intent(MainActivity.this, AnalysisActivity.class);
        intent.putExtra("res", res);
        intent.putExtra("image", imageUri);
        startActivity(intent);
    }

    /**
     * 图片剪裁为正方形
     *
     * @param uri 图片uri
     */
    private void pictureCropping(Uri uri) {
        // 调用系统中自带的图片剪裁
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= 24) {
            intent.setDataAndType(uri, "image/*");
            FileProviderUtils.grantUriPermission(this, intent, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(uri, "image/*");
        }

        intent.putExtra("crop", "true");
        // 裁剪框的比例，1：1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        // 图片格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
        // 取消人脸识别
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", true);

        if (Build.VERSION.SDK_INT >= 24) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            FileProviderUtils.grantUriPermission(this, intent, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            //设置裁剪后文件保存路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }
        startActivityForResult(intent, PICTURE_CROPPING_CODE);

    }

    private void pictureCropping2(Uri uri) {
        // 调用系统中自带的图片剪裁
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= 24) {
            intent.setDataAndType(uri, "image/*");
            FileProviderUtils.grantUriPermission(this, intent, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(uri, "image/*");
        }

        intent.putExtra("crop", "true");
        // 裁剪框的比例，1：1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        // 图片格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
        // 取消人脸识别
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", true);

        if (Build.VERSION.SDK_INT >= 24) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            FileProviderUtils.grantUriPermission(this, intent, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            //设置裁剪后文件保存路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }
        startActivityForResult(intent, PICTURE_CROPPING_CODE);

    }

    public void sendImageToServerAndToNextActivity(Bitmap imageBitmap) {
        // 创建一个字节数组输出流，将 Bitmap 图像压缩为 JPEG 格式并写入字节数组流中
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // 创建一个 Multipart 请求体，用于传递图片数据
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.jpg", RequestBody.create(MediaType.parse("image/jpeg"), byteArray))
                .build();

        // 创建一个 POST 请求，指定目标服务器的 URL 和请求体
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        // 使用 OkHttpClient 发起异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                // 请求失败时调用此方法，打印异常信息
                e.printStackTrace();

                // 在请求失败时执行其他操作，例如更新 UI 或显示错误消息
                runOnUiThread(() -> {
                    res = "server response failed";
                    sendImageAndResToNextActivity(); // 在这里调用
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                // 服务器响应成功时调用此方法
                // 获取服务器返回的字符串响应体
                String serverResponse = response.body().string();
                System.out.println("serverResponse" + serverResponse);

                runOnUiThread(() -> {
                    res = serverResponse;
                    sendImageAndResToNextActivity(); // 在这里调用
                });
            }
        });
    }
}