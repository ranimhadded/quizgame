package com.example.quizgame.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizgame.HostPackActivity;
import com.example.quizgame.R;
import com.example.quizgame.model.TriviaCategory;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<TriviaCategory> items = new ArrayList<>();
    private final Context context;
    private String mode;
    public CategoryAdapter(Context ctx, String mode) {
        this.context = ctx;
        this.mode = mode;
    }
    public void setItems(List<TriviaCategory> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.sample_card_topic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TriviaCategory cat = items.get(position);
        holder.topicName.setText(cat.name);

        // Optional: set image if you have a mapping
        holder.topicImage.setImageResource(cat.imageResId); // see note below

        // --------- CLICK LISTENER ---------
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HostPackActivity.class);
            intent.putExtra("topicId", cat.id);
            intent.putExtra("topicName", cat.name);
            intent.putExtra("topicImage", cat.imageResId); // pass image resource ID
            intent.putExtra("mode", mode);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView topicName;
        ImageView topicImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            topicName = itemView.findViewById(R.id.topicName);
            topicImage = itemView.findViewById(R.id.topicImage);
        }
    }
}