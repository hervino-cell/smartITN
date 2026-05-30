package com.itn.smartitn.evaluation;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.annotations.SerializedName;
import com.itn.smartitn.R;
import com.itn.smartitn.auth.models.Etudiant;
import com.itn.smartitn.auth.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    /* ─── Modèles API ─── */
    public static class Course {
        @SerializedName("id")         public int    id;
        @SerializedName("nom_cours")  public String nomCours;
        @SerializedName("code_cours") public String codeCours;
    }

    public static class CoursesResponse {
        @SerializedName("status") public boolean      status;
        @SerializedName("cours")  public List<Course> cours;
    }

    public static class EvaluatedResponse {
        @SerializedName("status")      public boolean      status;
        @SerializedName("evaluations") public List<Evaluation> evaluations;

        public static class Evaluation {
            @SerializedName("cours_id") public int coursId;
        }
    }

    /* ─── Interface Retrofit ─── */
    public interface ApiService {
        @GET("API/courses")
        Call<CoursesResponse> getCourses();

        @GET("API/evaluation/{student_id}")
        Call<EvaluatedResponse> getEvaluatedCourses(@Path("student_id") int studentId);
    }

    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://itn.ub.edu.bi/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public static final ApiService apiService = retrofit.create(ApiService.class);

    // Interface pour le clic sur un cours
    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    /* ─── Adapter RecyclerView Multi-Section ─── */
    static class MultiSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM   = 1;

        static class Item {
            String title;
            Course course;
            boolean isHeader;
            boolean isEvaluated;

            Item(String title) { this.title = title; this.isHeader = true; }
            Item(Course course, boolean evaluated) { 
                this.course = course; 
                this.isEvaluated = evaluated; 
                this.isHeader = false; 
            }
        }

        private final List<Item> items = new ArrayList<>();
        private final OnCourseClickListener listener;

        MultiSectionAdapter(List<Course> available, List<Course> evaluated, OnCourseClickListener listener) {
            this.listener = listener;
            
            // Section : Cours a evaluer
            items.add(new Item("Cours a evaluer"));
            for (Course c : available) items.add(new Item(c, false));

            // Section : Cours evalues
            items.add(new Item("Cours evalues"));
            for (Course c : evaluated) items.add(new Item(c, true));
        }

        @Override
        public int getItemViewType(int pos) {
            return items.get(pos).isHeader ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new HeaderVH(v);
            }
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.evaluation_item_course, parent, false);
            return new ItemVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            Item item = items.get(pos);
            if (holder instanceof HeaderVH) {
                TextView tv = ((HeaderVH) holder).tv;
                tv.setText(item.title);
                tv.setPadding(32, 48, 32, 16);
                tv.setTextSize(16);
                tv.setTextColor(0xFF3F51B5);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                ItemVH h = (ItemVH) holder;
                h.tvNom.setText(item.course.nomCours);
                h.tvCode.setText(item.course.codeCours);
                
                if (item.isEvaluated) {
                    h.tvStatus.setText("✓ Évaluation terminée");
                    h.tvStatus.setTextColor(0xFF4CAF50);
                    h.itemView.setAlpha(0.6f);
                    h.itemView.setOnClickListener(v -> 
                        Toast.makeText(v.getContext(), "Ce cours est deja evalue", Toast.LENGTH_SHORT).show());
                } else {
                    h.tvStatus.setText("Évaluer ce cours");
                    h.tvStatus.setTextColor(0xFF3F51B5);
                    h.itemView.setAlpha(1.0f);
                    h.itemView.setOnClickListener(v -> listener.onCourseClick(item.course));
                }
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView tv;
            HeaderVH(View v) { super(v); tv = (TextView) v; }
        }

        static class ItemVH extends RecyclerView.ViewHolder {
            TextView tvNom, tvCode, tvStatus;
            ItemVH(View v) {
                super(v);
                tvNom    = v.findViewById(R.id.tv_nom_cours);
                tvCode   = v.findViewById(R.id.tv_code_cours);
                tvStatus = v.findViewById(R.id.tv_status_eval);
            }
        }
    }

    private RecyclerView recyclerView;
    private int studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluation_main);

        SessionManager sm = new SessionManager(this);
        Etudiant user = sm.getUser();
        if (user == null) { finish(); return; }
        studentId = user.getId();

        recyclerView = findViewById(R.id.recycler_courses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        apiService.getCourses().enqueue(new Callback<CoursesResponse>() {
            @Override
            public void onResponse(@NonNull Call<CoursesResponse> call, @NonNull Response<CoursesResponse> r1) {
                if (r1.isSuccessful() && r1.body() != null) {
                    fetchEvaluatedAndSplit(r1.body().cours);
                }
            }
            @Override public void onFailure(@NonNull Call<CoursesResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEvaluatedAndSplit(List<Course> allCourses) {
        apiService.getEvaluatedCourses(studentId).enqueue(new Callback<EvaluatedResponse>() {
            @Override
            public void onResponse(@NonNull Call<EvaluatedResponse> call, @NonNull Response<EvaluatedResponse> r2) {
                Set<Integer> evaluatedIds = new HashSet<>();
                if (r2.isSuccessful() && r2.body() != null && r2.body().evaluations != null) {
                    for (EvaluatedResponse.Evaluation e : r2.body().evaluations) {
                        evaluatedIds.add(e.coursId);
                    }
                }

                List<Course> available = new ArrayList<>();
                List<Course> evaluated = new ArrayList<>();

                for (Course c : allCourses) {
                    if (evaluatedIds.contains(c.id)) evaluated.add(c);
                    else available.add(c);
                }

                recyclerView.setAdapter(new MultiSectionAdapter(available, evaluated, course -> {
                    Intent i = new Intent(MainActivity.this, EvaluationActivity.class);
                    i.putExtra("course_id", course.id);
                    i.putExtra("course_name", course.nomCours);
                    startActivity(i);
                }));
            }

            @Override public void onFailure(@NonNull Call<EvaluatedResponse> call, @NonNull Throwable t) {
                // En cas d'échec de la récupération des évaluations, on considère tout comme disponible
                recyclerView.setAdapter(new MultiSectionAdapter(allCourses, new ArrayList<>(), course -> {
                    Intent i = new Intent(MainActivity.this, EvaluationActivity.class);
                    i.putExtra("course_id", course.id);
                    i.putExtra("course_name", course.nomCours);
                    startActivity(i);
                }));
            }
        });
    }
}
