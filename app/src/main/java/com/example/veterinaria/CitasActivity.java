package com.example.veterinaria;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CitasActivity extends AppCompatActivity {

    private LinearLayout layoutCitas;
    private TextView tvVacio;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citas);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        layoutCitas = findViewById(R.id.layoutCitas);
        tvVacio     = findViewById(R.id.tvVacio);

        cargarCitas();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarCitas();
    }

    // ─────────────────────────────────────────
    // CARGAR CITAS DESDE FIRESTORE
    // ─────────────────────────────────────────
    private void cargarCitas() {
        db.collection("citas").get()
                .addOnSuccessListener(snapshot -> {
                    layoutCitas.removeAllViews();

                    if (snapshot.isEmpty()) {
                        tvVacio.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvVacio.setVisibility(View.GONE);

                    // Recoger y ordenar por fecha + hora
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm",
                            Locale.getDefault());

                    docs.sort((a, b) -> {
                        try {
                            String fa = getStr(a, "fecha") + " " + getStr(a, "hora");
                            String fb = getStr(b, "fecha") + " " + getStr(b, "hora");
                            Date da = sdf.parse(fa);
                            Date db2 = sdf.parse(fb);
                            if (da != null && db2 != null) return da.compareTo(db2);
                        } catch (ParseException ignored) {}
                        return 0;
                    });

                    // Agrupar por fecha
                    String fechaAnterior = "";
                    for (DocumentSnapshot doc : docs) {
                        String fecha    = getStr(doc, "fecha");
                        String hora     = getStr(doc, "hora");
                        String paciente = getStr(doc, "pacienteNombre");
                        String cliente  = getStr(doc, "clienteEmail");
                        String motivo   = getStr(doc, "motivo");
                        String citaId   = doc.getId();

                        // Cabecera de fecha si cambia
                        if (!fecha.equals(fechaAnterior)) {
                            fechaAnterior = fecha;
                            TextView tvFecha = new TextView(this);
                            tvFecha.setText("📆 " + fecha);
                            tvFecha.setTextColor(0xFF4E342E);
                            tvFecha.setTextSize(16f);
                            tvFecha.setTypeface(null, android.graphics.Typeface.BOLD);
                            LinearLayout.LayoutParams pf = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            pf.setMargins(0, 20, 0, 6);
                            tvFecha.setLayoutParams(pf);
                            layoutCitas.addView(tvFecha);
                        }

                        // Tarjeta de cita
                        LinearLayout tarjeta = new LinearLayout(this);
                        tarjeta.setOrientation(LinearLayout.VERTICAL);
                        tarjeta.setBackgroundColor(0xFFFFECB3);
                        tarjeta.setPadding(20, 16, 20, 16);
                        LinearLayout.LayoutParams pt = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        pt.setMargins(0, 0, 0, 10);
                        tarjeta.setLayoutParams(pt);

                        // Fila hora + paciente
                        LinearLayout filaTop = new LinearLayout(this);
                        filaTop.setOrientation(LinearLayout.HORIZONTAL);

                        TextView tvHora = new TextView(this);
                        tvHora.setText("🕐 " + hora);
                        tvHora.setTextColor(0xFFFBC02D);
                        tvHora.setTextSize(15f);
                        tvHora.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvHora.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        filaTop.addView(tvHora);

                        TextView tvSep = new TextView(this);
                        tvSep.setText("  ·  ");
                        tvSep.setTextColor(0xFF9E9E9E);
                        tvSep.setTextSize(15f);
                        filaTop.addView(tvSep);

                        TextView tvPaciente = new TextView(this);
                        tvPaciente.setText("🐾 " + paciente);
                        tvPaciente.setTextColor(0xFF4E342E);
                        tvPaciente.setTextSize(15f);
                        tvPaciente.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvPaciente.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        filaTop.addView(tvPaciente);

                        tarjeta.addView(filaTop);

                        // Cliente
                        TextView tvCliente = new TextView(this);
                        tvCliente.setText("👤 " + cliente);
                        tvCliente.setTextColor(0xFF6D4C41);
                        tvCliente.setTextSize(13f);
                        LinearLayout.LayoutParams pc = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        pc.setMargins(0, 6, 0, 0);
                        tvCliente.setLayoutParams(pc);
                        tarjeta.addView(tvCliente);

                        // Motivo
                        TextView tvMotivo = new TextView(this);
                        tvMotivo.setText("📋 " + motivo);
                        tvMotivo.setTextColor(0xFF6D4C41);
                        tvMotivo.setTextSize(13f);
                        LinearLayout.LayoutParams pm = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        pm.setMargins(0, 4, 0, 8);
                        tvMotivo.setLayoutParams(pm);
                        tarjeta.addView(tvMotivo);

                        // Botón eliminar cita
                        Button btnEliminar = new Button(this);
                        btnEliminar.setText("🗑️ Cancelar cita");
                        btnEliminar.setAllCaps(false);
                        btnEliminar.setTextColor(0xFFFFFFFF);
                        btnEliminar.setBackgroundColor(0xFFE53935);
                        btnEliminar.setTextSize(13f);
                        btnEliminar.setOnClickListener(v ->
                                confirmarEliminarCita(citaId));
                        tarjeta.addView(btnEliminar);

                        layoutCitas.addView(tarjeta);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────
    // ELIMINAR CITA
    // ─────────────────────────────────────────
    private void confirmarEliminarCita(String citaId) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Cancelar cita")
                .setMessage("¿Estás seguro de que deseas cancelar esta cita?")
                .setPositiveButton("Sí, cancelar", (d, w) ->
                        db.collection("citas").document(citaId).delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Cita cancelada",
                                            Toast.LENGTH_SHORT).show();
                                    cargarCitas();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .setNegativeButton("No", null)
                .show();
    }

    // ─────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────
    private String getStr(DocumentSnapshot doc, String key) {
        String val = doc.getString(key);
        return val != null ? val : "-";
    }
}