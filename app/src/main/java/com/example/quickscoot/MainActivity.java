package com.example.quickscoot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

import firebase.com.protolitewrapper.BuildConfig;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private ImageView profileImageView;
    private TextView aboutMeTextView;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private final List<Scooter> scooterList = new ArrayList<>();
    private GeoPoint userLocation;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10 секунд
    private Handler locationUpdateHandler;
    private Runnable locationUpdateRunnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startLocationUpdates();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        ImageButton burgerMenuButton = findViewById(R.id.burgerMenuButton);
        burgerMenuButton.setOnClickListener(v -> {
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        mapView = findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button listButton = findViewById(R.id.listButton);
        listButton.setOnClickListener(v -> showScooterList());

        requestLocationPermission();
        loadScootersFromFirestore();

        NavigationView navigationView = findViewById(R.id.navigation_view);
        profileImageView = navigationView.getHeaderView(0).findViewById(R.id.profile_image_view);
        aboutMeTextView = navigationView.getHeaderView(0).findViewById(R.id.about_me_text_view);
        loadProfileData();

        Button startRideButton = findViewById(R.id.startRideButton);
        startRideButton.setOnClickListener(v -> requestCameraPermission());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scooterLatitude") && intent.hasExtra("scooterLongitude")) {
            double scooterLatitude = intent.getDoubleExtra("scooterLatitude", 0);
            double scooterLongitude = intent.getDoubleExtra("scooterLongitude", 0);
            Log.d("MainActivity", "Scooter Latitude: " + scooterLatitude + ", Longitude: " + scooterLongitude); // Переходим к местоположению самоката на карте
            zoomToScooterLocation(scooterLatitude, scooterLongitude);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                Intent intentProfile = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intentProfile);
                return true;
            }
            if (id == R.id.nav_history) {
                Intent intentHistory = new Intent(MainActivity.this, TripHistoryActivity.class);
                startActivity(intentHistory);
                return true;
            }
            if (id == R.id.nav_payment) {
                Intent intentPayment = new Intent(MainActivity.this, PaymentActivity.class);
                startActivity(intentPayment);
                return true;
            }
            if (id == R.id.nav_faq) {
                Intent intentFAQ = new Intent(MainActivity.this, FAQActivity.class);
                startActivity(intentFAQ);
                return true;
            }
            if (id == R.id.nav_exit) {
                logoutUser();
                return true;
            }
            return false;
        });

        registerReceiver(profileUpdateReceiver, new IntentFilter("PROFILE_UPDATED"));
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        Intent intentLogin = new Intent(MainActivity.this, LoginActivity.class);
        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentLogin);

        finish();
    }


    private final BroadcastReceiver profileUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Обновление данных профиля
            loadProfileData();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Отменяем регистрацию при уничтожении активности
        unregisterReceiver(profileUpdateReceiver);
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
            locationUpdateHandler.removeCallbacks(locationUpdateRunnable); // Останавливаем обновления
        }
    }

    private void loadProfileData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        String firstName = sharedPreferences.getString("firstName", "");
        String lastName = sharedPreferences.getString("lastName", "");
        String encodedImage = sharedPreferences.getString("profileImage", "");

        if (!firstName.isEmpty() || !lastName.isEmpty()) {
            aboutMeTextView.setText(firstName + " " + lastName);
        }

        if (!encodedImage.isEmpty()) {
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImageView.setImageBitmap(decodedByte);
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startQrCodeScanner();
        }
    }

    private void startLocationUpdates() {
        locationUpdateHandler = new Handler();
        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                showUserLocation();
                locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        };
        locationUpdateHandler.post(locationUpdateRunnable);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            showUserLocation();
        }
    }

    private void startQrCodeScanner() {
        new IntentIntegrator(this)
                .setPrompt("Scan a QR code to start the ride")
                .setOrientationLocked(true)
                .initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scooterId = result.getContents();
                loadScooterInfo(scooterId);
            } else {
                Toast.makeText(this, "No QR code scanned", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadScooterInfo(String scooterId) {
        getScooterById(scooterId, scooter -> {
            if (scooter != null) {
                showScooterInfo(scooter);
            } else {
                Toast.makeText(MainActivity.this, "Scooter not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawer_menu, menu);
        return true;
    }

    private void getScooterById(String scooterId, ScooterCallback callback) {
        db.collection("scooters").document(scooterId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String id = documentSnapshot.getId();
                        Double battery = documentSnapshot.getDouble("battery");
                        com.google.firebase.firestore.GeoPoint firestoreLocation = documentSnapshot.getGeoPoint("location");
                        String status = documentSnapshot.getString("status");

                        if (firestoreLocation != null && battery != null) {
                            GeoPoint osmdroidLocation = new GeoPoint(firestoreLocation.getLatitude(), firestoreLocation.getLongitude());
                            Scooter scooter = new Scooter(id, battery, osmdroidLocation, status);
                            callback.onCallback(scooter); // Передаём самокат в callback
                        } else {
                            Log.w("MainActivity", "Scooter with ID " + id + " has missing data.");
                            callback.onCallback(null);
                        }
                    } else {
                        Toast.makeText(this, "Scooter not found", Toast.LENGTH_SHORT).show();
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("MainActivity", "Error loading scooter", e);
                    callback.onCallback(null);
                });
    }

    private interface ScooterCallback {
        void onCallback(Scooter scooter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation();
            } else {
                Toast.makeText(this, "Permission denied for location access", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapView.getController().setCenter(userLocation);

                Marker userMarker = new Marker(mapView);

                mapView.getOverlays().clear();

                userMarker.setPosition(userLocation);
                userMarker.setTitle("Вы здесь");
                if (userMarker != null) {
                    mapView.getOverlays().remove(userMarker);
                }
                userMarker.setIcon(getResources().getDrawable(R.drawable.ic_user_marker, null));
                mapView.getOverlays().clear();
                mapView.getOverlays().add(userMarker);

                displayScootersOnMap();
                mapView.invalidate();
            }
        });
    }

    private void loadScootersFromFirestore() {
        db.collection("scooters").get()
                .addOnSuccessListener(querySnapshot -> {
                    scooterList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String id = document.getId();
                        Double battery = document.getDouble("battery");
                        com.google.firebase.firestore.GeoPoint location = document.getGeoPoint("location");
                        String status = document.getString("status");

                        if (battery != null && location != null) {
                            GeoPoint osmdroidLocation = Scooter.convertGeoPoint(location);
                            scooterList.add(new Scooter(id, battery, osmdroidLocation, status));
                        } else {
                            Log.w("MainActivity", "Scooter with ID " + id + " has unavailable data.");
                        }
                    }
                    displayScootersOnMap();
                })
                .addOnFailureListener(e -> Log.w("MainActivity", "Failed to load scooters", e));
    }

    private void displayScootersOnMap() {
        for (Scooter scooter : scooterList) {
            Marker marker = new Marker(mapView);
            marker.setPosition(scooter.getLocation());
            marker.setTitle("ID: " + scooter.getId() + ", Status: " + scooter.getStatus());

            // Уменьшение размера изображения
            @SuppressLint("UseCompatLoadingForDrawables") Drawable originalIcon = getResources().getDrawable(R.drawable.ic_scooter_marker, null);
            Drawable resizedIcon = resizeDrawable(originalIcon, 30, 30); // Измените размеры по своему усмотрению
            marker.setIcon(resizedIcon);

            marker.setOnMarkerClickListener((marker1, mapView1) -> {
                showScooterInfo(scooter);
                return true;
            });
            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    public Drawable resizeDrawable(Drawable image, int width, int height) {
        Bitmap bitmap = ((BitmapDrawable) image).getBitmap();
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        return new BitmapDrawable(getResources(), resizedBitmap);
    }


    private void zoomToScooterLocation(double latitude, double longitude) {
        org.osmdroid.util.GeoPoint scooterGeoPoint = new org.osmdroid.util.GeoPoint(latitude, longitude);
        mapView.getController().setZoom(17); // Устанавливаем уровень зума
        mapView.getController().setCenter(scooterGeoPoint); // Центрируем карту на местоположении самоката
    }

    private void showScooterInfo(Scooter scooter) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ScooterInfoBottomSheet bottomSheet = ScooterInfoBottomSheet.newInstance(
                scooter.getBattery() + "%",
                "ID: " + scooter.getId(),
                calculateRange(scooter.getBattery()),
                scooter.getId() // Передача scooterId
        );
        bottomSheet.show(fragmentManager, "ScooterInfoBottomSheet");

        double latitude = scooter.getLocation().getLatitude();
        double longitude = scooter.getLocation().getLongitude();
        zoomToScooterLocation(latitude, longitude);
    }


    private String calculateRange(Double battery) {
        if (battery >= 75) {
            return "Запас хода: ~20 км";
        } else if (battery > 50) {
            return "Запас хода: ~15 км";
        } else {
            return "Запас хода: менее 10 км";
        }
    }

    private void showScooterList() {
        Intent intent = new Intent(MainActivity.this, ScooterListActivity.class);
        intent.putParcelableArrayListExtra("scooterList", new ArrayList<>(scooterList));
        startActivity(intent);
    }
}
