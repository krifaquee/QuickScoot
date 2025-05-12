package com.example.quickscoot;

import android.app.AlertDialog;
import android.Manifest;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ScooterInfoBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "ScooterInfoBottomSheet";
    private Button btnStartRide, btnBookRide;
    private TextView batteryTextView, rangeTextView;
    private String battery, model, range, scooterId;
    private Location previousLocation;
    private double totalDistance = 0.0;
    private FirebaseFirestore db;
    private BroadcastReceiver rideEndReceiver;
    private FusedLocationProviderClient fusedLocationClient;
    private static final double MAX_DISTANCE_METERS = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private LocationManager locationManager; private LocationListener locationListener;


    public static ScooterInfoBottomSheet newInstance(String battery, String model, String range, String scooterId) {
        ScooterInfoBottomSheet fragment = new ScooterInfoBottomSheet();
        Bundle args = new Bundle();
        args.putString("battery", battery);
        args.putString("model", model);
        args.putString("range", range);
        args.putString("scooterId", scooterId); // Передача scooterId
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_scooter_info, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        db = FirebaseFirestore.getInstance();

        assert getArguments() != null;
        battery = getArguments().getString("battery", "N/A" + "%");
        model = getArguments().getString("model", "N/A");
        range = getArguments().getString("range", "N/A");
        scooterId = getArguments().getString("scooterId", "");

        batteryTextView = view.findViewById(R.id.scooterBattery);
        rangeTextView = view.findViewById(R.id.scooterRange);
        btnStartRide = view.findViewById(R.id.btnStartRide);
        btnBookRide = view.findViewById(R.id.btnBookRide);

        batteryTextView.setText(battery);
        rangeTextView.setText(range);

        // Устанавливаем единственный OnClickListener для проверки статуса перед действиями
        btnStartRide.setOnClickListener(v -> checkScooterStatusAndProceed("start"));
        btnBookRide.setOnClickListener(v -> {
            if (isCardSaved()) {
                openBookingTimePicker();
            } else {
                navigateToPaymentActivity();
            }
        });


        rideEndReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                endRide();
            }
        };
        getContext().registerReceiver(rideEndReceiver, new IntentFilter("com.example.quickscoot.RIDE_END"));

        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (previousLocation != null) {
                    totalDistance += previousLocation.distanceTo(location);
                }
                previousLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
        }

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (rideEndReceiver != null) {
            getContext().unregisterReceiver(rideEndReceiver);
        }
    }

    // Проверка наличия сохраненной карты
    private boolean isCardSaved() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        String cardNumber = sharedPreferences.getString("cardNumber", null);
        return cardNumber != null && cardNumber.length() == 19; // Проверка на наличие и формат карты
    }

    // Переход к активности ввода карты
    private void navigateToPaymentActivity() {
        Intent intent = new Intent(getContext(), PaymentActivity.class);
        startActivity(intent);
        Toast.makeText(getContext(), "Пожалуйста, добавьте карту", Toast.LENGTH_SHORT).show();
    }

    private void bookScooter() {
        if (!scooterId.isEmpty()) {
            db.collection("scooters").document(scooterId)
                    .update("status", "забронирован")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Самокат забронирован", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка при бронировании", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e(TAG, "Scooter ID is null or empty when booking");
        }
    }

    private void checkScooterStatusAndProceed(String action) {
        if (!scooterId.isEmpty()) {
            db.collection("scooters").document(scooterId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String status = documentSnapshot.getString("status");
                        double scooterLat = documentSnapshot.getGeoPoint("location").getLatitude();
                        double scooterLng = documentSnapshot.getGeoPoint("location").getLongitude();

                        if ("забронирован".equals(status) || "занят".equals(status)) {
                            Toast.makeText(getContext(), "Самокат недоступен для бронирования или поездки", Toast.LENGTH_SHORT).show();
                        } else {
                            // Проверка разрешения на местоположение
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                                    if (location != null) {
                                        float[] results = new float[1];
                                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), scooterLat, scooterLng, results);
                                        if (results[0] <= MAX_DISTANCE_METERS) {
                                            if ("start".equals(action)) {
                                                if (isCardSaved()) {
                                                    openTimePickerDialog();
                                                } else {
                                                    navigateToPaymentActivity();
                                                }
                                            } else if ("book".equals(action)) {
                                                bookScooter();
                                            }
                                        } else {
                                            Toast.makeText(getContext(), "Вы должны быть ближе к самокату, чтобы начать поездку", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "Не удалось получить ваше местоположение", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(getContext(), "Для продолжения необходимо разрешение на доступ к местоположению", Toast.LENGTH_SHORT).show();
                                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка при получении статуса самоката", e);
                        Toast.makeText(getContext(), "Ошибка при проверке статуса самоката", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e(TAG, "Scooter ID is null or empty when checking status");
        }
    }


    private void openTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            long rideDurationMillis = (hourOfDay * 60 + minute) * 60 * 1000;
            startRide(rideDurationMillis);
        }, 0, 30, true);
        timePickerDialog.setTitle("Выберите время поездки");
        timePickerDialog.show();
    }
    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        } else {
            Log.e(TAG, "Пользователь не авторизован");
            return null;
        }
    }
    private void startRide(long rideDurationMillis) {
        Intent serviceIntent = new Intent(getContext(), RideTimerService.class);
        serviceIntent.putExtra("rideDurationMillis", rideDurationMillis);
        serviceIntent.putExtra("scooterId", scooterId);
        serviceIntent.putExtra("totalDistance", totalDistance); // Если расстояние известно на момент запуска
        ContextCompat.startForegroundService(getContext(), serviceIntent);

        // Обновление статуса самоката и сохранение поездки
        if (!scooterId.isEmpty()) {
            db.collection("scooters").document(scooterId)
                    .update("status", "занят")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Поездка начата", Toast.LENGTH_SHORT).show();
                        String userId = getCurrentUserId();
                        Trip ride = new Trip(userId, scooterId, System.currentTimeMillis(), 0, 0);
                        db.collection("rides")
                                .add(ride)
                                .addOnSuccessListener(docRef -> Log.d(TAG, "Поездка сохранена: " + docRef.getId()))
                                .addOnFailureListener(e -> Log.e(TAG, "Ошибка при сохранении поездки", e));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка при начале поездки", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e(TAG, "Идентификатор скутера равен нулю или пуст при начале поездки");
        }
    }

    private void openBookingTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            // Рассчитываем время бронирования в миллисекундах
            long bookingDurationMillis = (hourOfDay * 60 + minute) * 60 * 1000;

            if (bookingDurationMillis > 15 * 60 * 1000) {
                Toast.makeText(getContext(), "Вы можете бронировать максимум на 15 минут.", Toast.LENGTH_SHORT).show();
            } else if (bookingDurationMillis <= 0) {
                Toast.makeText(getContext(), "Пожалуйста, выберите корректное время.", Toast.LENGTH_SHORT).show();
            } else {
                bookScooterWithTimer(bookingDurationMillis);
            }
        }, 0, 15, true); // По умолчанию выставляем максимум - 15 минут
        timePickerDialog.setTitle("Выберите время бронирования (до 15 минут)");
        timePickerDialog.show();
    }


    private void bookScooterWithTimer(long bookingDurationMillis) {
        if (!scooterId.isEmpty()) {
            db.collection("scooters").document(scooterId)
                    .update("status", "забронирован")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Самокат забронирован", Toast.LENGTH_SHORT).show();

                        // Запуск таймера для отмены бронирования
                        new android.os.Handler().postDelayed(() -> {
                            cancelBooking();
                        }, bookingDurationMillis);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка при бронировании", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e(TAG, "Scooter ID is null or empty when booking");
        }
    }

    private void cancelBooking() {
        if (!scooterId.isEmpty()) {
            db.collection("scooters").document(scooterId)
                    .update("status", "доступен")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Бронирование завершено", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка при отмене бронирования", e);
                    });
        } else {
            Log.e(TAG, "Scooter ID is null or empty when canceling booking");
        }
    }


    private void endRide() {
        Intent intent = new Intent("com.example.quickscoot.END_RIDE");
        getContext().sendBroadcast(intent);
    }
}
