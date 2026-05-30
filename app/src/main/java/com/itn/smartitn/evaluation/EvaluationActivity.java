package com.itn.smartitn.evaluation;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.annotations.SerializedName;
import com.itn.smartitn.R;
import com.itn.smartitn.auth.models.Etudiant;
import com.itn.smartitn.auth.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class EvaluationActivity extends AppCompatActivity {

    /* ─── Modèles API ─── */
    public static class Critere {
        @SerializedName("id")      public int    id;
        @SerializedName("libelle") public String libelle;
        @SerializedName("ordre")   public int    ordre;
    }

    public static class CriteresResponse {
        @SerializedName("status")   public boolean       status;
        @SerializedName("criteres") public List<Critere> criteres;
    }

    // Modèle pour parser evaluationRating/{course_id}
    public static class EvaluationRatingResponse {
        @SerializedName("status")      public boolean           status;
        @SerializedName("evaluations") public List<EvalEntry>   evaluations;

        public static class EvalEntry {
            @SerializedName("etudiant") public Etudiant etudiant;

            public static class Etudiant {
                @SerializedName("id") public String id; // retourné comme String dans le JSON
            }
        }
    }

    static class CritereNote {
        @SerializedName("critere_id") int critereId;
        @SerializedName("note")       int note;
        CritereNote(int critereId, int note) {
            this.critereId = critereId;
            this.note      = note;
        }
    }

    static class EvalRequest {
        @SerializedName("cours_id")        int               coursId;
        @SerializedName("date_evaluation") String            dateEvaluation;
        @SerializedName("criteres")        List<CritereNote> criteres;
        EvalRequest(int coursId, String date, List<CritereNote> criteres) {
            this.coursId        = coursId;
            this.dateEvaluation = date;
            this.criteres       = criteres;
        }
    }

    static class EvalResponse {
        @SerializedName("status")  boolean status;
        @SerializedName("message") String  message;
    }

    /* ─── Interface Retrofit ─── */
    interface EvalApi {
        @GET("API/criteres")
        Call<CriteresResponse> getCriteres();

        @GET("API/evaluationRating/{course_id}")
        Call<EvaluationRatingResponse> getEvaluationRating(@Path("course_id") int courseId);

        @POST("API/evaluation/{student_id}")
        Call<EvalResponse> submit(@Path("student_id") int studentId,
                                  @Body EvalRequest body);
    }

    private int            courseId;
    private int            studentId;
    private final List<Critere>   criteres   = new ArrayList<>();
    private final List<RatingBar> ratingBars = new ArrayList<>();
    private LinearLayout criteresContainer;
    private Button       btnSubmit;
    private EvalApi      evalApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluation_evaluation);

        SessionManager sessionManager = new SessionManager(this);
        Etudiant user = sessionManager.getUser();
        if (user != null) {
            studentId = user.getId();
        } else {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        courseId = getIntent().getIntExtra("course_id", 1);
        String courseName = getIntent().getStringExtra("course_name");

        TextView tvTitle = findViewById(R.id.tv_eval_title);
        tvTitle.setText("Évaluer : " + courseName);

        criteresContainer = findViewById(R.id.criteres_container);
        btnSubmit         = findViewById(R.id.btn_submit);

        // Bouton désactivé jusqu'à confirmation
        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.4f);

        evalApi = MainActivity.retrofit.create(EvalApi.class);

        // 1. Vérifier si déjà évalué via evaluationRating/{course_id}
        checkIfAlreadyEvaluated();
    }

    /**
     * Appelle GET /API/evaluationRating/{course_id} et parcourt la liste
     * des évaluations pour voir si studentId y figure déjà.
     */
    private void checkIfAlreadyEvaluated() {
        evalApi.getEvaluationRating(courseId)
                .enqueue(new Callback<EvaluationRatingResponse>() {
                    @Override
                    public void onResponse(Call<EvaluationRatingResponse> call,
                                           Response<EvaluationRatingResponse> response) {
                        boolean dejaEvalue = false;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().evaluations != null) {

                            String myId = String.valueOf(studentId);

                            for (EvaluationRatingResponse.EvalEntry entry
                                    : response.body().evaluations) {
                                if (entry.etudiant != null
                                        && myId.equals(entry.etudiant.id)) {
                                    dejaEvalue = true;
                                    break;
                                }
                            }
                        }

                        if (dejaEvalue) {
                            showAlreadyEvaluatedDialog();
                        } else {
                            // Pas encore évalué : charger le formulaire
                            loadCriteres();
                            btnSubmit.setEnabled(true);
                            btnSubmit.setAlpha(1.0f);
                            btnSubmit.setOnClickListener(v -> submitEvaluation());
                        }
                    }

                    @Override
                    public void onFailure(Call<EvaluationRatingResponse> call, Throwable t) {
                        // Réseau indisponible : on laisse accéder
                        loadCriteres();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setAlpha(1.0f);
                        btnSubmit.setOnClickListener(v -> submitEvaluation());
                    }
                });
    }

    /**
     * Dialog non annulable — clic OK renvoie à la liste des cours.
     */
    private void showAlreadyEvaluatedDialog() {
        if (isFinishing() || isDestroyed()) return;

        new AlertDialog.Builder(this)
                .setTitle("Cours déjà évalué")
                .setMessage("Vous avez déjà soumis une évaluation pour ce cours.\nUne seule évaluation par cours est autorisée.")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    private void loadCriteres() {
        evalApi.getCriteres().enqueue(new Callback<CriteresResponse>() {
            @Override
            public void onResponse(Call<CriteresResponse> call, Response<CriteresResponse> r) {
                if (r.isSuccessful() && r.body() != null && r.body().status) {
                    criteres.addAll(r.body().criteres);
                    buildUI();
                } else {
                    Toast.makeText(EvaluationActivity.this, R.string.erreur_chargement, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<CriteresResponse> call, Throwable t) {
                Toast.makeText(EvaluationActivity.this, "Réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildUI() {
        criteresContainer.removeAllViews();
        ratingBars.clear();
        int dp = (int)(getResources().getDisplayMetrics().density);

        for (Critere c : criteres) {
            TextView tv = new TextView(this);
            tv.setText(c.libelle);
            tv.setTextSize(16f);
            tv.setPadding(0, 16 * dp, 0, 4 * dp);
            criteresContainer.addView(tv);

            RatingBar rb = new RatingBar(this, null, android.R.attr.ratingBarStyle);
            rb.setNumStars(5);
            rb.setStepSize(1.0f);
            rb.setRating(3f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 8 * dp;
            rb.setLayoutParams(params);

            criteresContainer.addView(rb);
            ratingBars.add(rb);
        }
    }

    private void submitEvaluation() {
        if (ratingBars.isEmpty()) {
            Toast.makeText(this, "Critères non encore chargés", Toast.LENGTH_SHORT).show();
            return;
        }

        List<CritereNote> notes = new ArrayList<>();
        for (int i = 0; i < criteres.size(); i++) {
            notes.add(new CritereNote(criteres.get(i).id, (int) ratingBars.get(i).getRating()));
        }

        Calendar cal = Calendar.getInstance();
        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        evalApi.submit(studentId, new EvalRequest(courseId, date, notes))
                .enqueue(new Callback<EvalResponse>() {
                    @Override
                    public void onResponse(Call<EvalResponse> call, Response<EvalResponse> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            Toast.makeText(EvaluationActivity.this, r.body().message, Toast.LENGTH_LONG).show();
                            if (r.body().status) finish();
                        }
                    }
                    @Override public void onFailure(Call<EvalResponse> call, Throwable t) {
                        Toast.makeText(EvaluationActivity.this, "Réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}