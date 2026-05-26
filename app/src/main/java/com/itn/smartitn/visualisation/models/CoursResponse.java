package com.itn.smartitn.visualisation.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CoursResponse {

    @SerializedName("status")
    private boolean status;

    @SerializedName("cours")
    private List<Cours> cours;

    public boolean isStatus() { return status; }
    public List<Cours> getCours() { return cours; }
}
