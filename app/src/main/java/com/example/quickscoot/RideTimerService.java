package com.example.quickscoot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class RideTimerService extends Service {
    public static final String CHANNEL_ID = "RideTimerChannel";
    private CountDownTimer timer;
    private FirebaseFirestore db;
    private String scooterId;
    private double totalDistance = 0.0;
    private static final double COST_PER_MINUTE = 0.25;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            long rideDurationMillis = intent.getLongExtra("rideDurationMillis", 0);
            scooterId = intent.getStringExtra("scooterId");
            totalDistance = intent.getDoubleExtra("totalDistance", 0.0);
            startRideTimer(rideDurationMillis);
            startForeground(1, buildNotification("00:00"));
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    private Notification buildNotification(String timeLeft) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ride Timer")
                .setContentText("Время поездки: " + timeLeft)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSmallIcon(R.drawable.userpng)  // Замените на свой иконку
                .build();
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ride Timer Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void startRideTimer(long rideDurationMillis) {
        timer = new CountDownTimer(rideDurationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String timeLeft = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                updateNotification(timeLeft);
            }

            @Override
            public void onFinish() {
                endRide();
                stopSelf();
            }
        }.start();
    }

    private void updateNotification(String timeLeft) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Поездка в процессе")
                .setContentText("Оставшееся время: " + timeLeft)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher);

        // Обновление уведомления
        startForeground(1, notificationBuilder.build());
    }

    private void endRide() {
        if (scooterId != null && !scooterId.isEmpty()) {
            db.collection("scooters").document(scooterId)
                    .update("status", "доступен")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getApplicationContext(), "Поездка завершена", Toast.LENGTH_SHORT).show();

                        // Обновляем время завершения поездки и сохраняем расстояние
                        String userId = getCurrentUserId();
                        db.collection("rides")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("scooterId", scooterId)
                                .whereEqualTo("endTime", 0) // Ищем незавершённую поездку
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        String tripId = querySnapshot.getDocuments().get(0).getId();
                                        db.collection("rides").document(tripId)
                                                .update("endTime", System.currentTimeMillis(), "distance", totalDistance)
                                                .addOnSuccessListener(aVoid1 -> Log.d("RideTimerService", "Время завершения и расстояние обновлены"))
                                                .addOnFailureListener(e -> Log.e("RideTimerService", "Ошибка при обновлении данных поездки", e));
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("RideTimerService", "Ошибка при поиске незавершённой поездки", e));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getApplicationContext(), "Ошибка при завершении поездки", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("RideTimerService", "Scooter ID is null or empty when ending ride");
        }
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        } else {
            Log.e("RideTimerService", "Пользователь не авторизован");
            return null;
        }
    }
}