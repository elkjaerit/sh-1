package dk.elkjaerit.smartheating;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;

public class DemoSearch {

  public static void main(String[] args) throws IOException {
    FirestoreOptions firestoreOptions =
        FirestoreOptions.getDefaultInstance()
            .toBuilder()
            .setProjectId(ProjectInfo.PROJECT_ID)
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build();
    Firestore db = firestoreOptions.getService();



  }
}
