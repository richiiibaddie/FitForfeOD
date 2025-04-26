package com.example.myapplication;

public class User {
    private String full_name;
    private String email_address;
    private String password;
    private String rePassword;
    public User(String full_name, String email_address, String password, String rePassword) {
        this.full_name = full_name;
        this.email_address = email_address;
        this.password = password;
        this.rePassword = rePassword;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRePassword() {
        return rePassword;
    }

    public void setRePassword(String rePassword) {
        this.rePassword = rePassword;
    }
}
