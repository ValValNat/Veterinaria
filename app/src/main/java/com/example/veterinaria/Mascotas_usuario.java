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

// Pantalla principal del dueno: muestra sus mascotas y el boton de contacto
public class Mascotas_usuario extends AppCompatActivity {

    LinearLayout layoutMascotas;
    FirebaseFirestore db;
    FirebaseAuth auth;

    // Numeros de WhatsApp sin "+" porque la URL de wa.me lo añade sola
    // Como constantes para poder cambiarlos facilmente en el futuro
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


    // Popup con dos opciones: pedir cita o emergencias
    // Cada opcion abre WhatsApp con un numero y mensaje distintos
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


    // Abre WhatsApp con el numero y el mensaje ya escritos
    // Uri.encode convierte el texto a formato seguro para URL (espacios = %20, etc.)
    private void abrirWhatsApp(String numero, String mensaje) {
        try {
            String url = "https://wa.me/" + numero
                    + "?text=" + Uri.encode(mensaje);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo abrir WhatsApp",
                    Toast.LENGTH_SHORT).show();
        }
    }


    // Carga solo las mascotas del usuario logueado usando su UID como filtro
    private void cargarMascotasDelUsuario() {

        // getCurrentUser() devuelve null si no hay sesion iniciada
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "No hay usuario logueado",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String ownerUid = auth.getCurrentUser().getUid();

        // whereEqualTo filtra los documentos donde ownerId == UID del usuario actual
        db.collection("mascotas")
                .whereEqualTo("ownerId", ownerUid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    // Limpiamos antes de añadir para evitar botones duplicados
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


    // Crea un boton para una mascota y lo añade al contenedor
    private void crearBotonMascota(String idMascota,
                                   String nombre, String especie) {
        Button boton = new Button(this);
        boton.setText("🐾  " + nombre + "  (" + especie + ")");

        // setTag guarda el ID en el boton para recuperarlo cuando se pulse
        boton.setTag(idMascota);

        // Sin este flag Android pondria el texto en MAYUSCULAS por defecto
        boton.setAllCaps(false);

        boton.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.teal_700));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        boton.setLayoutParams(params);

        // this::ver_mascotas es equivalente a v -> ver_mascotas(v)
        boton.setOnClickListener(this::ver_mascotas);

        layoutMascotas.addView(boton);
    }


    // Se ejecuta al pulsar un boton de mascota
    // Recupera el ID guardado en el tag y abre la pantalla de detalle
    public void ver_mascotas(View view) {

        // getTag() devuelve Object, el cast a String es necesario para usarlo
        String idMascota = (String) view.getTag();

        Intent intent = new Intent(this, DetalleMascota.class);
        intent.putExtra("idMascota", idMascota);
        startActivity(intent);
    }
}