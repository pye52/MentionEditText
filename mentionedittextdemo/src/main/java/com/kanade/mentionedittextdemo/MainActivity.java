package com.kanade.mentionedittextdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kanade.mentionedittext.MentionEditText;

public class MainActivity extends AppCompatActivity {

    private MentionEditText met;
    private Button add;
    private Button print;
    private TextView textView;
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        met = (MentionEditText) findViewById(R.id.met);
        add = (Button) findViewById(R.id.add);
        print = (Button) findViewById(R.id.print);
        textView = (TextView) findViewById(R.id.tv);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                met.addMentionText(i, "测试" + i, false);
                i++;
            }
        });

        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = met.printMentionString("[提醒:%s, %s]");
                textView.setText(text);
            }
        });
    }
}
