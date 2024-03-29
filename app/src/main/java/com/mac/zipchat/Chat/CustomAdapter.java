package com.mac.zipchat.Chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


import com.mac.zipchat.R;

import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    ArrayList<MessageModel> list;
    public static final int MESSAGE_TYPE_IN = 1;
    public static final int MESSAGE_TYPE_OUT = 2;

    public CustomAdapter(Context context, ArrayList<MessageModel> list) { // you can pass other parameters in constructor
        this.context = context;
        this.list = list;
    }

    private class MessageInViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV,dateTV;
        MessageInViewHolder(final View itemView) {
            super(itemView);
//            messageTV = itemView.findViewById(R.id.message_text);
//            dateTV = itemView.findViewById(R.id.date_text);
            messageTV=itemView.findViewById(R.id.text_message);

        }
        void bind(int position) {
            MessageModel messageModel = list.get(position);
            messageTV.setText(messageModel.message);//            messageTV.setText(messageModel.message);
//            dateTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messageModel.messageTime));
        }
    }

    private class MessageOutViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV,dateTV;
        MessageOutViewHolder(final View itemView) {
            super(itemView);
//            messageTV = itemView.findViewById(R.id.message_text);
//            dateTV = itemView.findViewById(R.id.date_text);

            messageTV=itemView.findViewById(R.id.text_message);
        }
        void bind(int position) {
            MessageModel messageModel = list.get(position);
            messageTV.setText(messageModel.message);
//            dateTV.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(messageModel.messageTime));
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == MESSAGE_TYPE_IN) {
            return new MessageInViewHolder(LayoutInflater.from(context).inflate(R.layout.item_container_received, parent, false));
        }
        return new MessageOutViewHolder(LayoutInflater.from(context).inflate(R.layout.item_container_sent_message, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (list.get(position).messageType == MESSAGE_TYPE_IN) {
            ((MessageInViewHolder) holder).bind(position);
        } else {
            ((MessageOutViewHolder) holder).bind(position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).messageType;
    }
}