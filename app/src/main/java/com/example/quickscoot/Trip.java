package com.example.quickscoot;

public class Trip {
    private String userId;
    private String scooterId;
    private long startTime;
    private long endTime;
    private double distance; // расстояние в метрах

    // Конструктор
    public Trip(String userId, String scooterId, long startTime, long endTime, double distance) {
        this.userId = userId;
        this.scooterId = scooterId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distance = distance;
    }

    // Пустой конструктор для Firestore
    public Trip() {}

    // Геттеры и сеттеры
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getScooterId() { return scooterId; }
    public void setScooterId(String scooterId) { this.scooterId = scooterId; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}
