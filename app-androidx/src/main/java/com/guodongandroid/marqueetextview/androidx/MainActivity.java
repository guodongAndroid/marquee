package com.guodongandroid.marqueetextview.androidx;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.guodongandroid.marquee.androidx.HorizontalMarqueeTextView;
import com.guodongandroid.marquee.androidx.VerticalMarqueeTextView;

public class MainActivity extends AppCompatActivity {

    private HorizontalMarqueeTextView mHorizontalMarqueeTextViewX;
    private VerticalMarqueeTextView mVerticalMarqueeTextViewX;

    private Button mBtnMarquee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnMarquee = findViewById(R.id.btn_marquee);
        mHorizontalMarqueeTextViewX = findViewById(R.id.androidx_horizontal);
        mVerticalMarqueeTextViewX = findViewById(R.id.androidx_vertical);

        mBtnMarquee.setOnClickListener(v -> {
            mBtnMarquee.setSelected(!mBtnMarquee.isSelected());
            boolean selected = mBtnMarquee.isSelected();
            if (selected) {
                mBtnMarquee.setText("停止滚动");
            } else {
                mBtnMarquee.setText("开始滚动");
            }

            mHorizontalMarqueeTextViewX.setMarquee(selected);
            mVerticalMarqueeTextViewX.setMarquee(selected);
        });
    }
}