package com.example.translator;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

public class FBref {

    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static FirebaseStorage  FBST = FirebaseStorage.getInstance();

    public static DatabaseReference refUsers = FBDB.getReference("Users");

}
