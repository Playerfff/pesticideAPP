package com.example.pesticideapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class AnalysisActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private TextView textViewData;
    private ObjectMapper objectMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        objectMapper = new ObjectMapper();

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        textViewData = findViewById(R.id.textViewData);

        Intent intent = getIntent();
        if (intent != null) {
            // 提取传递的字符串数据
            String serverResponse = intent.getStringExtra("res");
            // 提取传递的 Bitmap 数据
            Uri uri = intent.getParcelableExtra("image");
//            Bitmap myImage = intent.getParcelableExtra("image");
            Bitmap myImage= null;
            try {
                myImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 解析 JSON 字符串
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(serverResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            // 获取特定字段的值
            String message = jsonNode.get("message").asText();

            if (message.equals("chip abnormality") || message.equals("server response failed")) {
                textView.setText(message);
                textView.setTextColor(Color.BLACK);
            } else {
                String result = jsonNode.get("result").asText();
                textViewData.setText(message);
                if (result.equals("Mild")) {
                    textView.setTextColor(getResources().getColor(R.color.purple));
                    textView.setText(result);
                } else if (result.equals("Moderate")) {
                    textView.setTextColor(Color.YELLOW);
                    textView.setText(result);
                } else if (result.equals("Severe")) {
                    textView.setTextColor(Color.RED);
                    textView.setText(result);
                } else if (result.equals("Unknown")) {
                    textView.setTextColor(getResources().getColor(R.color.gray));
                    textView.setText(result);
                }
            }

            // 在此处更新 UI
            if (myImage != null) {
                imageView.setImageBitmap(myImage);
            }
        }
    }


}