package com.qrcode.refilling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    ArrayList<ArrayList<String>> list;

    public ReportAdapter(ArrayList<ArrayList<String>> list) {
        this.list = list;
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        LinearLayout containerView;

        public ReportViewHolder(View itemView) {
            super(itemView);
            containerView = itemView.findViewById(R.id.ll);
        }
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_report, parent, false);
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ReportViewHolder holder, int position) {
        ArrayList<String> localList = list.get(position);

        for(int i = 0; i < holder.containerView.getChildCount(); i++){
            ((TextView)(holder.containerView.getChildAt(i))).setText("");
            ((TextView)(holder.containerView.getChildAt(i))).setText(localList.get(i));
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
