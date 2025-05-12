package com.example.quickscoot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;


import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;

public class ScooterAdapter extends ArrayAdapter<Scooter> {

    public ScooterAdapter(Context context, ArrayList<Scooter> scooters) {
        super(context, 0, scooters);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_scooter, parent, false);
        }

        Scooter scooter = getItem(position);

        TextView scooterIdTextView = convertView.findViewById(R.id.scooterIdTextView);
        TextView scooterBatteryTextView = convertView.findViewById(R.id.scooterBatteryTextView);
        TextView scooterStatusTextView = convertView.findViewById(R.id.scooterStatusTextView);
        TextView scooterDistanceTextView = convertView.findViewById(R.id.scooterDistanceTextView);

        if (scooter != null) {
            scooterIdTextView.setText("ID: " + scooter.getId());
            scooterBatteryTextView.setText("Заряд: " + scooter.getBattery() + "%");
            scooterDistanceTextView.setText("Расстояние: " + String.format("%.0f m", scooter.getDistance()));

            String status = scooter.getStatus();
            scooterStatusTextView.setText("Статус: " + status);
            switch (status.toLowerCase()) {
                case "занят":
                    scooterStatusTextView.setTextColor(Color.RED);
                    break;
                case "забронирован":
                    scooterStatusTextView.setTextColor(Color.YELLOW);
                    break;
                case "доступен":
                    scooterStatusTextView.setTextColor(Color.GREEN);
                    break;
                default:
                    scooterStatusTextView.setTextColor(Color.GRAY);
                    break;
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getContext() instanceof FragmentActivity) {
                        FragmentActivity activity = (FragmentActivity) getContext();
                        ScooterInfoBottomSheet bottomSheet = ScooterInfoBottomSheet.newInstance(
                                String.valueOf(scooter.getBattery()),
                                "ID: " + String.valueOf(scooter.getId()), // model
                                String.valueOf(scooter.getDistance()), // range
                                scooter.getId() // scooterId
                        );
                        bottomSheet.show(activity.getSupportFragmentManager(), "ScooterInfoBottomSheet");
                    } else {
                        Toast.makeText(getContext(), "Ошибка отображения информации о скутере", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        return convertView;
    }
}