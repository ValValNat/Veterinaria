package com.example.veterinaria;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;
import java.util.*;

// Pantalla "Pacientes" del veterinario
// Muestra todas las mascotas con buscador en tiempo real y boton para crear nuevas
public class PacientesActivity extends AppCompatActivity {

    private EditText etBuscar;
    private LinearLayout layoutPacientes;
    private FirebaseFirestore db;

    // Dos listas paralelas: datos de cada mascota y su ID de Firebase
    // La posicion 0 de ambas listas corresponde siempre a la misma mascota
    private final List<Map<String, Object>> listaMascotas = new ArrayList<>();
    private final List<String> idsMascotas = new ArrayList<>();

    // Razas, vacunas y edades para el formulario de crear mascota
    // Son static final porque nunca cambian
    private static final String[] RAZAS_PERRO = {
            "Labrador Retriever","Golden Retriever","Pastor Alemán",
            "Bulldog Francés","Caniche / Poodle","Yorkshire Terrier",
            "Chihuahua","Beagle","Boxer","Rottweiler","Dóberman",
            "Shih Tzu","Schnauzer","Husky Siberiano","Bichón Maltés",
            "Cocker Spaniel","Dálmata","Pomerania","Border Collie",
            "Galgo Español","Otra raza","Desconocido"
    };

    private static final String[] RAZAS_GATO = {
            "Europeo Común","Persa","Siamés","Maine Coon","Bengalí",
            "Ragdoll","British Shorthair","Abisinio","Esfinge",
            "Scottish Fold","Angora","Ruso Azul","Noruego del Bosque",
            "Otra raza","Desconocido"
    };

    private static final String[] VACUNAS_PERRO = {
            "Moquillo canino","Parvovirus canino",
            "Hepatitis infecciosa canina","Leptospirosis","Rabia",
            "Tos de las perreras (Bordetella)","Leishmaniosis"
    };

    private static final String[] VACUNAS_GATO = {
            "Panleucopenia felina","Rinotraqueítis viral (Herpesvirus)",
            "Calicivirus felino","Leucemia felina (FeLV)",
            "Rabia","Clamidiosis felina"
    };

    private static final String[] EDADES = buildEdades();

    // Genera ["Menos de 1 año", "1 año", "2 años", ..., "15 años", "Mas de 15 años"]
    private static String[] buildEdades() {
        String[] e = new String[17];
        e[0] = "Menos de 1 año";
        for (int i = 1; i <= 15; i++) e[i] = i + (i == 1 ? " año" : " años");
        e[16] = "Más de 15 años";
        return e;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pacientes);

        db              = FirebaseFirestore.getInstance();
        etBuscar        = findViewById(R.id.etBuscar);
        layoutPacientes = findViewById(R.id.layoutPacientes);

        findViewById(R.id.btnCrearMascota).setOnClickListener(v ->
                mostrarDialogCrearMascota());

        // Buscador en tiempo real: filtra en cada tecla pulsada
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                cargarMascotas(s.toString().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }


    // onResume se ejecuta cada vez que la pantalla vuelve a ser visible
    // Recargamos para reflejar cambios hechos en otras pantallas (ej: borrar una mascota)
    @Override
    protected void onResume() {
        super.onResume();
        cargarMascotas(etBuscar.getText().toString().toLowerCase());
    }


    // Descarga todas las mascotas de Firebase y filtra por nombre
    // El filtrado se hace aqui porque Firebase no soporta busqueda de texto parcial
    private void cargarMascotas(String filtro) {
        db.collection("mascotas").get()
                .addOnSuccessListener(snapshot -> {
                    listaMascotas.clear();
                    idsMascotas.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String nombre = doc.getString("nombre");
                        // contains("") siempre es true, asi que con filtro vacio se muestran todas
                        if (nombre != null && nombre.toLowerCase().contains(filtro)) {
                            listaMascotas.add(doc.getData());
                            idsMascotas.add(doc.getId());
                        }
                    }
                    mostrarMascotas();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    // Pinta una fila por cada mascota de la lista
    private void mostrarMascotas() {
        layoutPacientes.removeAllViews();

        if (listaMascotas.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No hay mascotas registradas");
            tv.setTextColor(0xFF9E9E9E);
            tv.setTextSize(15f);
            tv.setPadding(8, 16, 8, 16);
            layoutPacientes.addView(tv);
            return;
        }

        for (int i = 0; i < listaMascotas.size(); i++) {
            // "id" como final para poder usarlo dentro del listener del boton
            // Si usaramos idsMascotas.get(i) dentro de la lambda, Java se quejaria
            // porque "i" cambia en cada iteracion del bucle
            final String id = idsMascotas.get(i);
            Map<String, Object> m = listaMascotas.get(i);

            String nombre  = getStr(m, "nombre",  "Sin nombre");
            String especie = getStr(m, "especie", "-");
            String raza    = getStr(m, "raza",    "-");

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setBackgroundColor(0xFFFFECB3);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            fp.setMargins(0, 0, 0, 10);
            fila.setLayoutParams(fp);
            fila.setPadding(20, 16, 16, 16);

            String icono = "Gato".equals(especie) ? "🐱 " : "🐶 ";

            // weight = 1f: ocupa todo el espacio sobrante dejando sitio justo para el boton
            TextView tv = new TextView(this);
            tv.setText(icono + nombre + "\n" + especie + " · " + raza);
            tv.setTextColor(0xFF4E342E);
            tv.setTextSize(14f);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            fila.addView(tv);

            Button btnVer = new Button(this);
            btnVer.setText("Ver");
            btnVer.setAllCaps(false);
            btnVer.setBackgroundColor(0xFFFBC02D);
            btnVer.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(8, 0, 0, 0);
            btnVer.setLayoutParams(bp);
            btnVer.setOnClickListener(v -> {
                Intent intent = new Intent(this, DetalleMascota.class);
                intent.putExtra("idMascota", id);
                startActivity(intent);
            });

            fila.addView(btnVer);
            layoutPacientes.addView(fila);
        }
    }


    // Formulario para registrar una mascota nueva
    // Se puede crear sin dueno asignado o vincularla a uno escribiendo su email
    private void mostrarDialogCrearMascota() {

        // android.app.AlertDialog con paquete completo para evitar conflicto
        // con androidx.appcompat.app.AlertDialog que tambien existe
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this);
        builder.setTitle("🐾 Nueva mascota");

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 32, 48, 32);
        scroll.addView(form);

        // Fila "No - switch - Si"
        form.addView(label("Microchip"));
        LinearLayout rowChip = new LinearLayout(this);
        rowChip.setOrientation(LinearLayout.HORIZONTAL);
        rowChip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView tvNo = new TextView(this);
        tvNo.setText("No  ");
        tvNo.setTextColor(0xFF4E342E);
        Switch swChip = new Switch(this);
        swChip.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFBC02D));
        swChip.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFFFFECB3));
        TextView tvSi = new TextView(this);
        tvSi.setText("  Sí");
        tvSi.setTextColor(0xFF4E342E);
        rowChip.addView(tvNo);
        rowChip.addView(swChip);
        rowChip.addView(tvSi);
        form.addView(rowChip);

        form.addView(label("Nombre *"));
        EditText etNombre = input("", "Nombre de la mascota");
        form.addView(etNombre);

        // Si se rellena este campo buscamos al dueno por email y vinculamos la mascota
        // Si se deja vacio la mascota queda sin dueno hasta que se asigne manualmente
        form.addView(label("Email del dueño (opcional)"));
        EditText etDueno = input("", "email@ejemplo.com");
        etDueno.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(etDueno);

        form.addView(label("Edad"));
        Spinner spEdad = spinnerOf(EDADES);
        form.addView(spEdad);

        form.addView(label("Especie *"));
        String[] especies = {"Perro", "Gato"};
        Spinner spEspecie = spinnerOf(especies);
        form.addView(spEspecie);

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
        LinearLayout layoutCond = new LinearLayout(this);
        layoutCond.setOrientation(LinearLayout.VERTICAL);
        form.addView(layoutCond);
        Button btnAddCond = new Button(this);
        btnAddCond.setText("+ Añadir condición");
        btnAddCond.setAllCaps(false);
        btnAddCond.setBackgroundColor(0xFFFFECB3);
        btnAddCond.setTextColor(0xFFFBC02D);
        btnAddCond.setOnClickListener(v -> agregarCampoCond(layoutCond));
        form.addView(btnAddCond);

        form.addView(label("Vacunas administradas"));
        LinearLayout layoutVac = new LinearLayout(this);
        layoutVac.setOrientation(LinearLayout.VERTICAL);
        form.addView(layoutVac);

        // Perro por defecto porque es la primera opcion del spinner
        actualizarRazas(acRaza, true);
        actualizarVacunas(layoutVac, true);

        // Al cambiar la especie actualizamos razas y vacunas dinamicamente
        spEspecie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, android.view.View v,
                                       int pos, long id) {
                boolean esPerro = pos == 0;
                actualizarRazas(acRaza, esPerro);
                actualizarVacunas(layoutVac, esPerro);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        builder.setView(scroll);
        builder.setNegativeButton("Cancelar", null);

        // El listener de "Guardar" va despues del show() para controlar
        // si el dialogo se cierra o no segun haya errores de validacion
        builder.setPositiveButton("Guardar", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String nombre = etNombre.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre es obligatorio",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Recorremos cada fila del layoutCond buscando el EditText dentro de ella
            List<String> condiciones = new ArrayList<>();
            for (int i = 0; i < layoutCond.getChildCount(); i++) {
                android.view.View child = layoutCond.getChildAt(i);
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
            for (int i = 0; i < layoutVac.getChildCount(); i++) {
                android.view.View child = layoutVac.getChildAt(i);
                if (child instanceof CheckBox && ((CheckBox) child).isChecked())
                    vacunas.add(((CheckBox) child).getText().toString());
            }

            String emailDueno = etDueno.getText().toString().trim();
            Map<String, Object> mascota = new HashMap<>();
            mascota.put("nombre",      nombre);
            mascota.put("especie",     especies[spEspecie.getSelectedItemPosition()]);
            mascota.put("raza",        acRaza.getText().toString().trim());
            mascota.put("edad",        EDADES[spEdad.getSelectedItemPosition()]);
            mascota.put("color",       etColor.getText().toString().trim());
            mascota.put("microchip",   swChip.isChecked());
            mascota.put("condiciones", condiciones);
            mascota.put("vacunas",     vacunas);

            if (emailDueno.isEmpty()) {
                guardarMascota(mascota, dialog);
            } else {
                // Buscamos el UID del dueno por email para asignarselo a la mascota
                // limit(1) evita resultados multiples aunque haya emails duplicados
                db.collection("users")
                        .whereEqualTo("email", emailDueno)
                        .limit(1).get()
                        .addOnSuccessListener(snap -> {
                            if (!snap.isEmpty()) {
                                mascota.put("ownerId",
                                        snap.getDocuments().get(0).getId());
                            }
                            guardarMascota(mascota, dialog);
                        })
                        .addOnFailureListener(e -> guardarMascota(mascota, dialog));
            }
        });
    }


    // Separado en su propio metodo porque se llama desde dos sitios:
    // cuando hay dueno y cuando no hay dueno
    private void guardarMascota(Map<String, Object> mascota,
                                android.app.AlertDialog dialog) {
        db.collection("mascotas").add(mascota)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Mascota creada ✔",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cargarMascotas(etBuscar.getText().toString().toLowerCase());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    // ── Helpers ──────────────────────────────────────────────

    // Actualiza las sugerencias de raza segun la especie elegida
    private void actualizarRazas(AutoCompleteTextView ac, boolean esPerro) {
        ac.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                esPerro ? RAZAS_PERRO : RAZAS_GATO));
    }

    // Reemplaza los checkboxes de vacunas segun la especie
    // Todos empiezan desmarcados porque es una mascota nueva
    private void actualizarVacunas(LinearLayout layout, boolean esPerro) {
        layout.removeAllViews();
        for (String v : (esPerro ? VACUNAS_PERRO : VACUNAS_GATO)) {
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

    // Añade una fila [campo de texto] [boton X] al contenedor de condiciones
    private void agregarCampoCond(LinearLayout layout) {
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

        Button btn = new Button(this);
        btn.setText("✕");
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(0xFFE53935);
        btn.setOnClickListener(v -> layout.removeView(fila));
        fila.addView(btn);
        layout.addView(fila);
    }

    // Lee un campo de un Map, devuelve "def" si no existe o no es String
    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return (val instanceof String) ? (String) val : def;
    }

    // Etiqueta estilizada para los campos del formulario
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

    // Campo de texto estilizado con valor inicial y placeholder
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

    // Spinner estilizado con las opciones que se le pasan
    private Spinner spinnerOf(String[] opciones) {
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