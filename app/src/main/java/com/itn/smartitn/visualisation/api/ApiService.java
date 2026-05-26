package com.itn.smartitn.visualisation.api;

import com.itn.smartitn.visualisation.models.CoursResponse;
import com.itn.smartitn.visualisation.models.NoteResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    // GET /API/courses → liste de tous les cours
    @GET("courses")
    Call<CoursResponse> getCours();

    // GET /API/notes/{course_id}/{student_id} → notes d'un étudiant pour un cours
    @GET("notes/{course_id}/{student_id}")
    Call<NoteResponse> getNotes(
            @Path("course_id") int courseId,
            @Path("student_id") int studentId
    );
}
