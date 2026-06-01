package com.example.veterinaria;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Inicio extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    private static final int MAX_INTENTOS = 3;
    private static final long TIEMPO_BLOQUEO_MS = 5 * 60 * 1000; // 5 minutos de bloqueo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        Button btnPrimeraVez = findViewById(R.id.btnPrimeraVez);
        btnPrimeraVez.setOnClickListener(v -> {
            startActivity(new Intent(Inicio.this, CrearPass.class));
        });
    }

    //--------------
    //---MÉTODOS----
    //--------------

    //INICIAR SESIÓN
    public void iniciarSesion(View view) {

        EditText etUsuario = findViewById(R.id.etUsuario);
        EditText etPassword = findViewById(R.id.etPassword);

        String email = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introduce usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        //Cogemos la hora actual
        long ahora = System.currentTimeMillis();
        long bloqueadoHasta = prefs.getLong("bloqueado_hasta", 0);

        // Cuenta bloqueada--> si el tiempo actual (desde 1970) es menor que lo guardado en preferences
        if (ahora < bloqueadoHasta) {
            long segundos = (bloqueadoHasta - ahora) / 1000;
            Toast.makeText(
                    this,
                    "Cuenta bloqueada. Inténtalo en " + segundos + " segundos",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    // Resetear intentos
                    prefs.edit()
                            .remove("intentos_fallidos")
                            .remove("bloqueado_hasta")
                            .apply();

                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // Comprobar tipo de usuario en Firestore
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {

                                String tipo = doc.getString("tipo");
                                String uid1 = FirebaseAuth.getInstance().getCurrentUser().getUid();

//y si la empresa contrata mas veterinarios?? debo de poder como veterinario crear un usuario VETERINARIO.
                                if ("MASTER".equals(tipo)) {
                                    startActivity(new Intent(this, PanelVeterinario.class));
                                } else {
                                    startActivity(new Intent(this, Mascotas_usuario.class));
                                }

                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                        "ERROR Firestore: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    //número de intentos fallidos

                    int intentos = prefs.getInt("intentos_fallidos", 0);
                    intentos++;

                    //el editor nos permite modificar el sharedpreferences
                    SharedPreferences.Editor editor = prefs.edit();

                    if (intentos >= MAX_INTENTOS) {
                        long bloquearHasta = System.currentTimeMillis() + TIEMPO_BLOQUEO_MS;
                        editor.putLong("bloqueado_hasta", bloquearHasta);
                        editor.remove("intentos_fallidos");
                        editor.apply();

                        Toast.makeText(
                                this,
                                "Has fallado 3 veces.\nCuenta bloqueada durante 5 minutos",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        editor.putInt("intentos_fallidos", intentos);
                        editor.apply();

                        Toast.makeText(
                                this,
                                "Contraseña incorrecta (" + intentos + " de " + MAX_INTENTOS + ")",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
