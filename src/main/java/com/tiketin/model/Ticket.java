package com.tiketin.model;

public class Ticket {
    private int id;
    private int orderId;
    private int eventId;
    private String ticketCode;
    private int purchasePrice;
    private String status;
    private String createdAt;

    public Ticket() {}

    public Ticket(int id, int orderId, int eventId, String ticketCode, int purchasePrice, String status, String createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.eventId = eventId;
        this.ticketCode = ticketCode;
        this.purchasePrice = purchasePrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public int getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(int purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
