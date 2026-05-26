package com.itn.smartitn.visualisation.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itn.smartitn.R;
import com.itn.smartitn.visualisation.models.Note;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notesList;

    public NotesAdapter(List<Note> notesList) {
        this.notesList = notesList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.visualisation_item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notesList.get(position);

        holder.tvEtudiantNom.setText(note.getEtudiantNom());
        holder.tvNoteTP.setText(String.format("TP : %.1f / 20", note.getNoteTP()));
        holder.tvNoteExamen.setText(String.format("Examen : %.1f / 20", note.getNoteExamen()));
        holder.tvMoyenne.setText(String.format("Moy : %.2f / 20", note.getMoyenne()));
        holder.tvMention.setText(note.getMention());

        // Couleur selon la mention
        double moy = note.getMoyenne();
        if (moy >= 14) {
            holder.tvMention.setTextColor(Color.parseColor("#27AE60")); // vert
        } else if (moy >= 10) {
            holder.tvMention.setTextColor(Color.parseColor("#F39C12")); // orange
        } else {
            holder.tvMention.setTextColor(Color.parseColor("#E74C3C")); // rouge
        }
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvEtudiantNom, tvNoteTP, tvNoteExamen, tvMoyenne, tvMention;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEtudiantNom = itemView.findViewById(R.id.tvEtudiantNom);
            tvNoteTP      = itemView.findViewById(R.id.tvNoteTP);
            tvNoteExamen  = itemView.findViewById(R.id.tvNoteExamen);
            tvMoyenne     = itemView.findViewById(R.id.tvMoyenne);
            tvMention     = itemView.findViewById(R.id.tvMention);
        }
    }
}
