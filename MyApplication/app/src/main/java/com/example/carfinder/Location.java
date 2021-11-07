package com.example.carfinder;

public class Location {
    private double latitude;
    private double longitude;
    private float accuracy;
    String description;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getAccuracyDescription() {
        if (accuracy < 1) return("NINGUNA");
        else if (accuracy < 15) return("BUENA");
        else if (accuracy < 25) return("MEDIA");
        else return("MALA");
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
