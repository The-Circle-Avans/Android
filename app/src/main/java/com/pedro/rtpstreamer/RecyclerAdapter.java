package com.pedro.rtpstreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pedro.rtpstreamer.domain.ChatMessage;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private ArrayList<ChatMessage> mData;
    private LayoutInflater mInflater;

    public RecyclerAdapter(Context context, ArrayList<ChatMessage> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        ChatMessage chat = mData.get(position);
        StringBuilder build = new StringBuilder();

        if (chat.getUserName().length() != 0) {
            build.append(chat.getUserName());
            build.append(": ");
            build.append(chat.getChatMessage());
        } else {
            build.append(chat.getChatMessage());
        }

        holder.textView.setText(build);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.chat_message);
        }

    }
}
