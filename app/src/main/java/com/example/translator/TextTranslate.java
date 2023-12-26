package com.example.translator;

public class TextTranslate {
    //components
    private String date;
    private String original;
    private String translated;

    //constructor
    public TextTranslate(String date, String original, String translated)
    {
        this.date = date;
        this.original = original;
        this.translated = translated;
    }

    //def constructor
    public TextTranslate() {
    }

    //Setters and Getters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getTranslated() {
        return translated;
    }

    public void setTranslated(String translated) {
        this.translated = translated;
    }
}
