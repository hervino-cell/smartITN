package com.itn.smartitn.presence;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.itn.smartitn.R;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class MainActivity extends AppCompatActivity {

    EditText etSearchCourse, etSearchLevel;
    RecyclerView rvCourses, rvLevels;
    Button btnStart;
    ProgressBar progressBar;
    TextView tvError, tvSelectedCourse, tvSelectedLevel;

    String selectedCourseId   = null;
    String selectedCourseName = null;
    String selectedCourseCode = null;
    String selectedLevel      = null;

    List<Course> allCourses   = new ArrayList<>();
    List<Course> shownCourses = new ArrayList<>();

    List<String> shownLevels  = new ArrayList<>(Arrays.asList("Bac1", "Bac2", "Bac3", "Bac4"));
    final List<String> allLevels = Arrays.asList("Bac1", "Bac2", "Bac3", "Bac4");

    CourseAdapter courseAdapter;
    LevelAdapter levelAdapter;

    // ─────────────────────────────────────────────
    // MODELS
    // ─────────────────────────────────────────────

    static class Course {
        @SerializedName("id")          String id;
        @SerializedName("nom_cours")   String nomCours;
        @SerializedName("code_cours")  String codeCours;
        @SerializedName("description") String description;
    }

    static class CourseResponse {
        @SerializedName("status") boolean status;
        @SerializedName("cours")  List<Course> cours;
    }

    interface ApiService {
        @GET("courses")
        Call<CourseResponse> getCourses();
    }

    // ─────────────────────────────────────────────
    // COURSE ADAPTER
    // ─────────────────────────────────────────────

    class CourseAdapter extends Adapter<CourseAdapter.VH> {

        class VH extends ViewHolder {
            TextView tvName, tvCode, tvDesc;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_course_name);
                tvCode = v.findViewById(R.id.tv_course_code);
                tvDesc = v.findViewById(R.id.tv_course_desc);
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.presence_item_course, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Course c = shownCourses.get(pos);

            h.tvName.setText(c.nomCours != null ? c.nomCours : "");
            h.tvCode.setText(c.codeCours != null ? c.codeCours : "");
            h.tvDesc.setText(c.description != null ? c.description : "");

            boolean sel = c.id != null && c.id.equals(selectedCourseId);

            h.itemView.setBackgroundResource(
                    sel ? R.drawable.item_selected_bg : R.drawable.item_bg
            );

            h.itemView.setOnClickListener(v -> {
                selectedCourseId   = c.id;
                selectedCourseName = c.nomCours;
                selectedCourseCode = c.codeCours;

                tvSelectedCourse.setText("✓ " + c.nomCours + " [" + c.codeCours + "]");
                tvSelectedCourse.setVisibility(View.VISIBLE);

                notifyDataSetChanged();
                refreshButton();
            });
        }

        @Override
        public int getItemCount() {
            return shownCourses.size();
        }
    }

    // ─────────────────────────────────────────────
    // LEVEL ADAPTER (SIMPLIFIÉ)
    // ─────────────────────────────────────────────

    class LevelAdapter extends Adapter<LevelAdapter.VH> {

        class VH extends ViewHolder {
            TextView tvLevel;

            VH(View v) {
                super(v);
                tvLevel = v.findViewById(R.id.tv_level_name);
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.presence_item_level, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            String lv = shownLevels.get(pos);

            // ✅ affichage simple
            h.tvLevel.setText(lv);

            boolean sel = lv.equals(selectedLevel);

            h.itemView.setBackgroundResource(
                    sel ? R.drawable.item_selected_bg : R.drawable.item_bg
            );

            h.itemView.setOnClickListener(v -> {
                selectedLevel = lv;

                tvSelectedLevel.setText("✓ Niveau: " + lv);
                tvSelectedLevel.setVisibility(View.VISIBLE);

                notifyDataSetChanged();
                refreshButton();
            });
        }

        @Override
        public int getItemCount() {
            return shownLevels.size();
        }
    }

    // ─────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_main);

        etSearchCourse   = findViewById(R.id.et_search_course);
        etSearchLevel    = findViewById(R.id.et_search_level);
        rvCourses        = findViewById(R.id.rv_courses);
        rvLevels         = findViewById(R.id.rv_levels);
        btnStart         = findViewById(R.id.btn_start);
        progressBar      = findViewById(R.id.progress_bar);
        tvError          = findViewById(R.id.tv_error);
        tvSelectedCourse = findViewById(R.id.tv_selected_course);
        tvSelectedLevel  = findViewById(R.id.tv_selected_level);

        courseAdapter = new CourseAdapter();
        levelAdapter  = new LevelAdapter();

        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        rvCourses.setAdapter(courseAdapter);

        rvLevels.setLayoutManager(new LinearLayoutManager(this));
        rvLevels.setAdapter(levelAdapter);

        // 🔍 Recherche cours (sécurisée)
        etSearchCourse.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                shownCourses.clear();
                String q = s.toString().toLowerCase().trim();

                for (Course c : allCourses) {
                    if (q.isEmpty() ||
                            (c.nomCours != null && c.nomCours.toLowerCase().contains(q)) ||
                            (c.codeCours != null && c.codeCours.toLowerCase().contains(q))) {

                        shownCourses.add(c);
                    }
                }

                courseAdapter.notifyDataSetChanged();
            }
        });

        // 🔍 Recherche niveau
        etSearchLevel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                shownLevels.clear();
                String q = s.toString().toLowerCase().trim();

                for (String lv : allLevels) {
                    if (q.isEmpty() || lv.toLowerCase().contains(q)) {
                        shownLevels.add(lv);
                    }
                }

                levelAdapter.notifyDataSetChanged();
            }
        });

        // ▶️ bouton
        btnStart.setOnClickListener(v -> {
            if (selectedCourseId == null || selectedLevel == null) {
                Toast.makeText(this, "Veuillez sélectionner un cours et un niveau", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, AttendanceActivity.class);
            intent.putExtra("course_id", selectedCourseId);
            intent.putExtra("course_name", selectedCourseName);
            intent.putExtra("course_code", selectedCourseCode);
            intent.putExtra("level", selectedLevel);

            startActivity(intent);
        });

        // 🌐 API CALL
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://itn.ub.edu.bi/API/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService api = retrofit.create(ApiService.class);

        api.getCourses().enqueue(new Callback<CourseResponse>() {
            @Override
            public void onResponse(Call<CourseResponse> call, Response<CourseResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    allCourses.addAll(response.body().cours);
                    shownCourses.addAll(allCourses);
                    courseAdapter.notifyDataSetChanged();
                } else {
                    tvError.setText("Impossible de charger les cours.");
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<CourseResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvError.setText("Erreur réseau: " + t.getMessage());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    void refreshButton() {
        boolean ok = selectedCourseId != null && selectedLevel != null;
        btnStart.setEnabled(ok);
        btnStart.setAlpha(ok ? 1f : 0.45f);
    }
}
