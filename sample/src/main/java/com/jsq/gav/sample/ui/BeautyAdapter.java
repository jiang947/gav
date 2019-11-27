package com.jsq.gav.sample.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jsq.gav.sample.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiang on 2019/4/25
 */

public class BeautyAdapter extends RecyclerView.Adapter<BeautyAdapter.ViewHolder> {

    private List<String> mDataList = new ArrayList<>();
    private final OnItemClickListener mItemClickListener;

    public BeautyAdapter(OnItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        ViewHolder holder = new ViewHolder(inflater.inflate(R.layout.item_lookup, viewGroup, false));
        holder.itemView.setOnClickListener(v ->
                mItemClickListener.onItemClick(holder.getAdapterPosition(), v, getItem(holder.getAdapterPosition())));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.textView.setText(getItem(i));
    }

    public String getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public void replace(List<String> data) {
        if (mDataList != data) {
            mDataList = data;
            notifyDataSetChanged();
        }
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_view);
        }

    }


    public interface OnItemClickListener {
        void onItemClick(int position, View view, String item);
    }

}
