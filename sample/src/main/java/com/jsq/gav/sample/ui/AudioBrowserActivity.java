package com.jsq.gav.sample.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jiang on 2019/5/13
 */

public class AudioBrowserActivity extends BaseActivity {


    public static void start(Context context) {
        Intent starter = new Intent(context, AudioBrowserActivity.class);
        context.startActivity(starter);
    }

    private RecyclerView mRecyclerView;

    private InnerAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_browser);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new InnerAdapter();
        mRecyclerView.setAdapter(mAdapter);

    }


    @Override
    protected void onResume() {
        super.onResume();
        RxPermissions permissions = new RxPermissions(this);
        addDisposable(permissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        addDisposable(audioProvider()
                                .toList()
                                .toObservable()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(entries -> {
                                    mAdapter.data = entries;
                                    mAdapter.notifyDataSetChanged();
                                }, Throwable::printStackTrace));
                    } else {
                        Toast.makeText(this, "需要权限!", Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private Observable<Entry> audioProvider() {
        return Observable.create(emitter -> {
            try (Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null)) {
                while (cursor.moveToNext()) {
                    Entry entry = new Entry();
                    entry.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    entry.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    entry.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
                    entry.duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    emitter.onNext(entry);
                }
            }
            emitter.onComplete();
        });
    }

    static class InnerAdapter extends RecyclerView.Adapter<ViewHolder> {

        List<Entry> data = new ArrayList<>();


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View view = inflater.inflate(R.layout.item_audio_browser, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            viewHolder.titleView.setText(String.format("名称:%s", data.get(i).title));
            viewHolder.durationView.setText(String.format("时长:%s", data.get(i).duration));
            viewHolder.mimeTypeView.setText(String.format("MIME TYPE:%s", data.get(i).mimeType));
            viewHolder.pathView.setText(String.format("Path:%s", data.get(i).path));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView titleView;
        TextView durationView;
        TextView mimeTypeView;
        TextView pathView;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            durationView = itemView.findViewById(R.id.duration);
            mimeTypeView = itemView.findViewById(R.id.mime_type);
            pathView = itemView.findViewById(R.id.path);
        }
    }

    static class Entry {
        public String title = "";
        public String path = "";
        public String cutPath = "";
        public long duration;
        public long height;
        public long width;
        public long size;
        public String diaplayName;
        public String thumPath;
        public float start;
        public float end;
        public float ratio;

        public String mimeType;

        @Override
        public String toString() {
            return "Entry{" +
                    "title='" + title + '\'' +
                    ", path='" + path + '\'' +
                    ", cutPath='" + cutPath + '\'' +
                    ", duration=" + duration +
                    ", height=" + height +
                    ", width=" + width +
                    ", size=" + size +
                    ", diaplayName='" + diaplayName + '\'' +
                    ", thumPath='" + thumPath + '\'' +
                    ", start=" + start +
                    ", end=" + end +
                    ", ratio=" + ratio +
                    '}';
        }
    }


}
