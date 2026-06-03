package com.example.veterinaria;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

public class CitasActivity extends AppCompatActivity {

    private LinearLayout layoutCitas;
    private TextView tvVacio;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static final String[] HORAS = buildHoras();

    private static String[] buildHoras() {
        List<String> lista = new ArrayList<>();
        for (int h = 9; h <= 17; h++) {
            lista.add(String.format(Locale.getDefault(), "%02d:00", h));
            if (h < 17) lista.add(String.format(Locale.getDefault(), "%02d:30", h));
        }
        return lista.toArray(new String[0]);
    }

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
    // CARGAR CITAS
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

                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm",
                            Locale.getDefault());

                    docs.sort((a, b) -> {
                        try {
                            Date da  = sdf.parse(getStr(a, "fecha") + " " + getStr(a, "hora"));
                            Date db2 = sdf.parse(getStr(b, "fecha") + " " + getStr(b, "hora"));
                            if (da != null && db2 != null) return da.compareTo(db2);
                        } catch (ParseException ignored) {}
                        return 0;
                    });

                    String fechaAnterior = "";
                    for (DocumentSnapshot doc : docs) {
                        String fecha    = getStr(doc, "fecha");
                        String hora     = getStr(doc, "hora");
                        String paciente = getStr(doc, "pacienteNombre");
                        String cliente  = getStr(doc, "clienteEmail");
                        String motivo   = getStr(doc, "motivo");
                        String citaId   = doc.getId();

                        // Cabecera de fecha
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

                        // Tarjeta
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
                        filaTop.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));

                        TextView tvHora = new TextView(this);
                        tvHora.setText("🕐 " + hora);
                        tvHora.setTextColor(0xFFFBC02D);
                        tvHora.setTextSize(15f);
                        tvHora.setTypeface(null, android.graphics.Typeface.BOLD);
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
                        pm.setMargins(0, 4, 0, 12);
                        tvMotivo.setLayoutParams(pm);
                        tarjeta.addView(tvMotivo);

                        // Fila botones
                        LinearLayout filaBotones = new LinearLayout(this);
                        filaBotones.setOrientation(LinearLayout.HORIZONTAL);
                        filaBotones.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));

                        // Botón editar
                        Button btnEditar = new Button(this);
                        btnEditar.setText("✏️ Editar");
                        btnEditar.setAllCaps(false);
                        btnEditar.setTextColor(0xFFFFFFFF);
                        btnEditar.setBackgroundColor(0xFFFBC02D);
                        btnEditar.setTextSize(13f);
                        LinearLayout.LayoutParams pEdit = new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        pEdit.setMargins(0, 0, 6, 0);
                        btnEditar.setLayoutParams(pEdit);
                        btnEditar.setOnClickListener(v ->
                                mostrarDialogEditar(citaId, fecha, hora, motivo));
                        filaBotones.addView(btnEditar);

                        // Botón eliminar
                        Button btnEliminar = new Button(this);
                        btnEliminar.setText("🗑️ Cancelar");
                        btnEliminar.setAllCaps(false);
                        btnEliminar.setTextColor(0xFFFFFFFF);
                        btnEliminar.setBackgroundColor(0xFFE53935);
                        btnEliminar.setTextSize(13f);
                        btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        btnEliminar.setOnClickListener(v ->
                                confirmarEliminarCita(citaId));
                        filaBotones.addView(btnEliminar);

                        tarjeta.addView(filaBotones);
                        layoutCitas.addView(tarjeta);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────
    // DIALOG EDITAR CITA
    // ─────────────────────────────────────────
    private void mostrarDialogEditar(String citaId, String fechaActual,
                                     String horaActual, String motivoActual) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 32, 48, 32);

        // Fecha
        form.addView(labelEdit("Fecha"));
        EditText etFecha = new EditText(this);
        etFecha.setText(fechaActual);
        etFecha.setFocusable(false);
        etFecha.setClickable(true);
        etFecha.setHint("Selecciona fecha");
        etFecha.setTextColor(0xFF4E342E);
        etFecha.setBackgroundColor(0xFFFFECB3);
        etFecha.setPadding(16, 12, 16, 12);
        etFecha.setInputType(InputType.TYPE_NULL);
        form.addView(etFecha);

        etFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) ->
                            etFecha.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Hora
        form.addView(labelEdit("Hora"));
        Spinner spHora = new Spinner(this);
        ArrayAdapter<String> adHora = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, HORAS);
        adHora.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHora.setAdapter(adHora);
        for (int i = 0; i < HORAS.length; i++)
            if (HORAS[i].equals(horaActual)) { spHora.setSelection(i); break; }
        form.addView(spHora);

        // Motivo
        form.addView(labelEdit("Motivo"));
        EditText etMotivo = new EditText(this);
        etMotivo.setText(motivoActual);
        etMotivo.setTextColor(0xFF4E342E);
        etMotivo.setBackgroundColor(0xFFFFECB3);
        etMotivo.setPadding(16, 12, 16, 12);
        etMotivo.setInputType(InputType.TYPE_CLASS_TEXT);
        form.addView(etMotivo);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("✏️ Editar cita")
                .setView(form)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevaFecha  = etFecha.getText().toString().trim();
            String nuevaHora   = spHora.getSelectedItem().toString();
            String nuevoMotivo = etMotivo.getText().toString().trim();

            if (nuevaFecha.isEmpty() || nuevoMotivo.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> update = new HashMap<>();
            update.put("fecha",  nuevaFecha);
            update.put("hora",   nuevaHora);
            update.put("motivo", nuevoMotivo);

            db.collection("citas").document(citaId)
                    .update(update)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Cita actualizada ✔",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarCitas();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        });
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
    // HELPERS
    // ─────────────────────────────────────────
    private String getStr(DocumentSnapshot doc, String key) {
        String val = doc.getString(key);
        return val != null ? val : "-";
    }

    private TextView labelEdit(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(0xFF4E342E);
        tv.setTextSize(13f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 16, 0, 4);
        tv.setLayoutParams(p);
        return tv;
    }
}