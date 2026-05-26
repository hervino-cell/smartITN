package com.itn.smartitn.visualisation.models;

import com.google.gson.annotations.SerializedName;

public class Note {

    @SerializedName("id")
    private int id;

    @SerializedName("etudiant_nom")
    private String etudiantNom;

    @SerializedName("cours_id")
    private int coursId;

    @SerializedName("note_tp")
    private double noteTP;

    @SerializedName("note_examen")
    private double noteExamen;

    @SerializedName("date")
    private String date;

    // Constructeur
    public Note(int id, String etudiantNom, int coursId, double noteTP, double noteExamen, String date) {
        this.id = id;
        this.etudiantNom = etudiantNom;
        this.coursId = coursId;
        this.noteTP = noteTP;
        this.noteExamen = noteExamen;
        this.date = date;
    }

    // Getters
    public int getId() { return id; }
    public String getEtudiantNom() { return etudiantNom; }
    public int getCoursId() { return coursId; }
    public double getNoteTP() { return noteTP; }
    public double getNoteExamen() { return noteExamen; }
    public String getDate() { return date; }

    // Calcul automatique de la moyenne
    public double getMoyenne() {
        return (noteTP * 0.4) + (noteExamen * 0.6);
    }

    public String getMention() {
        double moy = getMoyenne();
        if (moy >= 16) return "Très Bien";
        else if (moy >= 14) return "Bien";
        else if (moy >= 12) return "Assez Bien";
        else if (moy >= 10) return "Passable";
        else return "Insuffisant";
    }
}
