package com.itn.smartitn.evaluation;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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

    public static class RatingResponse {
        @SerializedName("status") public boolean status;
        @SerializedName("rating") public float   rating;
        @SerializedName("count")  public int     count;
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

        @POST("API/evaluation/{student_id}")
        Call<EvalResponse> submit(@Path("student_id") int studentId,
                                  @Body EvalRequest body);

        @GET("API/evaluationRating/{id_cours}")
        Call<RatingResponse> getRating(@Path("id_cours") int idCours);
    }

    private int            courseId;
    private int            studentId;
    private final List<Critere>   criteres   = new ArrayList<>();
    private final List<RatingBar> ratingBars = new ArrayList<>();
    private LinearLayout criteresContainer;
    private EvalApi      evalApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluation_evaluation);

        // Récupération de l'ID étudiant depuis la session
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
        Button btnSubmit  = findViewById(R.id.btn_submit);

        evalApi = MainActivity.retrofit.create(EvalApi.class);

        loadCriteres();
        checkCurrentRating();
        
        btnSubmit.setOnClickListener(v -> submitEvaluation());
    }

    private void checkCurrentRating() {
        evalApi.getRating(courseId).enqueue(new Callback<RatingResponse>() {
            @Override
            public void onResponse(Call<RatingResponse> call, Response<RatingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    float rating = response.body().rating;
                    int count = response.body().count;
                    Toast.makeText(EvaluationActivity.this, 
                        String.format(Locale.getDefault(), "Note actuelle: %.1f/5 (%d évaluations)", rating, count), 
                        Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<RatingResponse> call, Throwable t) {}
        });
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

            // Création du RatingBar avec style par défaut pour assurer l'interactivité
            RatingBar rb = new RatingBar(this, null, android.R.attr.ratingBarStyle);
            rb.setNumStars(5); // Fixé à 5 étoiles
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
