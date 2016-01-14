package com.example.zhaimeng.imooc_guaguaka;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.zhaimeng.imooc_guaguaka.com.example.zhaimeng.imooc_guaguaka.view.GuaGuaKa;

public class MainActivity extends AppCompatActivity {

    private GuaGuaKa mGuaGuaKa;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGuaGuaKa = (GuaGuaKa) findViewById(R.id.id_guaguaka);
        mGuaGuaKa.setOnGuaGuaKaCompleteListener(new GuaGuaKa.OnGuaGuaKaCompleteListener() {
            @Override
            public void onComplete() {
                Toast.makeText(MainActivity.this, "挂到60%了！", Toast.LENGTH_SHORT).show();
            }
        });
        mGuaGuaKa.setText("挂挂卡效果！");
    }
}
