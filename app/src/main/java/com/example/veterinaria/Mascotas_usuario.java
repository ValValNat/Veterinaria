package com.example.veterinaria;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Mascotas_usuario extends AppCompatActivity {

    LinearLayout layoutMascotas;
    FirebaseFirestore db;
    FirebaseAuth auth;

    // Números de WhatsApp (sin + ni espacios)
    private static final String WA_CITA       = "34722610920";
    private static final String WA_EMERGENCIA = "34698745348";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mascotas_usuario);

        layoutMascotas = findViewById(R.id.layoutMascotas);
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        findViewById(R.id.btnContactar).setOnClickListener(
                v -> mostrarPopupContacto());

        cargarMascotasDelUsuario();
    }

    // ─────────────────────────────────────────
    // POPUP CONTACTO
    // ─────────────────────────────────────────
    private void mostrarPopupContacto() {
        new AlertDialog.Builder(this)
                .setTitle("¿En qué podemos ayudarte?")
                .setMessage("Selecciona el motivo de contacto:")
                .setPositiveButton("📅 Pedir cita", (d, w) ->
                        abrirWhatsApp(WA_CITA,
                                "Hola, me gustaría pedir una cita en PetCare."))
                .setNegativeButton("🚨 Emergencias", (d, w) ->
                        abrirWhatsApp(WA_EMERGENCIA,
                                "Hola, tengo una emergencia con mi mascota."))
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void abrirWhatsApp(String numero, String mensaje) {
        try {
            String url = "https://wa.me/" + numero
                    + "?text=" + Uri.encode(mensaje);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo abrir WhatsApp",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────
    // CARGAR MASCOTAS
    // ─────────────────────────────────────────
    private void cargarMascotasDelUsuario() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "No hay usuario logueado",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String ownerUid = auth.getCurrentUser().getUid();

        db.collection("mascotas")
                .whereEqualTo("ownerId", ownerUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    layoutMascotas.removeAllViews();

                    if (snapshot.isEmpty()) {
                        Toast.makeText(this,
                                "No tienes mascotas registradas",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        crearBotonMascota(
                                doc.getId(),
                                doc.getString("nombre"),
                                doc.getString("especie"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────
    // BOTÓN POR MASCOTA
    // ─────────────────────────────────────────
    private void crearBotonMascota(String idMascota,
                                   String nombre, String especie) {
        Button boton = new Button(this);
        boton.setText("🐾  " + nombre + "  (" + especie + ")");
        boton.setTag(idMascota);
        boton.setAllCaps(false);
        boton.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.teal_700));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        boton.setLayoutParams(params);
        boton.setOnClickListener(this::ver_mascotas);
        layoutMascotas.addView(boton);
    }

    // ─────────────────────────────────────────
    // VER DETALLE MASCOTA
    // ─────────────────────────────────────────
    public void ver_mascotas(View view) {
        String idMascota = (String) view.getTag();
        Intent intent = new Intent(this, DetalleMascota.class);
        intent.putExtra("idMascota", idMascota);
        startActivity(intent);
    }
}