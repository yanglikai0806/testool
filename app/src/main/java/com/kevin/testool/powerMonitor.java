package com.kevin.testool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class powerMonitor extends AppCompatActivity {
    private EditText bf1;
    private EditText bf2;
    private EditText bf3;
    private EditText bf4;
    private Button button;
    private int bateryLevel;
    private int voltage;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
//    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US);
    private Date date = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_monitor);
        final Intent intent_PMservice = new Intent(this, PMservice.class);
//        intent_PMservice.setAction("com.kevin.testool.PMservice");
        startService(intent_PMservice);
        bf1=findViewById(R.id.bf1);
        bf2=findViewById(R.id.bf2);
        bf3=findViewById(R.id.bf3);
        bf4=findViewById(R.id.bf4);
        button = findViewById(R.id.updateButton);
        PMReceiver receiver = new PMReceiver();
        IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        filter.addAction("com.kevin.testool.PMservice");
        this.registerReceiver(receiver,filter);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bf2.setText(bateryLevel+"");
                bf3.setText(voltage+"");
                bf4.setText(dateFormat.format(new Date()));

            }
        });

    }
    //接受PMservice数据的广播接收器
    public class PMReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            bateryLevel=intent.getExtras().getInt("level");//获得当前电量
//            int total=intent.getExtras().getInt("scale");//获得总电量
            voltage=intent.getExtras().getInt("voltage");
//            Bundle bundle=intent.getExtras();
//            int A=bundle.getInt("a");
//            int B=bundle.getInt("b");
//            int C=bundle.getInt("c");
//            int D=bundle.getInt("d");
//            a.setText(A+"");
//            b.setText(B+"");
//            c.setText(C+"");
//            d.setText(D+"");
        }
    }
}
