package com.example.translator;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class Users {
    //components
    private String UserId;
    private ArrayList<TextTranslate> translate;
    private boolean toCapture;

    //constructor
    public Users(String UserId)
    {
        this.UserId = UserId;
        this.translate = null;
        this.toCapture = false;
    }

    //default constructor
    public Users() {
    }

    //Setters and Getters
    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public ArrayList<TextTranslate> getTranslate() {
        return translate;
    }

    public void setTranslate(ArrayList<TextTranslate> translate) {
        this.translate = translate;
    }
}
