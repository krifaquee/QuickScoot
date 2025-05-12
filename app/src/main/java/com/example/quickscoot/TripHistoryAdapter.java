package com.example.quickscoot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.ViewHolder> {
    private final List<Trip> trips;

    public TripHistoryAdapter(List<Trip> trips) {
        this.trips = trips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.scooterIdTextView.setText("Самокат: " + trip.getScooterId());
        holder.startTimeTextView.setText("Начало: " + formatTimestamp(trip.getStartTime()));
        holder.endTimeTextView.setText("Конец: " + formatTimestamp(trip.getEndTime()));
        holder.distanceTextView.setText("Расстояние: " + String.format(Locale.getDefault(), "%.2f км", trip.getDistance() / 1000));
    }


    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView scooterIdTextView, startTimeTextView, endTimeTextView, distanceTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            scooterIdTextView = itemView.findViewById(R.id.scooterIdTextView);
            startTimeTextView = itemView.findViewById(R.id.startTimeTextView);
            endTimeTextView = itemView.findViewById(R.id.endTimeTextView);
            distanceTextView = itemView.findViewById(R.id.distanceTextView);
        }
    }

}
