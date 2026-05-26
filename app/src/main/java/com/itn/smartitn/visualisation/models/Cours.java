package com.itn.smartitn.visualisation.models;


import com.google.gson.annotations.SerializedName;

public class Cours {

    @SerializedName("id")
    private int id;

    @SerializedName("nom_cours")
    private String nomCours;

    @SerializedName("code_cours")
    private String codeCours;

    @SerializedName("description")
    private String description;

    public int getId() { return id; }
    public String getNomCours() { return nomCours; }
    public String getCodeCours() { return codeCours; }
    public String getDescription() { return description; }
}
