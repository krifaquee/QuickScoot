package com.example.quickscoot;
import android.os.Parcel;
import android.os.Parcelable;
import org.osmdroid.util.GeoPoint;

public class Scooter implements Parcelable {
    private String id;
    private Double battery;
    private org.osmdroid.util.GeoPoint location;
    private String status;
    private float distance; // Новое поле для хранения расстояния

    public Scooter(String id, Double battery, org.osmdroid.util.GeoPoint location, String status) {
        this.id = id;
        this.battery = battery;
        this.location = location;
        this.status = status;
        this.distance = Float.MAX_VALUE; // Изначально расстояние максимальное
    }

    protected Scooter(Parcel in) {
        id = in.readString();
        battery = in.readDouble();
        location = new org.osmdroid.util.GeoPoint(in.readDouble(), in.readDouble());
        status = in.readString();
        distance = in.readFloat(); // Чтение расстояния из Parcel
    }

    public Scooter() {
    }

    public static final Creator<Scooter> CREATOR = new Creator<Scooter>() {
        @Override
        public Scooter createFromParcel(Parcel in) {
            return new Scooter(in);
        }

        @Override
        public Scooter[] newArray(int size) {
            return new Scooter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeDouble(battery);
        dest.writeDouble(location.getLatitude());
        dest.writeDouble(location.getLongitude());
        dest.writeString(status);
        dest.writeFloat(distance); // Запись расстояния в Parcel
    }

    // Геттеры
    public String getId() {
        return id;
    }

    public Double getBattery() {
        return battery;
    }

    public org.osmdroid.util.GeoPoint getLocation() {
        return location;
    }

    public String getStatus() {
        return status;
    }

    public float getDistance() {
        return distance; // Геттер для расстояния
    }

    public void setDistance(float distance) {
        this.distance = distance; // Сеттер для расстояния
    }
    public static GeoPoint convertGeoPoint(com.google.firebase.firestore.GeoPoint geoPoint) {
        return new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
    }
}
