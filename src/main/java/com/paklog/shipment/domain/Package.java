package com.paklog.shipment.domain;

public class Package {
    private final double weight;
    private final double length;
    private final double width;
    private final double height;
    private final String type;

    public Package(double weight, double length, double width, double height, String type) {
        this.weight = weight;
        this.length = length;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public double getWeight() {
        return weight;
    }

    public double getLength() {
        return length;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String getType() {
        return type;
    }
}