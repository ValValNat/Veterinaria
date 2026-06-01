package com.example.veterinaria;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PanelVeterinario extends AppCompatActivity {

    Button btnIrUsuarios, btnPacientes, btnAsignarCita;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_veterinario);

        btnIrUsuarios       = findViewById(R.id.btnIrUsuarios);
        btnPacientes        = findViewById(R.id.btnPacientes);
        btnAsignarCita      = findViewById(R.id.btnAsignarCita);

        btnIrUsuarios.setOnClickListener(v ->
                startActivity(new Intent(this, VeterinarioListado.class)));

        btnPacientes.setOnClickListener(v ->
                startActivity(new Intent(this, PacientesActivity.class)));


        btnAsignarCita.setOnClickListener(v ->
                startActivity(new Intent(this, AsignarCitaActivity.class)));
    }
}