package com.itn.smartitn.visualisation;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.itn.smartitn.R;
import com.itn.smartitn.visualisation.api.ApiClient;
import com.itn.smartitn.visualisation.api.ApiService;
import com.itn.smartitn.visualisation.models.NoteResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotesActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvCoursNom, tvCoursCode, tvMoyenne, tvAppreciation;
    private TextView tvNoteTP, tvNoteExamen, tvEmpty;
    private LinearLayout layoutNotes;
    private int coursId, studentId;
    private String coursNom, coursCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualisation_notes);

        coursId   = getIntent().getIntExtra("cours_id", 0);
        coursNom  = getIntent().getStringExtra("cours_nom");
        coursCode = getIntent().getStringExtra("cours_code");
        studentId = getIntent().getIntExtra("student_id", 1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mes Notes");
        }

        progressBar    = findViewById(R.id.progressBar);
        tvCoursNom     = findViewById(R.id.tvCoursNom);
        tvCoursCode    = findViewById(R.id.tvCoursCode);
        tvMoyenne      = findViewById(R.id.tvMoyenne);
        tvAppreciation = findViewById(R.id.tvAppreciation);
        tvNoteTP       = findViewById(R.id.tvNoteTP);
        tvNoteExamen   = findViewById(R.id.tvNoteExamen);
        tvEmpty        = findViewById(R.id.tvEmpty);
        layoutNotes    = findViewById(R.id.layoutNotes);

        tvCoursNom.setText(coursNom);
        tvCoursCode.setText("Code : " + coursCode);

        chargerNotes();
    }

    private void chargerNotes() {
        progressBar.setVisibility(View.VISIBLE);
        layoutNotes.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        // Appel API: GET /API/notes/{course_id}/{student_id}
        Call<NoteResponse> call = apiService.getNotes(coursId, studentId);

        call.enqueue(new Callback<NoteResponse>() {
            @Override
            public void onResponse(Call<NoteResponse> call, Response<NoteResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    NoteResponse note = response.body();
                    if (note.isStatus()) {
                        afficherNotes(note);
                    } else {
                        // Notes pas encore disponibles
                        tvEmpty.setText("📋 " + (note.getMessage() != null ?
                                note.getMessage() : "Notes non disponibles pour ce cours"));
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmpty.setText("Erreur lors du chargement des notes");
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<NoteResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotesActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void afficherNotes(NoteResponse note) {
        layoutNotes.setVisibility(View.VISIBLE);

        tvNoteTP.setText(String.format("%.1f / 20", note.getNoteTP()));
        tvNoteExamen.setText(String.format("%.1f / 20", note.getNoteExamen()));
        tvMoyenne.setText(String.format("Moyenne : %.1f / 20", note.getMoyenne()));
        tvAppreciation.setText(note.getAppreciation());

        // Couleur selon la moyenne
        double moy = note.getMoyenne();
        if (moy >= 14) {
            tvMoyenne.setTextColor(Color.parseColor("#27AE60"));
            tvAppreciation.setTextColor(Color.parseColor("#27AE60"));
        } else if (moy >= 10) {
            tvMoyenne.setTextColor(Color.parseColor("#F39C12"));
            tvAppreciation.setTextColor(Color.parseColor("#F39C12"));
        } else {
            tvMoyenne.setTextColor(Color.parseColor("#E74C3C"));
            tvAppreciation.setTextColor(Color.parseColor("#E74C3C"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
