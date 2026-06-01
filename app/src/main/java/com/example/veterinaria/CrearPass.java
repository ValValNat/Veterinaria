package com.example.veterinaria;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CrearPass extends AppCompatActivity {

    private EditText etNombre, etPrefijo, etTelefono, etDni,
            etEmail, etNuevaPassword, etRepetirPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        etNombre          = findViewById(R.id.etNombre);
        etPrefijo         = findViewById(R.id.etPrefijo);
        etTelefono        = findViewById(R.id.etTelefono);
        etDni             = findViewById(R.id.etDni);
        etEmail           = findViewById(R.id.etEmail);
        etNuevaPassword   = findViewById(R.id.etNuevaPassword);
        etRepetirPassword = findViewById(R.id.etRepetirPassword);
    }

    // ─────────────────────────────────────────
    // BOTÓN CREAR CUENTA
    // ─────────────────────────────────────────
    public void crearPassword(View view) {

        String nombre   = etNombre.getText().toString().trim();
        String prefijo  = etPrefijo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String dni      = etDni.getText().toString().trim().toUpperCase();
        String email    = etEmail.getText().toString().trim();
        String pass1    = etNuevaPassword.getText().toString();
        String pass2    = etRepetirPassword.getText().toString();

        // ── Validaciones ──────────────────────

        if (nombre.isEmpty() || telefono.isEmpty() || dni.isEmpty()
                || email.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validarDni(dni)) {
            Toast.makeText(this, "DNI no válido (ej: 12345678A)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "El email no tiene un formato válido",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass1.contains(email) || email.contains(pass1)) {
            Toast.makeText(this, "La contraseña no puede ser igual al email",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass1.equals(pass2)) {
            Toast.makeText(this, "Las contraseñas no coinciden",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass1.length() < 6) {
            Toast.makeText(this,
                    "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String telefonoCompleto = (prefijo.isEmpty() ? "+34" : prefijo)
                + telefono;

        // ── Crear en Firebase Auth ─────────────
        mAuth.createUserWithEmailAndPassword(email, pass1)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("email",    email);
                    user.put("nombre",   nombre);
                    user.put("telefono", telefonoCompleto);
                    user.put("dni",      dni);
                    user.put("tipo",     "USER");

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this,
                                        "Cuenta creada correctamente ✔",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Error al guardar datos: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────
    // VALIDAR DNI ESPAÑOL
    // 8 dígitos + 1 letra (tabla oficial)
    // ─────────────────────────────────────────
    private boolean validarDni(String dni) {
        if (!Pattern.matches("[0-9]{8}[A-Z]", dni)) return false;
        String letras = "TRWAGMYFPDXBNJZSQVHLCKE";
        int numero = Integer.parseInt(dni.substring(0, 8));
        char letraEsperada = letras.charAt(numero % 23);
        return dni.charAt(8) == letraEsperada;
    }

    // ─────────────────────────────────────────
    // BOTÓN VOLVER
    // ─────────────────────────────────────────
    public void volver(View view) {
        finish();
    }
}