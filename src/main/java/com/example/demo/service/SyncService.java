package com.example.demo.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.demo.common.FirebaseConstants;
import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

@Service
public class SyncService {
	private final GithubService githubService;
    private final Firestore db;

    public SyncService(GithubService githubService) throws Exception {
        this.githubService = githubService;
        this.db = FirestoreClientWrapper.getDb();
    }

    public void syncIssues(String owner, String repo) throws Exception {
        List<Map<String, Object>> issues = githubService.fetchTopNIssuesWithRetry(owner, repo, 5);

        for (Map<String, Object> issue : issues) {
            String id = (String) issue.get("id");
            DocumentReference docRef = db.collection("github_issues").document(id);
            
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot doc = future.get();

            if (doc.exists()) {
            	System.out.println("Issue already exists (duplicate): " + id);
            	continue;
            }
            writeIssueWithRetry(id, issue);
        }
    }
    
    public boolean writeIssueWithRetry(String docId, Map<String, Object> issueData) {
        int attempt = 0;
        long backoff = FirebaseConstants.INITIAL_BACKOFF_MS;

        while (attempt < FirebaseConstants.MAX_RETRIES) {
            try {
                DocumentReference docRef = db.collection("github_issues").document(docId);
                docRef.set(issueData).get();  // blocking call to wait for completion
                System.out.println("Issue " + docId + " written to Firestore.");
                return true;

            } catch (Exception ex) {
                attempt++;

                // Handle Firestore-specific errors
                Throwable cause = ex.getCause();
                if (cause instanceof ApiException apiEx) {
                    StatusCode.Code code = apiEx.getStatusCode().getCode();

                    switch (code) {
                        case PERMISSION_DENIED, UNAUTHENTICATED, INVALID_ARGUMENT -> {
                            System.err.println("Unrecoverable Firestore error: " + code);
                            return false;
                        }
                        case UNAVAILABLE, DEADLINE_EXCEEDED, ABORTED, INTERNAL -> {
                            System.err.println("Transient Firestore error: " + code + ", retrying...");
                        }
                        default -> {
                            System.err.println("Unexpected Firestore error: " + code);
                            return false;
                        }
                    }
                } else if (cause instanceof IOException) {
                    System.err.println("Network error: " + cause.getMessage());
                } else {
                    System.err.println("Unknown error: " + ex.getMessage());
                    return false;
                }

                // Backoff before retrying
                try {
                    TimeUnit.MILLISECONDS.sleep(backoff);
                } catch (InterruptedException ignored) {
                }
                backoff *= 2; // exponential backoff
            }
        }

        System.err.println("Failed to write issue after " + FirebaseConstants.MAX_RETRIES + " attempts.");
        return false;
    }

}
