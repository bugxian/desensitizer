package com.desensitizer.test.model;

import com.desensitizer.core.annotation.Sensitive;
import com.desensitizer.core.api.SensitiveType;

public class User {

    @Sensitive(type = SensitiveType.NAME)
    private String name;

    @Sensitive(type = SensitiveType.PHONE)
    private String phone;

    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;

    private String address;

    public User() {
    }

    public User(String name, String phone, String idCard, String address) {
        this.name = name;
        this.phone = phone;
        this.idCard = idCard;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", idCard='" + idCard + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}