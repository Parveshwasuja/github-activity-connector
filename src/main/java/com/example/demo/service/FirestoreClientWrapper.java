package com.example.demo.service;

import java.io.FileInputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

public class FirestoreClientWrapper {
	private static Firestore fireStoreInstance;
	
	// Private constructor to prevent instantiation
    private FirestoreClientWrapper() {}

    public static void init(String serviceAccountPath) throws Exception {
    	// ✅ Check if already initialized
        if (FirebaseApp.getApps().isEmpty()) {
            FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        }
        fireStoreInstance = FirestoreClient.getFirestore();
    }

    public static Firestore getDb() throws Exception {
    	if(fireStoreInstance==null) {
    		String serviceAccountPath = "serviceAccountKey.json";
    		init(serviceAccountPath);
    	}
        return fireStoreInstance;
    }
}
