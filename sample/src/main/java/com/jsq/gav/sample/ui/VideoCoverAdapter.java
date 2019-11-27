package com.jsq.gav.sample.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.jsq.gav.MediaSourceInfo;
import com.jsq.gav.sample.R;

import java.util.List;

/**
 * Created by jiang on 2019/5/30
 */

public class VideoCoverAdapter extends RecyclerView.Adapter<VideoCoverAdapter.ViewHolder> {

    private AsyncListDiffer<MediaSourceInfo> mDiffer;
    private OnItemClickListener mOnItemClickListener;

    private int selected = -1;

    private DiffUtil.ItemCallback<MediaSourceInfo> diffCallback = new DiffUtil.ItemCallback<MediaSourceInfo>() {
        @Override
        public boolean areItemsTheSame(MediaSourceInfo oldItem, MediaSourceInfo newItem) {
            return oldItem.source.equals(newItem.source);
        }

        @Override
        public boolean areContentsTheSame(MediaSourceInfo oldItem, MediaSourceInfo newItem) {
            return oldItem.source.equals(newItem.source);
        }
    };
    private LayoutInflater mInflater;

    public VideoCoverAdapter() {
        mDiffer = new AsyncListDiffer<>(this, diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = getLayoutInflater(viewGroup.getContext())
                .inflate(R.layout.item_video_image, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(v, viewHolder.getAdapterPosition());
            }
        });
        return viewHolder;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    private LayoutInflater getLayoutInflater(Context context) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(context);
        }
        return mInflater;
    }

    public MediaSourceInfo getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Glide.with(viewHolder.itemView.getContext())
                .load(getItem(i).videoCover)
                .into(viewHolder.imageView);
    }

    @NonNull
    public List<MediaSourceInfo> getCurrentList() {
        return mDiffer.getCurrentList();
    }

    public void submitList(@Nullable List<MediaSourceInfo> newList) {
        mDiffer.submitList(newList);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public int getSelected() {
        return selected;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }

    public interface OnItemClickListener {

        void onItemClick(View view, int position);

    }


}
