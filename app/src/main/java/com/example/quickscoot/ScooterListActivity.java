package com.example.quickscoot;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScooterListActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ListView scooterListView;
    private List<Scooter> scooterList;
    private FirebaseFirestore db;
    private double userLatitude;
    private double userLongitude;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scooter_list);

        scooterListView = findViewById(R.id.scooterListView);
        db = FirebaseFirestore.getInstance();
        scooterList = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Получение местоположения пользователя
        getUserLocation();

        // Настройка спиннера для сортировки
        Spinner sortSpinner = findViewById(R.id.sortSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (scooterList.isEmpty()) return;

                if (position == 0) { // Сортировка по расстоянию
                    sortById();
                } else if (position == 1) { // Сортировка по заряду
                    sortByBattery();
                } else if (position == 2) { // Сортировка по ID
                    sortByDistance();
                }
                // Обновите адаптер
                updateAdapter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Получение данных о самокатах из Firestore
        loadScooters();
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Запросите разрешения
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            userLatitude = location.getLatitude();
                            userLongitude = location.getLongitude();
                            Log.d("ScooterListActivity", "User Location: " + userLatitude + ", " + userLongitude);
                        } else {
                            Log.w("ScooterListActivity", "Failed to get location.");
                        }
                    }
                });
    }

    private void loadScooters() {
        db.collection("scooters").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = document.getId();
                                Double battery = document.getDouble("battery");
                                GeoPoint geoPoint = document.getGeoPoint("location");
                                String status = document.getString("status");

                                if (geoPoint != null) {
                                    org.osmdroid.util.GeoPoint location = new org.osmdroid.util.GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
                                    scooterList.add(new Scooter(id, battery, location, status));
                                }
                            }
                            updateAdapter();
                        } else {
                            Log.w("ScooterListActivity", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void sortByDistance() {
        Location userLocation = new Location("userLocation");
        userLocation.setLatitude(userLatitude);
        userLocation.setLongitude(userLongitude);

        for (Scooter scooter : scooterList) {
            Location scooterLocation = new Location("scooterLocation");
            scooterLocation.setLatitude(scooter.getLocation().getLatitude());
            scooterLocation.setLongitude(scooter.getLocation().getLongitude());
            scooter.setDistance(userLocation.distanceTo(scooterLocation)); // Нужно добавить метод в классе Scooter
        }

        Collections.sort(scooterList, new Comparator<Scooter>() {
            @Override
            public int compare(Scooter s1, Scooter s2) {
                return Double.compare(s1.getDistance(), s2.getDistance());
            }
        });
    }

    private void sortByBattery() {
        Collections.sort(scooterList, new Comparator<Scooter>() {
            @Override
            public int compare(Scooter s1, Scooter s2) {
                return s2.getBattery().compareTo(s1.getBattery());
            }
        });
    }

    private void sortById() {
        Collections.sort(scooterList, new Comparator<Scooter>() {
            @Override
            public int compare(Scooter s1, Scooter s2) {
                return Integer.compare(Integer.parseInt(s1.getId()), Integer.parseInt(s2.getId())); // Сравнение по ID как числовому значению
            }
        });
    }

    private void updateAdapter() {
        ScooterAdapter adapter = new ScooterAdapter(ScooterListActivity.this, (ArrayList<Scooter>) scooterList);
        scooterListView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation(); // Повторно запрашиваем местоположение, если разрешение получено
            } else {
                Log.w("ScooterListActivity", "Location permission denied.");
            }
        }
    }
}
