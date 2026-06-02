package com.example.veterinaria;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

import java.util.*;

public class VeterinarioListado extends AppCompatActivity {

    private EditText etBuscar;
    private LinearLayout layoutUsuarios;
    private FirebaseFirestore db;

    private final List<Map<String, Object>> listaUsuarios = new ArrayList<>();
    private final List<String> idsUsuarios = new ArrayList<>();

    private static final String[] RAZAS_PERRO = {
            "Labrador Retriever", "Golden Retriever", "Pastor Alemán",
            "Bulldog Francés", "Caniche / Poodle", "Yorkshire Terrier",
            "Chihuahua", "Beagle", "Boxer", "Rottweiler", "Dóberman",
            "Shih Tzu", "Schnauzer", "Husky Siberiano", "Bichón Maltés",
            "Cocker Spaniel", "Dálmata", "Pomerania", "Border Collie",
            "Galgo Español", "Otra raza", "Desconocido"
    };
    private static final String[] RAZAS_GATO = {
            "Europeo Común", "Persa", "Siamés", "Maine Coon", "Bengalí",
            "Ragdoll", "British Shorthair", "Abisinio", "Esfinge",
            "Scottish Fold", "Angora", "Ruso Azul", "Noruego del Bosque",
            "Otra raza", "Desconocido"
    };
    private static final String[] VACUNAS_PERRO = {
            "Moquillo canino", "Parvovirus canino",
            "Hepatitis infecciosa canina", "Leptospirosis", "Rabia",
            "Tos de las perreras (Bordetella)", "Leishmaniosis"
    };
    private static final String[] VACUNAS_GATO = {
            "Panleucopenia felina", "Rinotraqueítis viral (Herpesvirus)",
            "Calicivirus felino", "Leucemia felina (FeLV)",
            "Rabia", "Clamidiosis felina"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veterinario_listado);

        db = FirebaseFirestore.getInstance();
        etBuscar = findViewById(R.id.etBuscar);
        layoutUsuarios = findViewById(R.id.layoutUsuarios);

        cargarUsuarios("");

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                cargarUsuarios(s.toString().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ═════════════════════════════════════════
    // CARGAR USUARIOS
    // ═════════════════════════════════════════
    private void cargarUsuarios(String filtro) {
        db.collection("users").get()
                .addOnSuccessListener(snapshot -> {
                    listaUsuarios.clear();
                    idsUsuarios.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String email  = doc.getString("email");
                        String nombre = doc.getString("nombre");
                        String busq   = (email != null ? email : "") + (nombre != null ? nombre : "");
                        if (busq.toLowerCase().contains(filtro)) {
                            listaUsuarios.add(doc.getData());
                            idsUsuarios.add(doc.getId());
                        }
                    }
                    mostrarUsuarios();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ═════════════════════════════════════════
    // MOSTRAR FILAS
    // ═════════════════════════════════════════
    private void mostrarUsuarios() {
        layoutUsuarios.removeAllViews();
        for (int i = 0; i < listaUsuarios.size(); i++) {
            final int idx = i;
            Map<String, Object> usuario = listaUsuarios.get(i);
            String uid   = idsUsuarios.get(i);
            String email = getStr(usuario, "email", "Sin email");
            String nombre = getStr(usuario, "nombre", "");

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setBackgroundColor(0xFFFFECB3);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            fp.setMargins(0, 0, 0, 10);
            fila.setLayoutParams(fp);
            fila.setPadding(20, 16, 16, 16);

            TextView tv = new TextView(this);
            tv.setText(nombre.isEmpty() ? email : nombre + "\n" + email);
            tv.setTextColor(0xFF4E342E);
            tv.setTextSize(14f);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            fila.addView(tv);

            Button btnEditar = new Button(this);
            btnEditar.setText("✏️");
            btnEditar.setBackgroundColor(0xFFFBC02D);
            btnEditar.setTextSize(16f);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(8, 0, 4, 0);
            btnEditar.setLayoutParams(bp);
            btnEditar.setOnClickListener(v ->
                    mostrarDialogEditar(uid, listaUsuarios.get(idx)));
            fila.addView(btnEditar);

            Button btnEliminar = new Button(this);
            btnEliminar.setText("🗑️");
            btnEliminar.setBackgroundColor(0xFFE53935);
            btnEliminar.setTextSize(16f);
            btnEliminar.setLayoutParams(bp);
            btnEliminar.setOnClickListener(v ->
                    mostrarDialogEliminar(uid, email));
            fila.addView(btnEliminar);

            layoutUsuarios.addView(fila);
        }
    }

    // ═════════════════════════════════════════
    // DIALOG EDITAR USUARIO
    // ═════════════════════════════════════════
    private void mostrarDialogEditar(String uid, Map<String, Object> datos) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 32, 48, 32);
        scroll.addView(container);

        container.addView(label("Nombre completo"));
        EditText etNombre = input(getStr(datos, "nombre", ""), "Nombre");
        container.addView(etNombre);

        container.addView(label("DNI"));
        EditText etDni = input(getStr(datos, "dni", ""), "DNI");
        container.addView(etDni);

        container.addView(label("Teléfono"));
        EditText etTelefono = input(getStr(datos, "telefono", ""), "Teléfono");
        etTelefono.setInputType(InputType.TYPE_CLASS_PHONE);
        container.addView(etTelefono);

        container.addView(label("Tipo de usuario"));
        String[] tipos = {"USER", "MASTER"};
        Spinner spinnerTipo = new Spinner(this);
        ArrayAdapter<String> adTipo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tipos);
        adTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adTipo);
        spinnerTipo.setSelection(getStr(datos, "tipo", "USER").equals("MASTER") ? 1 : 0);
        container.addView(spinnerTipo);

        // ── Sección mascotas ──────────────────
        LinearLayout seccionMascotas = new LinearLayout(this);
        seccionMascotas.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.setMargins(0, 24, 0, 0);
        seccionMascotas.setLayoutParams(mp);

        TextView tvMascotas = new TextView(this);
        tvMascotas.setText("🐾 Mascotas asignadas");
        tvMascotas.setTextColor(0xFF4E342E);
        tvMascotas.setTextSize(16f);
        tvMascotas.setTypeface(null, android.graphics.Typeface.BOLD);
        seccionMascotas.addView(tvMascotas);

        LinearLayout listaMascotas = new LinearLayout(this);
        listaMascotas.setOrientation(LinearLayout.VERTICAL);
        seccionMascotas.addView(listaMascotas);

        // Botón: asignar mascota existente sin dueño
        Button btnAsignarExistente = new Button(this);
        btnAsignarExistente.setText("🔗 Asignar mascota existente");
        btnAsignarExistente.setAllCaps(false);
        btnAsignarExistente.setBackgroundColor(0xFF6D4C41);
        btnAsignarExistente.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams pBtnEx = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pBtnEx.setMargins(0, 8, 0, 4);
        btnAsignarExistente.setLayoutParams(pBtnEx);
        btnAsignarExistente.setOnClickListener(v ->
                mostrarDialogAsignarExistente(uid, listaMascotas));
        seccionMascotas.addView(btnAsignarExistente);

        // Botón: crear mascota nueva
        Button btnAnadir = new Button(this);
        btnAnadir.setText("+ Crear nueva mascota");
        btnAnadir.setAllCaps(false);
        btnAnadir.setBackgroundColor(0xFFFBC02D);
        btnAnadir.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams pBtnNew = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pBtnNew.setMargins(0, 4, 0, 0);
        btnAnadir.setLayoutParams(pBtnNew);
        btnAnadir.setOnClickListener(v ->
                mostrarDialogAnadirMascota(uid, listaMascotas));
        seccionMascotas.addView(btnAnadir);

        if (getStr(datos, "tipo", "USER").equals("USER")) {
            container.addView(seccionMascotas);
            cargarMascotasEnDialog(uid, listaMascotas);
        }

        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean esUser = tipos[pos].equals("USER");
                if (esUser && seccionMascotas.getParent() == null) {
                    container.addView(seccionMascotas);
                    cargarMascotasEnDialog(uid, listaMascotas);
                } else if (!esUser && seccionMascotas.getParent() != null) {
                    container.removeView(seccionMascotas);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar usuario")
                .setView(scroll)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("nombre",   etNombre.getText().toString().trim());
            updates.put("dni",      etDni.getText().toString().trim());
            updates.put("telefono", etTelefono.getText().toString().trim());
            updates.put("tipo",     tipos[spinnerTipo.getSelectedItemPosition()]);

            db.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Usuario actualizado ✔",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarUsuarios(etBuscar.getText().toString().toLowerCase());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        });
    }

    // ═════════════════════════════════════════
    // ASIGNAR MASCOTA EXISTENTE SIN DUEÑO
    // ═════════════════════════════════════════
    private void mostrarDialogAsignarExistente(String uid, LinearLayout listaMascotas) {
        // Buscar mascotas sin ownerId o con ownerId vacío
        db.collection("mascotas").get()
                .addOnSuccessListener(snapshot -> {
                    List<String> nombres = new ArrayList<>();
                    List<String> ids     = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String ownerId = doc.getString("ownerId");
                        if (ownerId == null || ownerId.isEmpty()) {
                            String nombre  = doc.getString("nombre");
                            String especie = doc.getString("especie");
                            nombres.add((nombre != null ? nombre : "Sin nombre")
                                    + " (" + (especie != null ? especie : "-") + ")");
                            ids.add(doc.getId());
                        }
                    }

                    if (nombres.isEmpty()) {
                        Toast.makeText(this,
                                "No hay mascotas sin dueño asignado",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Mostrar lista para seleccionar
                    String[] opcionesArray = nombres.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle("🔗 Selecciona una mascota")
                            .setItems(opcionesArray, (d, which) -> {
                                String mascotaId = ids.get(which);

                                // Asignar ownerId a la mascota seleccionada
                                Map<String, Object> update = new HashMap<>();
                                update.put("ownerId", uid);

                                db.collection("mascotas").document(mascotaId)
                                        .set(update, SetOptions.merge())
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this,
                                                    "Mascota asignada ✔",
                                                    Toast.LENGTH_SHORT).show();
                                            cargarMascotasEnDialog(uid, listaMascotas);
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this,
                                                        "Error: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show());
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ═════════════════════════════════════════
    // MASCOTAS EN DIALOG EDITAR
    // ═════════════════════════════════════════
    private void cargarMascotasEnDialog(String uid, LinearLayout lista) {
        lista.removeAllViews();
        db.collection("mascotas").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("Sin mascotas registradas");
                        tv.setTextColor(0xFF9E9E9E);
                        lista.addView(tv);
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String nombre    = doc.getString("nombre");
                        String especie   = doc.getString("especie");
                        String mascotaId = doc.getId();

                        LinearLayout fila = new LinearLayout(this);
                        fila.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        p.setMargins(0, 6, 0, 6);
                        fila.setLayoutParams(p);
                        fila.setBackgroundColor(0xFFFFECB3);
                        fila.setPadding(12, 8, 8, 8);

                        TextView tv = new TextView(this);
                        tv.setText("🐾 " + nombre + " (" + especie + ")");
                        tv.setTextColor(0xFF4E342E);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        fila.addView(tv);

                        Button btnBorrar = new Button(this);
                        btnBorrar.setText("🗑️");
                        btnBorrar.setBackgroundColor(0xFFE53935);
                        btnBorrar.setTextColor(0xFFFFFFFF);
                        btnBorrar.setOnClickListener(v ->
                                db.collection("mascotas").document(mascotaId).delete()
                                        .addOnSuccessListener(x ->
                                                cargarMascotasEnDialog(uid, lista)));
                        fila.addView(btnBorrar);
                        lista.addView(fila);
                    }
                });
    }

    // ═════════════════════════════════════════
    // DIALOG AÑADIR MASCOTA NUEVA
    // ═════════════════════════════════════════
    private void mostrarDialogAnadirMascota(String uid, LinearLayout listaMascotasDialog) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 32, 48, 32);
        scroll.addView(form);

        form.addView(label("Microchip"));
        LinearLayout rowChip = new LinearLayout(this);
        rowChip.setOrientation(LinearLayout.HORIZONTAL);
        rowChip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView tvChipNo = new TextView(this);
        tvChipNo.setText("No  "); tvChipNo.setTextColor(0xFF4E342E);
        Switch switchChip = new Switch(this);
        switchChip.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFBC02D));
        switchChip.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFFFFECB3));
        TextView tvChipSi = new TextView(this);
        tvChipSi.setText("  Sí"); tvChipSi.setTextColor(0xFF4E342E);
        rowChip.addView(tvChipNo); rowChip.addView(switchChip); rowChip.addView(tvChipSi);
        form.addView(rowChip);

        form.addView(label("Nombre *"));
        EditText etNombre = input("", "Nombre de la mascota");
        form.addView(etNombre);

        form.addView(label("Edad"));
        String[] edades = new String[17];
        edades[0] = "Menos de 1 año";
        for (int i = 1; i <= 15; i++) edades[i] = i + (i == 1 ? " año" : " años");
        edades[16] = "Más de 15 años";
        Spinner spinnerEdad = spinner(edades);
        form.addView(spinnerEdad);

        form.addView(label("Especie *"));
        String[] especies = {"Perro", "Gato"};
        Spinner spinnerEspecie = spinner(especies);
        form.addView(spinnerEspecie);

        form.addView(label("Raza"));
        AutoCompleteTextView acRaza = new AutoCompleteTextView(this);
        acRaza.setHint("Escribe o selecciona una raza");
        acRaza.setTextColor(0xFF4E342E);
        acRaza.setHintTextColor(0xFFBCAAA4);
        acRaza.setBackgroundColor(0xFFFFECB3);
        acRaza.setPadding(16, 12, 16, 12);
        acRaza.setThreshold(1);
        form.addView(acRaza);

        form.addView(label("Color del pelaje"));
        EditText etColor = input("", "Ej: negro, blanco, atigrado...");
        form.addView(etColor);

        form.addView(label("Condiciones médicas"));
        LinearLayout layoutCondiciones = new LinearLayout(this);
        layoutCondiciones.setOrientation(LinearLayout.VERTICAL);
        form.addView(layoutCondiciones);
        Button btnAddCondicion = new Button(this);
        btnAddCondicion.setText("+ Añadir condición");
        btnAddCondicion.setAllCaps(false);
        btnAddCondicion.setBackgroundColor(0xFFFFECB3);
        btnAddCondicion.setTextColor(0xFFFBC02D);
        btnAddCondicion.setOnClickListener(v -> agregarCampoCondicion(layoutCondiciones));
        form.addView(btnAddCondicion);

        form.addView(label("Vacunas administradas"));
        LinearLayout layoutVacunas = new LinearLayout(this);
        layoutVacunas.setOrientation(LinearLayout.VERTICAL);
        form.addView(layoutVacunas);

        actualizarRazas(acRaza, true);
        actualizarVacunas(layoutVacunas, true);

        spinnerEspecie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean esPerro = pos == 0;
                actualizarRazas(acRaza, esPerro);
                actualizarVacunas(layoutVacunas, esPerro);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🐾 Nueva mascota")
                .setView(scroll)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre es obligatorio",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> condiciones = new ArrayList<>();
            for (int i = 0; i < layoutCondiciones.getChildCount(); i++) {
                View child = layoutCondiciones.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout fila = (LinearLayout) child;
                    for (int j = 0; j < fila.getChildCount(); j++)
                        if (fila.getChildAt(j) instanceof EditText) {
                            String c = ((EditText) fila.getChildAt(j))
                                    .getText().toString().trim();
                            if (!c.isEmpty()) condiciones.add(c);
                        }
                }
            }
            List<String> vacunas = new ArrayList<>();
            for (int i = 0; i < layoutVacunas.getChildCount(); i++) {
                View child = layoutVacunas.getChildAt(i);
                if (child instanceof CheckBox && ((CheckBox) child).isChecked())
                    vacunas.add(((CheckBox) child).getText().toString());
            }
            Map<String, Object> mascota = new HashMap<>();
            mascota.put("nombre",      nombre);
            mascota.put("especie",     especies[spinnerEspecie.getSelectedItemPosition()]);
            mascota.put("raza",        acRaza.getText().toString().trim());
            mascota.put("edad",        edades[spinnerEdad.getSelectedItemPosition()]);
            mascota.put("color",       etColor.getText().toString().trim());
            mascota.put("microchip",   switchChip.isChecked());
            mascota.put("condiciones", condiciones);
            mascota.put("vacunas",     vacunas);
            mascota.put("ownerId",     uid);

            db.collection("mascotas").add(mascota)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Mascota creada ✔",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarMascotasEnDialog(uid, listaMascotasDialog);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        });
    }

    private void agregarCampoCondicion(LinearLayout layout) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 4, 0, 4);
        fila.setLayoutParams(p);
        EditText et = new EditText(this);
        et.setHint("Condición médica");
        et.setTextColor(0xFF4E342E);
        et.setHintTextColor(0xFFBCAAA4);
        et.setBackgroundColor(0xFFFFECB3);
        et.setPadding(12, 10, 12, 10);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        fila.addView(et);
        Button btnRemove = new Button(this);
        btnRemove.setText("✕");
        btnRemove.setTextColor(0xFFFFFFFF);
        btnRemove.setBackgroundColor(0xFFE53935);
        btnRemove.setOnClickListener(v -> layout.removeView(fila));
        fila.addView(btnRemove);
        layout.addView(fila);
    }

    private void actualizarRazas(AutoCompleteTextView ac, boolean esPerro) {
        String[] razas = esPerro ? RAZAS_PERRO : RAZAS_GATO;
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, razas);
        ac.setAdapter(ad);
        ac.setText("");
    }

    private void actualizarVacunas(LinearLayout layout, boolean esPerro) {
        layout.removeAllViews();
        String[] vacunas = esPerro ? VACUNAS_PERRO : VACUNAS_GATO;
        for (String v : vacunas) {
            CheckBox cb = new CheckBox(this);
            cb.setText(v);
            cb.setTextColor(0xFF4E342E);
            cb.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFBC02D));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 4, 0, 4);
            cb.setLayoutParams(p);
            layout.addView(cb);
        }
    }

    // ═════════════════════════════════════════
    // DIALOG ELIMINAR CON CUENTA ATRÁS
    // ═════════════════════════════════════════
    private void mostrarDialogEliminar(String uid, String email) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView tvMsg = new TextView(this);
        tvMsg.setText("¿Estás seguro de que deseas eliminar a este usuario?\n\n📧 " + email);
        tvMsg.setTextColor(0xFF4E342E);
        tvMsg.setTextSize(15f);
        layout.addView(tvMsg);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar usuario")
                .setView(layout)
                .setPositiveButton("Sí, eliminar", null)
                .setNegativeButton("No", null)
                .create();
        dialog.show();

        Button btnSi = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSi.setEnabled(false);
        btnSi.setBackgroundColor(0xFFBDBDBD);
        btnSi.setTextColor(0xFFFFFFFF);

        new CountDownTimer(5000, 1000) {
            @Override public void onTick(long ms) {
                btnSi.setText("Sí — espera " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                btnSi.setText("Sí, eliminar");
                btnSi.setEnabled(true);
                btnSi.setBackgroundColor(0xFFE53935);
            }
        }.start();

        btnSi.setOnClickListener(v ->
                db.collection("users").document(uid).delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Usuario eliminado",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            cargarUsuarios(etBuscar.getText().toString().toLowerCase());
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show()));
    }

    // ═════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════
    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return (val instanceof String) ? (String) val : def;
    }

    private TextView label(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(0xFF4E342E);
        tv.setTextSize(13f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 20, 0, 4);
        tv.setLayoutParams(p);
        return tv;
    }

    private EditText input(String valor, String hint) {
        EditText et = new EditText(this);
        et.setText(valor);
        et.setHint(hint);
        et.setTextColor(0xFF4E342E);
        et.setHintTextColor(0xFFBCAAA4);
        et.setBackgroundColor(0xFFFFECB3);
        et.setPadding(16, 12, 16, 12);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        return et;
    }

    private Spinner spinner(String[] opciones) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 4, 0, 4);
        sp.setLayoutParams(p);
        return sp;
    }
}