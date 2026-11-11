package com.tiketin.model;

public class Order {
    private int id;
    private String orderCode;
    private int userId;
    private int eventId;
    private int quantity;
    private int unitPrice;
    private int totalPrice;
    private String paymentMethod;
    private String paymentProof;
    private String status;
    private String purchasedAt;
    private String buyerName;
    private String buyerUsername;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String eventLocation;
    private String eventCategory;
    private String ticketCodes;

    public Order() {}

    public Order(int id, String orderCode, int userId, int eventId, int quantity, int unitPrice,
                 int totalPrice, String paymentMethod, String paymentProof, String status,
                 String purchasedAt, String buyerName, String buyerUsername, String eventName,
                 String eventDate, String eventTime, String eventLocation, String eventCategory,
                 String ticketCodes) {
        this.id = id;
        this.orderCode = orderCode;
        this.userId = userId;
        this.eventId = eventId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.paymentProof = paymentProof;
        this.status = status;
        this.purchasedAt = purchasedAt;
        this.buyerName = buyerName;
        this.buyerUsername = buyerUsername;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.eventLocation = eventLocation;
        this.eventCategory = eventCategory;
        this.ticketCodes = ticketCodes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentProof() {
        return paymentProof;
    }

    public void setPaymentProof(String paymentProof) {
        this.paymentProof = paymentProof;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(String purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public void setBuyerUsername(String buyerUsername) {
        this.buyerUsername = buyerUsername;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getTicketCodes() {
        return ticketCodes;
    }

    public void setTicketCodes(String ticketCodes) {
        this.ticketCodes = ticketCodes;
    }
}
