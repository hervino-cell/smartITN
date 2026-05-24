package com.itn.smartitn.presence;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.itn.smartitn.R;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class AttendanceActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────
    TextView tvHeaderCourse, tvHeaderCode, tvHeaderLevel;
    TextView tvStudentCount, tvPresentCount, tvAbsentCount;
    TextView tvErrorStudents, tvResultTitle, tvResultMsg;
    RecyclerView rvStudents;
    ProgressBar progressStudents;
    Button btnSelectAll, btnUnselectAll, btnSubmit, btnBack;
    LinearLayout panelList, panelResult, overlaySubmitting;
    CardView cardSummary;
    TableLayout tableResult;

    // ── Data ──────────────────────────────────────────────────────────
    String courseId, courseName, courseCode, level;
    List<Student> students = new ArrayList<>();
    StudentAdapter studentAdapter;

    // ── Models ────────────────────────────────────────────────────────

    static class Student {
        @SerializedName("id")     String id;
        @SerializedName("nom")    String nom;
        @SerializedName("prenom") String prenom;
        @SerializedName("niveau") String niveau;
        @SerializedName("gender") String gender;
        boolean present = false;
        String getFullName() { return prenom + " " + nom; }
    }

    static class StudentResponse {
        @SerializedName("status")    boolean status;
        @SerializedName("etudiants") List<Student> etudiants;
    }

    // ── New POST body ─────────────────────────────────────────────────
    // Sends:
    // {
    //   "cours_id": 1,
    //   "date_presence": "2026-03-10",
    //   "presents": [1, 2, 3, 4]
    // }

    static class AttendanceRequest {
        @SerializedName("cours_id")       int coursId;
        @SerializedName("date_presence")  String datePresence;
        @SerializedName("presents")       List<Integer> presents;

        AttendanceRequest(int coursId, String datePresence, List<Integer> presents) {
            this.coursId      = coursId;
            this.datePresence = datePresence;
            this.presents     = presents;
        }
    }

    static class AttendanceResponse {
        @SerializedName("status")  boolean status;
        @SerializedName("message") String message;
    }

    // ── API ───────────────────────────────────────────────────────────

    interface ApiService {
        @GET("students")
        Call<StudentResponse> getStudents();

        @POST("attendance")
        Call<AttendanceResponse> submitAttendance(@Body AttendanceRequest request);
    }

    // ── Adapter ───────────────────────────────────────────────────────

    class StudentAdapter extends Adapter<StudentAdapter.VH> {
        class VH extends ViewHolder {
            TextView tvName, tvId, tvGender, tvStatus;
            CheckBox cbPresent;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tv_student_name);
                tvId      = v.findViewById(R.id.tv_student_id);
                tvGender  = v.findViewById(R.id.tv_gender);
                tvStatus  = v.findViewById(R.id.tv_status);
                cbPresent = v.findViewById(R.id.cb_present);
            }
        }
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.presence_item_student, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(VH h, int pos) {
            Student s = students.get(pos);
            h.tvName.setText(s.getFullName());
            h.tvId.setText("ID : " + s.id);
            h.tvGender.setText(s.gender.equals("M") ? "♂" : "♀");
            h.tvGender.setBackgroundResource(s.gender.equals("M")
                    ? R.drawable.avatar_male : R.drawable.avatar_female);

            h.cbPresent.setOnCheckedChangeListener(null);
            h.cbPresent.setChecked(s.present);
            h.tvStatus.setText(s.present ? "Présent" : "Absent");
            h.tvStatus.setTextColor(getResources().getColor(s.present ? R.color.green : R.color.red));
            h.itemView.setAlpha(s.present ? 1f : 0.72f);

            h.cbPresent.setOnCheckedChangeListener((btn, checked) -> {
                s.present = checked;
                h.tvStatus.setText(checked ? "Présent" : "Absent");
                h.tvStatus.setTextColor(getResources().getColor(checked ? R.color.green : R.color.red));
                h.itemView.setAlpha(checked ? 1f : 0.72f);
                updateStats();
            });
            h.itemView.setOnClickListener(v -> h.cbPresent.setChecked(!s.present));
        }
        @Override
        public int getItemCount() { return students.size(); }
    }

    // ── onCreate ──────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_attendance);

        courseId   = getIntent().getStringExtra("course_id");
        courseName = getIntent().getStringExtra("course_name");
        courseCode = getIntent().getStringExtra("course_code");
        level      = getIntent().getStringExtra("level");

        tvHeaderCourse   = findViewById(R.id.tv_header_course);
        tvHeaderCode     = findViewById(R.id.tv_header_code);
        tvHeaderLevel    = findViewById(R.id.tv_header_level);
        tvStudentCount   = findViewById(R.id.tv_student_count);
        tvPresentCount   = findViewById(R.id.tv_present_count);
        tvAbsentCount    = findViewById(R.id.tv_absent_count);
        tvErrorStudents  = findViewById(R.id.tv_error_students);
        tvResultTitle    = findViewById(R.id.tv_result_title);
        tvResultMsg      = findViewById(R.id.tv_result_msg);
        rvStudents       = findViewById(R.id.rv_students);
        progressStudents = findViewById(R.id.progress_students);
        btnSelectAll     = findViewById(R.id.btn_select_all);
        btnUnselectAll   = findViewById(R.id.btn_unselect_all);
        btnSubmit        = findViewById(R.id.btn_submit);
        btnBack          = findViewById(R.id.btn_back);
        panelList        = findViewById(R.id.panel_list);
        panelResult      = findViewById(R.id.panel_result);
        overlaySubmitting= findViewById(R.id.overlay_submitting);
        cardSummary      = findViewById(R.id.card_summary);
        tableResult      = findViewById(R.id.table_result);

        tvHeaderCourse.setText(courseName);
        tvHeaderCode.setText(courseCode);
        tvHeaderLevel.setText("Niveau : " + level);

        studentAdapter = new StudentAdapter();
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);

        btnSelectAll.setOnClickListener(v -> {
            for (Student s : students) s.present = true;
            studentAdapter.notifyDataSetChanged();
            updateStats();
        });

        btnUnselectAll.setOnClickListener(v -> {
            for (Student s : students) s.present = false;
            studentAdapter.notifyDataSetChanged();
            updateStats();
        });

        btnSubmit.setOnClickListener(v -> {
            int total   = students.size();
            int present = getPresentCount();
            int absent  = total - present;

            new AlertDialog.Builder(this)
                    .setTitle("Confirmer la soumission")
                    .setMessage("Cours : " + courseName + "\nNiveau : " + level +
                            "\n\n✅ Présents : " + present + "\n❌ Absents : " + absent +
                            "\n\nSoumettre les présences ?")
                    .setPositiveButton("Soumettre", (d, w) -> {
                        overlaySubmitting.setVisibility(View.VISIBLE);
                        btnSubmit.setEnabled(false);

                        // Build presents array — only IDs of present students
                        List<Integer> presentIds = new ArrayList<>();
                        for (Student s : students) {
                            if (s.present) {
                                presentIds.add(Integer.parseInt(s.id));
                            }
                        }

                        // Today's date formatted as yyyy-MM-dd
                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                        // POST body: { "cours_id": 1, "date_presence": "2026-03-10", "presents": [1, 2, 3] }
                        AttendanceRequest req = new AttendanceRequest(
                                Integer.parseInt(courseId),
                                today,
                                presentIds
                        );

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

                        api.submitAttendance(req).enqueue(new Callback<AttendanceResponse>() {
                            @Override
                            public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {
                                overlaySubmitting.setVisibility(View.GONE);
                                String msg = (response.body() != null && response.body().message != null)
                                        ? response.body().message : "Présences enregistrées.";
                                showResultTable(response.isSuccessful(), msg);
                            }
                            @Override
                            public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                                overlaySubmitting.setVisibility(View.GONE);
                                showResultTable(false, "Erreur réseau : " + t.getMessage());
                            }
                        });
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        btnBack.setOnClickListener(v -> finish());

        // Load students from API
        progressStudents.setVisibility(View.VISIBLE);
        tvErrorStudents.setVisibility(View.GONE);

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

        api.getStudents().enqueue(new Callback<StudentResponse>() {
            @Override
            public void onResponse(Call<StudentResponse> call, Response<StudentResponse> response) {
                progressStudents.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    for (Student s : response.body().etudiants) {
                        if (s.niveau.equalsIgnoreCase(level)) students.add(s);
                    }
                    if (students.isEmpty()) {
                        tvErrorStudents.setText("Aucun étudiant inscrit en " + level);
                        tvErrorStudents.setVisibility(View.VISIBLE);
                        btnSubmit.setEnabled(false);
                    } else {
                        studentAdapter.notifyDataSetChanged();
                        updateStats();
                    }
                } else {
                    tvErrorStudents.setText("Erreur lors du chargement des étudiants.");
                    tvErrorStudents.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<StudentResponse> call, Throwable t) {
                progressStudents.setVisibility(View.GONE);
                tvErrorStudents.setText("Connexion échouée : " + t.getMessage());
                tvErrorStudents.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────

    int getPresentCount() {
        int n = 0;
        for (Student s : students) if (s.present) n++;
        return n;
    }

    void updateStats() {
        int total   = students.size();
        int present = getPresentCount();
        int absent  = total - present;
        tvStudentCount.setText(total   + " étudiant(s)");
        tvPresentCount.setText(present + " présent(s)");
        tvAbsentCount .setText(absent  + " absent(s)");
    }

    void showResultTable(boolean success, String serverMessage) {
        panelList  .setVisibility(View.GONE);
        panelResult.setVisibility(View.VISIBLE);

        cardSummary.setCardBackgroundColor(getResources().getColor(
                success ? R.color.green_bg : R.color.red_bg));
        tvResultTitle.setText(success ? "✅ Soumission réussie !" : "⚠️ Soumission échouée");
        tvResultTitle.setTextColor(getResources().getColor(success ? R.color.green : R.color.red));

        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        tvResultMsg.setText(serverMessage + "\n📅 " + date);

        tableResult.removeAllViews();

        // Header row
        String[] headers = {"#", "Nom & Prénom", "Niveau", "Statut", "ID"};
        int[]    weights = { 1,   4,              2,        2,        1  };
        TableRow header = new TableRow(this);
        header.setBackgroundColor(getResources().getColor(R.color.primary));
        for (int j = 0; j < headers.length; j++) {
            TextView tv = new TextView(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weights[j]);
            lp.setMargins(2, 2, 2, 2);
            tv.setLayoutParams(lp);
            tv.setText(headers[j]);
            tv.setPadding(10, 10, 10, 10);
            tv.setTextSize(12f);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTypeface(null, Typeface.BOLD);
            header.addView(tv);
        }
        tableResult.addView(header);

        // Data rows
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            TableRow row = new TableRow(this);
            row.setBackgroundColor(getResources().getColor(
                    i % 2 == 0 ? R.color.row_even : R.color.row_odd));

            String[] cols = {String.valueOf(i + 1), s.getFullName(), s.niveau,
                    s.present ? "Présent" : "Absent", s.id};
            for (int j = 0; j < cols.length; j++) {
                TextView tv = new TextView(this);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weights[j]);
                lp.setMargins(2, 2, 2, 2);
                tv.setLayoutParams(lp);
                tv.setText(cols[j]);
                tv.setPadding(10, 10, 10, 10);
                tv.setTextSize(12f);
                if (j == 3) {
                    tv.setTextColor(getResources().getColor(s.present ? R.color.green : R.color.red));
                    tv.setTypeface(null, Typeface.BOLD);
                } else {
                    tv.setTextColor(0xFF1A1A2E);
                }
                row.addView(tv);
            }
            tableResult.addView(row);
        }

        // Summary row
        int present = getPresentCount();
        int absent  = students.size() - present;
        TableRow summary = new TableRow(this);
        summary.setBackgroundColor(getResources().getColor(R.color.primary));
        TextView tvSum = new TextView(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        lp.span = 5;
        tvSum.setLayoutParams(lp);
        tvSum.setText("Total : " + students.size() + "   |   ✅ " + present + "   |   ❌ " + absent);
        tvSum.setPadding(16, 12, 16, 12);
        tvSum.setTextColor(0xFFFFFFFF);
        tvSum.setTypeface(null, Typeface.BOLD);
        tvSum.setTextSize(13f);
        summary.addView(tvSum);
        tableResult.addView(summary);
    }
}

