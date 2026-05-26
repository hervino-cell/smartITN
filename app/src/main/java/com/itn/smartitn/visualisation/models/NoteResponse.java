package com.itn.smartitn.visualisation.models;

import com.google.gson.annotations.SerializedName;

public class NoteResponse {

    @SerializedName("status")
    private boolean status;

    @SerializedName("cours")
    private String cours;

    @SerializedName("code_cours")
    private String codeCours;

    @SerializedName("note_tp")
    private double noteTP;

    @SerializedName("note_examen")
    private double noteExamen;

    @SerializedName("moyenne")
    private double moyenne;

    @SerializedName("appreciation")
    private String appreciation;

    @SerializedName("message")
    private String message;

    public boolean isStatus() { return status; }
    public String getCours() { return cours; }
    public String getCodeCours() { return codeCours; }
    public double getNoteTP() { return noteTP; }
    public double getNoteExamen() { return noteExamen; }
    public double getMoyenne() { return moyenne; }
    public String getAppreciation() { return appreciation; }
    public String getMessage() { return message; }
}
