package com.re.ordermanagement.model;

public enum OrderStatus {
    PENDING("Chờ xử lý"),
    SHIPPING("Đang giao"),
    COMPLETED("Hoàn thành"),
    CANCELED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

