package com.itn.smartitn.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.itn.smartitn.R;

import com.itn.smartitn.auth.utils.SessionManager;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

// =============================================================================
//  MainActivity.java
//  Contient :
//    • MainActivity          — Activité 1 : saisie du matricule
//    • ApiClient             — Singleton Retrofit
//    • ApiService            — Interface endpoints Retrofit
//    • DashboardResponse     — Modèle GET /API/dashboard/{id}
//    • PresenceResponse      — Modèle GET /API/todayAttendance/{id}
//    • EvenementRecent       — Sous-modèle événements/annonces
// =============================================================================

public class MainActivity extends AppCompatActivity {
    private EditText    etMatricule;
    private Button      btnAcceder;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_dashboard_main);
//
//        etMatricule = findViewById(R.id.et_matricule);
//        btnAcceder  = findViewById(R.id.btn_acceder);
//        progressBar = findViewById(R.id.progress_bar);
//
//        btnAcceder.setOnClickListener(v -> validerEtNaviguer());

        SessionManager sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            // Are you serious? by which means we got here without being logged in?
            finish();
        }

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra(DashboardActivity.EXTRA_STUDENT_ID, sessionManager.getUser().getId());
        startActivity(intent);
    }

    private void validerEtNaviguer() {
        String saisie = etMatricule.getText().toString().trim();

        if (TextUtils.isEmpty(saisie)) {
            etMatricule.setError("Veuillez entrer votre numéro matricule");
            etMatricule.requestFocus();
            return;
        }

        int studentId;
        try {
            studentId = Integer.parseInt(saisie);
        } catch (NumberFormatException e) {
            etMatricule.setError("Le matricule doit être un nombre entier");
            etMatricule.requestFocus();
            return;
        }

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra(DashboardActivity.EXTRA_STUDENT_ID, studentId);
        startActivity(intent);
    }

    // =========================================================================
    //  ApiClient — Singleton Retrofit
    //  Base URL : https://itn.ub.edu.bi/
    // =========================================================================

    public static class ApiClient {

        private static final String BASE_URL     = "https://itn.ub.edu.bi/";
        private static       Retrofit sInstance  = null;

        private ApiClient() {}

        public static Retrofit getInstance() {
            if (sInstance == null) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build();

                sInstance = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build();
            }
            return sInstance;
        }
    }

    // =========================================================================
    //  ApiService — Interface Retrofit (endpoints utilisés)
    // =========================================================================

    public interface ApiService {

        /** GET /API/dashboard/{student_id} */
        @GET("API/dashboard/{student_id}")
        Call<DashboardResponse> getDashboard(@Path("student_id") int studentId);

        /** GET /API/todayAttendance/{student_id} */
        @GET("API/todayAttendance/{student_id}")
        Call<PresenceResponse> getTodayAttendance(@Path("student_id") int studentId);
    }

    // =========================================================================
    //  DashboardResponse — Modèle de GET /API/dashboard/{student_id}
    //
    //  JSON attendu :
    //  {
    //    "status": true,
    //    "etudiant": { "nom":"Doe","prenom":"John","niveau":"Bac2","email":"..." },
    //    "total_presences": 12,
    //    "total_evaluations": 5,
    //    "total_cours": 4,
    //    "evenements_recents": [ { "id":1,"titre":"...","lieu":"...","date_evenement":"..." } ]
    //  }
    // =========================================================================

    public static class DashboardResponse {

        @SerializedName("status")
        private boolean status;

        @SerializedName("etudiant")
        private Etudiant etudiant;

        @SerializedName("total_presences")
        private int totalPresences;

        @SerializedName("total_evaluations")
        private int totalEvaluations;

        @SerializedName("total_cours")
        private int totalCours;

        @SerializedName("evenements_recents")
        private List<EvenementRecent> evenementsRecents;

        public boolean              isStatus()              { return status; }
        public Etudiant             getEtudiant()           { return etudiant; }
        public int                  getTotalPresences()     { return totalPresences; }
        public int                  getTotalEvaluations()   { return totalEvaluations; }
        public int                  getTotalCours()         { return totalCours; }
        public List<EvenementRecent> getEvenementsRecents() { return evenementsRecents; }

        // ----- Sous-modèle Etudiant ------------------------------------------
        public static class Etudiant {
            @SerializedName("nom")    private String nom;
            @SerializedName("prenom") private String prenom;
            @SerializedName("niveau") private String niveau;
            @SerializedName("email")  private String email;

            public String getNom()    { return nom; }
            public String getPrenom() { return prenom; }
            public String getNiveau() { return niveau; }
            public String getEmail()  { return email; }
        }
    }

    // =========================================================================
    //  PresenceResponse — Modèle de GET /API/todayAttendance/{student_id}
    //
    //  JSON attendu :
    //  { "status": true, "total_presences": 12, "message": "..." }
    // =========================================================================

    public static class PresenceResponse {

        @SerializedName("status")
        private boolean status;

        @SerializedName("total_presences")
        private int totalPresences;

        @SerializedName("message")
        private String message;

        public boolean isStatus()      { return status; }
        public int getTotalPresences() { return totalPresences; }
        public String getMessage()     { return message; }
    }

    // =========================================================================
    //  EvenementRecent — Sous-modèle événements dans DashboardResponse
    //
    //  JSON attendu :
    //  { "id":1, "titre":"Conférence IA", "date_evenement":"2026-04-15 14:00:00", "lieu":"Amphi A" }
    // =========================================================================

    public static class EvenementRecent {

        @SerializedName("id")
        private int id;

        @SerializedName("titre")
        private String titre;

        @SerializedName("date_evenement")
        private String dateEvenement;

        @SerializedName("lieu")
        private String lieu;

        public int    getId()            { return id; }
        public String getTitre()         { return titre; }
        public String getDateEvenement() { return dateEvenement; }
        public String getLieu()          { return lieu; }
    }
}
