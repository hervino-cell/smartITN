package com.itn.smartitn.visualisation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itn.smartitn.R;
import com.itn.smartitn.auth.utils.SessionManager;
import com.itn.smartitn.visualisation.adapters.CoursAdapter;
import com.itn.smartitn.visualisation.api.ApiClient;
import com.itn.smartitn.visualisation.api.ApiService;
import com.itn.smartitn.visualisation.models.Cours;
import com.itn.smartitn.visualisation.models.CoursResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements CoursAdapter.OnCoursClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    //student_id récupéré après login - à remplacer dynamiquement
    // Pour l'examen, on utilise un ID fixe ou celui stocké après login
    public static int STUDENT_ID = 1; // À remplacer par l'ID réel de l'étudiant connecté

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualisation_main);

        // Récupérer student_id si transmis depuis login
//        STUDENT_ID = getIntent().getIntExtra("student_id", 1);

        SessionManager sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            // Are you serious? by which means we got here without being logged in?
            finish();
        }
        STUDENT_ID = sessionManager.getUser().getId();

        recyclerView = findViewById(R.id.recyclerViewCours);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chargerCours();
    }

    private void chargerCours() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<CoursResponse> call = apiService.getCours();

        call.enqueue(new Callback<CoursResponse>() {
            @Override
            public void onResponse(Call<CoursResponse> call, Response<CoursResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    List<Cours> coursList = response.body().getCours();
                    if (coursList == null || coursList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        CoursAdapter adapter = new CoursAdapter(coursList, MainActivity.this);
                        recyclerView.setAdapter(adapter);
                    }
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Impossible de charger les cours", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CoursResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCoursClick(Cours cours) {
        Intent intent = new Intent(this, NotesActivity.class);
        intent.putExtra("cours_id", cours.getId());
        intent.putExtra("cours_nom", cours.getNomCours());
        intent.putExtra("cours_code", cours.getCodeCours());
        intent.putExtra("student_id", STUDENT_ID);
        startActivity(intent);
    }
}
