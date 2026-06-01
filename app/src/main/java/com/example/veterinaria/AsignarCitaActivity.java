package com.example.veterinaria;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;

public class AsignarCitaActivity extends AppCompatActivity {

    private Spinner spinnerPaciente, spinnerHora;
    private EditText etCliente, etFecha, etMotivo;
    private Button btnSeleccionarFecha, btnGuardarCita;
    private FirebaseFirestore db;

    // Datos de mascotas cargados desde Firestore
    private final List<String> nombresMascotas = new ArrayList<>();
    private final List<String> idsMascotas     = new ArrayList<>();
    private final List<String> emailsDuenos    = new ArrayList<>();
    private ArrayAdapter<String> adapterMascotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asignar_cita);

        db = FirebaseFirestore.getInstance();

        spinnerPaciente    = findViewById(R.id.spinnerPaciente);
        spinnerHora        = findViewById(R.id.spinnerHora);
        etCliente          = findViewById(R.id.etCliente);
        etFecha            = findViewById(R.id.etFecha);
        etMotivo           = findViewById(R.id.etMotivo);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnGuardarCita     = findViewById(R.id.btnGuardarCita);

        configurarSpinnerHoras();
        cargarMascotas();

        btnSeleccionarFecha.setOnClickListener(v -> abrirCalendario());
        btnGuardarCita.setOnClickListener(v -> guardarCita());
    }

    // -------------------------------------------------------
    // SPINNER HORAS  (9:00 – 17:00 cada 30 minutos)
    // -------------------------------------------------------
    private void configurarSpinnerHoras() {
        List<String> horas = new ArrayList<>();
        for (int h = 9; h <= 17; h++) {
            horas.add(String.format(Locale.getDefault(), "%02d:00", h));
            if (h < 17) {
                horas.add(String.format(Locale.getDefault(), "%02d:30", h));
            }
        }
        ArrayAdapter<String> adapterHoras = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, horas);
        adapterHoras.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerHora.setAdapter(adapterHoras);
    }

    // -------------------------------------------------------
    // CARGAR MASCOTAS DESDE FIRESTORE
    // -------------------------------------------------------
    private void cargarMascotas() {
        adapterMascotas = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, nombresMascotas);
        adapterMascotas.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerPaciente.setAdapter(adapterMascotas);

        db.collection("mascotas")
                .get()
                .addOnSuccessListener(snapshot -> {
                    nombresMascotas.clear();
                    idsMascotas.clear();
                    emailsDuenos.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String nombre  = doc.getString("nombre");
                        String ownerId = doc.getString("ownerId");
                        if (nombre != null) {
                            nombresMascotas.add(nombre);
                            idsMascotas.add(doc.getId());
                            emailsDuenos.add(ownerId != null ? ownerId : "");
                        }
                    }
                    adapterMascotas.notifyDataSetChanged();

                    // Al seleccionar mascota → buscar email del dueño
                    spinnerPaciente.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent,
                                                           android.view.View view, int position, long id) {
                                    String ownerId = emailsDuenos.get(position);
                                    buscarEmailDueno(ownerId);
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error cargando mascotas: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // -------------------------------------------------------
    // BUSCAR EMAIL DEL DUEÑO EN LA COLECCIÓN users
    // -------------------------------------------------------
    private void buscarEmailDueno(String uid) {
        if (uid == null || uid.isEmpty()) {
            etCliente.setText("");
            return;
        }
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String email = doc.getString("email");
                    etCliente.setText(email != null ? email : uid);
                })
                .addOnFailureListener(e -> etCliente.setText(uid));
    }

    // -------------------------------------------------------
    // CALENDARIO
    // -------------------------------------------------------
    private void abrirCalendario() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) ->
                        etFecha.setText(day + "/" + (month + 1) + "/" + year),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // -------------------------------------------------------
    // GUARDAR CITA EN FIRESTORE
    // -------------------------------------------------------
    private void guardarCita() {
        if (nombresMascotas.isEmpty()) {
            Toast.makeText(this, "No hay mascotas disponibles",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerPaciente.getSelectedItemPosition();
        String paciente = nombresMascotas.get(pos);
        String mascotaId = idsMascotas.get(pos);
        String cliente  = etCliente.getText().toString().trim();
        String fecha    = etFecha.getText().toString().trim();
        String hora     = spinnerHora.getSelectedItem().toString();
        String motivo   = etMotivo.getText().toString().trim();

        if (fecha.isEmpty() || motivo.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> cita = new HashMap<>();
        cita.put("pacienteNombre", paciente);
        cita.put("mascotaId",      mascotaId);
        cita.put("clienteEmail",   cliente);
        cita.put("fecha",          fecha);
        cita.put("hora",           hora);
        cita.put("motivo",         motivo);

        db.collection("citas").add(cita)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Cita guardada correctamente",
                            Toast.LENGTH_LONG).show();
                    limpiarCampos();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // -------------------------------------------------------
    // LIMPIAR
    // -------------------------------------------------------
    private void limpiarCampos() {
        etFecha.setText("");
        etMotivo.setText("");
        etCliente.setText("");
        spinnerPaciente.setSelection(0);
        spinnerHora.setSelection(0);
    }
}