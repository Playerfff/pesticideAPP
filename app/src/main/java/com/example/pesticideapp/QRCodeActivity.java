package com.example.pesticideapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QRCodeActivity extends AppCompatActivity {

    private TextView textView;
    private ObjectMapper objectMapper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        textView = findViewById(R.id.textView);
        objectMapper = new ObjectMapper();

        // 初始化 IntentIntegrator 对象
        IntentIntegrator integrator = new IntentIntegrator(this);
        // 设置扫描结果显示的格式
        integrator.setPrompt("Scan a QR Code");
        // 开始扫描二维码
        integrator.initiateScan();
    }

    // 处理扫描结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.d("MainActivity", "Cancelled");
                textView.setText("Cancelled");
            } else {
                Log.d("MainActivity", "Scanned: " + result.getContents());
                String jsonStr = result.getContents();
                // 解析 JSON 字符串
                JsonNode jsonNode = null;
                try {
                    jsonNode = objectMapper.readTree(jsonStr);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                // 获取特定字段的值
                String name = jsonNode.get("name").asText();
                String gender = jsonNode.get("gender").asText();
                String age = jsonNode.get("age").asText();

                textView.setText("Scanned: " + "\n" + "name:    " + name + "\n" + "gender:    " + gender + "\n" + "age:    " + age);
            }
        }
    }
}