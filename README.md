# Firebase (Firestore) – GitHub-to-Firestore Sync Agent

 a data sync agent between GitHub and Firebase.

Once the repo code is in the local system, start the springboot application.
By default the port no will be 8080.

Hit this rest endpoint and the issues will be persisted to the firebase.
http://localhost:8080/api/github/repos/<owner>/<repo>
