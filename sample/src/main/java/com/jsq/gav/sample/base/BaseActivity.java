package com.jsq.gav.sample.base;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jsq.gav.sample.R;
import com.gyf.barlibrary.ImmersionBar;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by jiang on 2019/5/13
 */

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(View view) {
        LinearLayout rootView = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_base, null, false);
        rootView.addView(view);
        super.setContentView(rootView);
        setupStatusBar(rootView);
    }

    @Override
    public void setContentView(int layoutResID) {
        LinearLayout rootView = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_base, null, false);
        getLayoutInflater().inflate(layoutResID, rootView);
        super.setContentView(rootView);
        setupStatusBar(rootView);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        LinearLayout rootView = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_base, null, false);
        rootView.addView(view, params);
        super.setContentView(view);
        setupStatusBar(rootView);
    }

    protected void setupStatusBar(LinearLayout rootView) {
        View statusBar = findViewById(R.id.status_bar);
        if (statusBar == null) {
            ViewGroup contentView = findViewById(android.R.id.content);
            statusBar = getLayoutInflater().inflate(R.layout.status_bar, contentView, false);
            rootView.addView(statusBar, 0);
        }
        ImmersionBar.with(this)
                .statusBarView(statusBar)
                .statusBarDarkFont(getBackgroundColor() == 0xFFFFFFFF)
                .init();
    }

    @ColorInt
    private int getBackgroundColor() {
        TypedArray array = getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground,
        });
        int backgroundColor = array.getColor(0, 0xFFFFFFFF);
        array.recycle();
        return backgroundColor;
    }

    protected void addDisposable(Disposable disposable) {
        mCompositeDisposable.add(disposable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImmersionBar.with(this).destroy();
        mCompositeDisposable.clear();
    }
}
