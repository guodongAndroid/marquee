package com.guodongandroid.marqueetextview.support;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.guodongandroid.marquee.support.HorizontalMarqueeTextView;
import com.guodongandroid.marquee.support.VerticalMarqueeTextView;

public class MainActivity extends AppCompatActivity {

    private HorizontalMarqueeTextView mHorizontalMarqueeTextViewS;
    private VerticalMarqueeTextView mVerticalMarqueeTextViewS;

    private Button mBtnMarquee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnMarquee = findViewById(R.id.btn_marquee);
        mHorizontalMarqueeTextViewS = findViewById(R.id.support_horizontal);
        mVerticalMarqueeTextViewS = findViewById(R.id.support_vertical);

        mBtnMarquee.setOnClickListener(v -> {
            mBtnMarquee.setSelected(!mBtnMarquee.isSelected());
            boolean selected = mBtnMarquee.isSelected();
            if (selected) {
                mBtnMarquee.setText("停止滚动");
            } else {
                mBtnMarquee.setText("开始滚动");
            }

            mHorizontalMarqueeTextViewS.setMarquee(selected);
            mVerticalMarqueeTextViewS.setMarquee(selected);
        });
    }
}