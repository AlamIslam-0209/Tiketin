package com.tiketin.model;

public class Event {
    private int id;
    private int ownerId;
    private String name;
    private String date;
    private String time;
    private String location;
    private int price;
    private int slots;
    private String category;
    private String description;
    private String createdAt;
    private String ownerName;
    private String ownerOrg;
    private String imagePath;

    public Event() {}

    public Event(int id, int ownerId, String name, String date, String time, String location,
                 int price, int slots, String category, String description, String createdAt,
                 String ownerName, String ownerOrg) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.date = date;
        this.time = time;
        this.location = location;
        this.price = price;
        this.slots = slots;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
        this.ownerName = ownerName;
        this.ownerOrg = ownerOrg;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerOrg() {
        return ownerOrg;
    }

    public void setOwnerOrg(String ownerOrg) {
        this.ownerOrg = ownerOrg;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
