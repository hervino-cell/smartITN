package com.itn.smartitn.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.itn.smartitn.R;

import com.itn.smartitn.dashboard.MainActivity.ApiClient;
import com.itn.smartitn.dashboard.MainActivity.ApiService;
import com.itn.smartitn.dashboard.MainActivity.DashboardResponse;
import com.itn.smartitn.dashboard.MainActivity.EvenementRecent;
import com.itn.smartitn.dashboard.MainActivity.PresenceResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// =============================================================================
//  DashboardActivity.java
//  Activité 2 : Tableau de bord
//  Appelle 2 endpoints via Retrofit et remplit dynamiquement :
//    • Carte Présences     ← GET /API/todayAttendance/{id}
//    • Carte Évaluations   ← GET /API/dashboard/{id}
//    • Carte Cours         ← GET /API/dashboard/{id}
//    • Carte Annonces      ← GET /API/dashboard/{id} (evenements_recents)
//    • Liste annonces      ← titres + lieux des événements récents
//
//  Toutes les classes métier (modèles, ApiClient, ApiService)
//  sont définies comme classes statiques internes dans MainActivity.java.
// =============================================================================

public class DashboardActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "student_id";

    // ── Card View ──────────────────────────────────────────────────────────
    private CardView cardProfile;
    private CardView cardPresences;
    private CardView cardEvaluations;
    private CardView cardCours;
    private CardView cardAnnonces;

    // ── Vues Header ──────────────────────────────────────────────────────────
    private TextView tvBonjour;
    private TextView tvNiveau;

    // ── Vues Cartes ──────────────────────────────────────────────────────────
    private TextView tvCountPresences;
    private TextView tvCountEvaluations;
    private TextView tvCountCours;
    private TextView tvCountAnnonces;

    // ── Section liste annonces ────────────────────────────────────────────────
    private LinearLayout llAnnonces;

    // ── Chargement ───────────────────────────────────────────────────────────
    private ProgressBar progressBar;
    private ScrollView  layoutContent;

    // ── Données ──────────────────────────────────────────────────────────────
    private int        studentId;
    private ApiService apiService;

    // =========================================================================
    //  Cycle de vie
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_dashboard);

        cardProfile = findViewById(R.id.card_profile);
        cardPresences = findViewById(R.id.card_presences);
        cardEvaluations = findViewById(R.id.card_evaluations);
        cardCours = findViewById(R.id.card_cours);
        cardAnnonces = findViewById(R.id.card_annonces);

        cardProfile.setOnClickListener(v -> {
//            Toast.makeText(this, "Carte Présences cliquée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, com.itn.smartitn.ProfileActivity.class));
        });
        cardPresences.setOnClickListener(v -> {
//            Toast.makeText(this, "Carte Présences cliquée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, com.itn.smartitn.presence.MainActivity.class));
        });
        cardEvaluations.setOnClickListener(v -> {
//            Toast.makeText(this, "Carte Présences cliquée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, com.itn.smartitn.evaluation.MainActivity.class));
        });
        cardCours.setOnClickListener(v -> {
//            Toast.makeText(this, "Carte Présences cliquée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, com.itn.smartitn.visualisation.MainActivity.class));
        });
        cardAnnonces.setOnClickListener(v -> {
//            Toast.makeText(this, "Carte Présences cliquée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, com.itn.smartitn.events.MainActivity.class));
        });


        studentId  = getIntent().getIntExtra(EXTRA_STUDENT_ID, -1);
        apiService = ApiClient.getInstance().create(ApiService.class);

        lierVues();
        chargerDonnees();
    }

    // =========================================================================
    //  Initialisation des vues
    // =========================================================================

    private void lierVues() {
        tvBonjour          = findViewById(R.id.tv_bonjour);
        tvNiveau           = findViewById(R.id.tv_niveau);
        tvCountPresences   = findViewById(R.id.tv_count_presences);
        tvCountEvaluations = findViewById(R.id.tv_count_evaluations);
        tvCountCours       = findViewById(R.id.tv_count_cours);
        tvCountAnnonces    = findViewById(R.id.tv_count_annonces);
        llAnnonces         = findViewById(R.id.ll_annonces);
        progressBar        = findViewById(R.id.progress_bar);
        layoutContent      = findViewById(R.id.layout_content);
    }

    // =========================================================================
    //  Chargement des données (2 appels Retrofit en parallèle)
    // =========================================================================

    private void chargerDonnees() {
        afficherChargement(true);
        appelDashboard();
        appelPresences();
    }

    // ── Appel 1 : GET /API/dashboard/{student_id} ────────────────────────────

    private void appelDashboard() {
        apiService.getDashboard(studentId).enqueue(new Callback<DashboardResponse>() {

            @Override
            public void onResponse(Call<DashboardResponse> call,
                                   Response<DashboardResponse> response) {
                afficherChargement(false);

                if (!response.isSuccessful() || response.body() == null) {
                    afficherErreur("Erreur serveur : " + response.code());
                    return;
                }

                DashboardResponse data = response.body();

                if (!data.isStatus()) {
                    Toast.makeText(DashboardActivity.this,
                            "Matricule introuvable : " + studentId,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // ── Header ──────────────────────────────────────────────────
                DashboardResponse.Etudiant etudiant = data.getEtudiant();
                if (etudiant != null) {
                    tvBonjour.setText("Bonjour, " + etudiant.getPrenom()
                            + " " + etudiant.getNom() + " !");
                    tvNiveau.setText(etudiant.getNiveau() + " – ITN Bujumbura");
                }

                // ── Cartes ───────────────────────────────────────────────────
                tvCountEvaluations.setText(String.valueOf(data.getTotalEvaluations()));
                tvCountCours.setText(String.valueOf(data.getTotalCours()));

                // ── Annonces (événements récents) ────────────────────────────
                List<EvenementRecent> evenements = data.getEvenementsRecents();
                int nbAnnonces = (evenements != null) ? evenements.size() : 0;
                tvCountAnnonces.setText(String.valueOf(nbAnnonces));
                afficherListeAnnonces(evenements);
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                afficherChargement(false);
                afficherErreur("Connexion impossible : " + t.getMessage());
            }
        });
    }

    // ── Appel 2 : GET /API/todayAttendance/{student_id} ──────────────────────

    private void appelPresences() {
        apiService.getTodayAttendance(studentId).enqueue(new Callback<PresenceResponse>() {

            @Override
            public void onResponse(Call<PresenceResponse> call,
                                   Response<PresenceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isStatus()) {
                    tvCountPresences.setText(
                            String.valueOf(response.body().getTotalPresences()));
                } else {
                    tvCountPresences.setText("0");
                }
            }

            @Override
            public void onFailure(Call<PresenceResponse> call, Throwable t) {
                tvCountPresences.setText("–");
            }
        });
    }

    // =========================================================================
    //  Affichage dynamique de la liste des annonces
    // =========================================================================

    private void afficherListeAnnonces(List<EvenementRecent> evenements) {
        llAnnonces.removeAllViews();

        if (evenements == null || evenements.isEmpty()) {
            TextView tvVide = new TextView(this);
            tvVide.setText("Aucune annonce récente");
            tvVide.setTextColor(getResources().getColor(android.R.color.darker_gray));
            llAnnonces.addView(tvVide);
            return;
        }

        for (EvenementRecent ev : evenements) {
            TextView tv = new TextView(this);
            tv.setText("• " + ev.getTitre() + " — " + ev.getLieu());
            tv.setTextSize(14f);
            tv.setPadding(0, 8, 0, 8);
            llAnnonces.addView(tv);
        }
    }

    // =========================================================================
    //  Utilitaires UI
    // =========================================================================

    private void afficherChargement(boolean enCours) {
        progressBar.setVisibility(enCours ? View.VISIBLE  : View.GONE);
        layoutContent.setVisibility(enCours ? View.GONE   : View.VISIBLE);
    }

    private void afficherErreur(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}