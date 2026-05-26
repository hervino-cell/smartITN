package com.itn.smartitn.visualisation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itn.smartitn.R;
import com.itn.smartitn.visualisation.models.Cours;

import java.util.List;

public class CoursAdapter extends RecyclerView.Adapter<CoursAdapter.CoursViewHolder> {

    private List<Cours> coursList;
    private OnCoursClickListener listener;

    public interface OnCoursClickListener {
        void onCoursClick(Cours cours);
    }

    public CoursAdapter(List<Cours> coursList, OnCoursClickListener listener) {
        this.coursList = coursList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CoursViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.visualisation_item_cours, parent, false);
        return new CoursViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoursViewHolder holder, int position) {
        Cours cours = coursList.get(position);
        holder.tvNomCours.setText(cours.getNomCours());
        holder.tvCodeCours.setText("Code : " + cours.getCodeCours());
        holder.tvDescription.setText(cours.getDescription());

        holder.itemView.setOnClickListener(v -> listener.onCoursClick(cours));
    }

    @Override
    public int getItemCount() { return coursList.size(); }

    public static class CoursViewHolder extends RecyclerView.ViewHolder {
        TextView tvNomCours, tvCodeCours, tvDescription;

        public CoursViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomCours    = itemView.findViewById(R.id.tvNomCours);
            tvCodeCours   = itemView.findViewById(R.id.tvCodeCours);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
