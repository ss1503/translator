package com.example.translator;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FBref {

    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();

    public static DatabaseReference refUsers = FBDB.getReference("Users");

}
