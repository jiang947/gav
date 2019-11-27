package com.jsq.gav.sample.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.jsq.gav.sample.utils.Constants;
import com.jsq.gav.sample.utils.Glide4Engine;
import com.google.common.collect.ImmutableList;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiang on 2019/4/26
 */

public class SimpleListActivity extends BaseActivity {
    private static final int REQUEST_CODE_TEST = 2;
    private static final int REQUEST_CODE_EXO = 3;

    private static final int REQUEST_CODE_AUDIO = 955;


    private List<ItemInfo> list = ImmutableList.of(

//            new ItemInfo("CameraPreview", CameraPreviewActivity.class, info -> {
//                Intent intent = new Intent(this, info.clss);
//                if (info.bundle != null) {
//                    intent.putExtras(info.bundle);
//                }
//                startActivity(intent);
//            }),

            new ItemInfo("Video Player Test", info -> {
                addDisposable(new RxPermissions(this).request(Constants.PermissionGroup.ALBUM)
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                Matisse.from(this)
                                        .choose(MimeType.ofVideo())
                                        .countable(true)
                                        .capture(false)
                                        .maxSelectable(9)
                                        .showSingleMediaType(true)
                                        .restrictOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                        .thumbnailScale(0.85f)
                                        .imageEngine(new Glide4Engine())
                                        .theme(R.style.Matisse_Dracula)
                                        .originalEnable(true)
                                        .forResult(REQUEST_CODE_TEST);
                            }
                        }, Throwable::printStackTrace));
            }),

            new ItemInfo("exo test", info -> {
                addDisposable(new RxPermissions(this)
                        .request(Constants.PermissionGroup.ALBUM)
                        .subscribe(aBoolean -> {
                            if (aBoolean) {
                                Matisse.from(this)
                                        .choose(MimeType.ofVideo())
                                        .countable(true)
                                        .capture(false)
                                        .maxSelectable(9)
                                        .showSingleMediaType(true)
                                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                        .thumbnailScale(0.85f)
                                        .imageEngine(new Glide4Engine())
                                        .theme(R.style.Matisse_Dracula)
                                        .originalEnable(true)
                                        .forResult(REQUEST_CODE_EXO);
                            }
                        }, Throwable::printStackTrace));

            }),
            new ItemInfo("choose audio", info -> {
                AudioBrowserActivity.start(this);
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("audio/*");
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                startActivityForResult(intent, REQUEST_CODE_AUDIO);

            }),

            new ItemInfo("SampleAudioPlayerActivity", info -> {
                SampleAudioPlayerActivity.start(this);
            }),
//            new ItemInfo("LightGiftRenderTest", info -> {
//                LightGiftRenderActivity.start(this);
//            }),
//            new ItemInfo("LightGiftRenderGlSurfaceViewActivity", info -> {
//                LightGiftRenderGlSurfaceViewActivity.start(this);
//            }),
            new ItemInfo("Render3D-Obj-hat", info -> {
                RenderObj3dHatActivity.start(this);
            }),
            new ItemInfo("Render3D-pikachu", info -> {
                RenderObj3dPiKaChuActivity.start(this);
            })

    );


    public static void start(Context context) {
        Intent starter = new Intent(context, SimpleListActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new Adapter(list));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_TEST) {
                ArrayList<String> selected = (ArrayList<String>) Matisse.obtainPathResult(data);
                if (selected != null && !selected.isEmpty()) {
                    VideoPlayerTestActivity.start(this, selected);
                }
            }
            if (requestCode == REQUEST_CODE_EXO) {
                List<Uri> selected = Matisse.obtainResult(data);
                if (selected != null && !selected.isEmpty()) {
                    ExoPlayerTestActivity.start(this, (ArrayList<Uri>) selected);
                }
            }
            if (requestCode == REQUEST_CODE_AUDIO) {
                System.out.println(1111);
            }
        }
    }

    static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private List<ItemInfo> mDataList;

        Adapter(List<ItemInfo> dataList) {
            mDataList = dataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            Context context = viewGroup.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            ViewHolder holder = new ViewHolder(inflater.inflate(R.layout.item_text_view, viewGroup, false));
            holder.itemView.setOnClickListener(v -> {
                ItemInfo info = mDataList.get(holder.getAdapterPosition());
                if (info.clickDelegate != null) {
                    info.clickDelegate.onClick(info);
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            viewHolder.textView.setText(mDataList.get(i).title);
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_view);
        }
    }

    static class ItemInfo {
        String title;
        Class<?> clss;
        Bundle bundle;

        ClickDelegate clickDelegate;

        ItemInfo(String title, Class<?> clss) {
            this.title = title;
            this.clss = clss;
        }

        ItemInfo(String title, Class<?> clss, Bundle bundle) {
            this.title = title;
            this.clss = clss;
            this.bundle = bundle;
        }

        ItemInfo(String title, ClickDelegate clickDelegate) {
            this.title = title;
            this.clickDelegate = clickDelegate;
        }

        ItemInfo(String title, Class<?> clss, ClickDelegate clickDelegate) {
            this.title = title;
            this.clss = clss;
            this.clickDelegate = clickDelegate;
        }

        ItemInfo(String title, Class<?> clss, Bundle bundle, ClickDelegate clickDelegate) {
            this.title = title;
            this.clss = clss;
            this.bundle = bundle;
            this.clickDelegate = clickDelegate;
        }
    }


    interface ClickDelegate {
        void onClick(ItemInfo info);
    }


}
