package com.example.veterinaria;

// Para suprimir una advertencia de Android Studio que explico más abajo
import android.annotation.SuppressLint;

// Para navegar de una pantalla a otra
import android.content.Intent;

// Obligatorio para que la pantalla arranque
import android.os.Bundle;

// El elemento visual botón
import android.widget.Button;

// La base de toda pantalla Android
import androidx.appcompat.app.AppCompatActivity;


// Esta clase es el "Panel Veterinario" el menú principal que ve el veterinario
// después de iniciar sesión, solo tiene cuatro botones, cada uno lleva a una sección
public class PanelVeterinario extends AppCompatActivity {

    // Los cuatro botones del menú
    // Los declaramos aquí arriba (fuera de onCreate) aunque solo se usen en onCreate
    // por coherencia con el resto de clases del proyecto
    Button btnIrUsuarios;  // lleva a la lista de usuarios registrados
    Button btnPacientes;   // lleva a la lista de todas las mascotas
    Button btnVerCitas;    // lleva a la agenda de citas
    Button btnAsignarCita; // lleva al formulario para crear una cita nueva


    // ─────────────────────────────────────────
    // ARRANQUE DE LA PANTALLA
    // ─────────────────────────────────────────

    // @SuppressLint("MissingInflatedId") es una anotación que le dice a Android Studio:
    // "sé lo que estoy haciendo, no me avises de esto"
    // El aviso "MissingInflatedId" aparece cuando Android Studio cree que un
    // findViewById() podría no encontrar el elemento en el XML
    // En este caso es un falso positivo: los IDs sí existen en el XML
    // así que suprimimos el aviso para que no moleste
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargamos el diseño XML de esta pantalla
        setContentView(R.layout.activity_menu_veterinario);

        // Vinculamos cada variable con su botón del XML
        btnIrUsuarios  = findViewById(R.id.btnIrUsuarios);
        btnPacientes   = findViewById(R.id.btnPacientes);
        btnVerCitas    = findViewById(R.id.btnVerCitas);
        btnAsignarCita = findViewById(R.id.btnAsignarCita);

        // Cada botón tiene exactamente el mismo patrón
        // al pulsarlo, creamos un Intent que apunta a la pantalla de destino
        // y lo lanzamos con startActivity()
        // "new Intent(this, Destino.class)" significa
        // "desde esta pantalla, ve a la pantalla Destino".

        // Botón "Usuarios": va a la lista de todos los usuarios registrados
        btnIrUsuarios.setOnClickListener(v ->
                startActivity(new Intent(this, VeterinarioListado.class)));

        // Botón "Pacientes": va a la lista de todas las mascotas
        btnPacientes.setOnClickListener(v ->
                startActivity(new Intent(this, PacientesActivity.class)));

        // Botón "Ver citas": va a la agenda de citas ordenada por fecha
        btnVerCitas.setOnClickListener(v ->
                startActivity(new Intent(this, CitasActivity.class)));

        // Botón "Asignar cita": va al formulario para crear una cita nueva
        btnAsignarCita.setOnClickListener(v ->
                startActivity(new Intent(this, AsignarCitaActivity.class)));
    }
}