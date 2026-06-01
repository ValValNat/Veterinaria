package com.example.veterinaria;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.*;

public class DetalleMascota extends AppCompatActivity {

    TextView tvNombre, tvDatosBasicos, tvEdad, tvColor,
            tvMicrochip, tvVacunas, tvCondiciones, tvObservaciones;
    ImageView imgMascota;
    LinearLayout layoutSinFoto;
    Button btnEditar, btnEliminar;

    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseStorage storage;
    StorageReference storageRef;

    String idMascota;
    boolean esMaster = false;

    private Uri fotoUri;
    private ActivityResultLauncher<Uri> tomarFotoLauncher;
    private ActivityResultLauncher<String> pedirPermisoCamara;

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
        setContentView(R.layout.activity_detalle_mascota);

        db      = FirebaseFirestore.getInstance();
        auth    = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        tvNombre        = findViewById(R.id.tvNombre);
        tvDatosBasicos  = findViewById(R.id.tvDatosBasicos);
        tvEdad          = findViewById(R.id.tvEdad);
        tvColor         = findViewById(R.id.tvColor);
        tvMicrochip     = findViewById(R.id.tvMicrochip);
        tvVacunas       = findViewById(R.id.tvVacunas);
        tvCondiciones   = findViewById(R.id.tvCondiciones);
        tvObservaciones = findViewById(R.id.tvObservaciones);
        imgMascota      = findViewById(R.id.imgMascota);
        layoutSinFoto   = findViewById(R.id.layoutSinFoto);
        btnEditar       = findViewById(R.id.btnEditar);
        btnEliminar     = findViewById(R.id.btnEliminar);

        idMascota = getIntent().getStringExtra("idMascota");
        if (idMascota == null) { finish(); return; }

        storageRef = storage.getReference()
                .child("mascotas")
                .child(idMascota)
                .child("foto.jpg");

        // ── Launcher cámara ───────────────────
        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && fotoUri != null) {
                        subirFotoAStorage(fotoUri);
                    }
                });

        pedirPermisoCamara = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) lanzarCamara();
                    else Toast.makeText(this,
                            "Permiso de cámara denegado",
                            Toast.LENGTH_SHORT).show();
                });

        detectarRolYCargar();
    }

    // ─────────────────────────────────────────
    // DETECTAR ROL
    // ─────────────────────────────────────────
    private void detectarRolYCargar() {
        String uid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;
        if (uid == null) { cargarDatos(); return; }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    esMaster = "MASTER".equals(doc.getString("tipo"));
                    cargarDatos();
                })
                .addOnFailureListener(e -> cargarDatos());
    }

    // ─────────────────────────────────────────
    // CARGAR DATOS
    // ─────────────────────────────────────────
    private void cargarDatos() {
        db.collection("mascotas").document(idMascota).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    String nombre  = getDocStr(doc, "nombre",  "Sin nombre");
                    String especie = getDocStr(doc, "especie", "-");
                    String raza    = getDocStr(doc, "raza",    "-");
                    String edad    = getDocStr(doc, "edad",    "-");
                    String color   = getDocStr(doc, "color",   "-");
                    Boolean micro  = doc.getBoolean("microchip");
                    String obs     = getDocStr(doc, "observaciones", "Sin observaciones");

                    List<String> vacunasList = (List<String>) doc.get("vacunas");
                    StringBuilder vacunasStr = new StringBuilder();
                    if (vacunasList != null && !vacunasList.isEmpty())
                        for (String v : vacunasList)
                            vacunasStr.append("• ").append(v).append("\n");
                    else vacunasStr.append("No registradas");

                    List<String> condList = (List<String>) doc.get("condiciones");
                    StringBuilder condStr = new StringBuilder();
                    if (condList != null && !condList.isEmpty())
                        for (String c : condList)
                            condStr.append("• ").append(c).append("\n");
                    else condStr.append("Sin condiciones registradas");

                    tvNombre.setText(nombre);
                    tvDatosBasicos.setText(especie + " · " + raza);
                    tvEdad.setText("🎂 Edad: " + edad);
                    tvColor.setText("🎨 Color: " + color);
                    tvMicrochip.setText("📡 Microchip: "
                            + (micro != null ? (micro ? "Sí" : "No") : "-"));
                    tvVacunas.setText(vacunasStr.toString().trim());
                    tvCondiciones.setText(condStr.toString().trim());
                    tvObservaciones.setText(obs);

                    // Cargar foto desde Firebase Storage
                    cargarFotoDesdeStorage();

                    if (esMaster) {
                        layoutSinFoto.setOnClickListener(v -> pedirOUsarCamara());
                        imgMascota.setOnClickListener(v -> pedirOUsarCamara());
                        btnEditar.setVisibility(View.VISIBLE);
                        btnEliminar.setVisibility(View.VISIBLE);
                        btnEditar.setOnClickListener(v ->
                                mostrarDialogEditar(doc.getData()));
                        btnEliminar.setOnClickListener(v -> confirmarEliminar());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────
    // CARGAR FOTO DESDE STORAGE
    // ─────────────────────────────────────────
    private void cargarFotoDesdeStorage() {
        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // Foto existe → cargar con Glide
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(imgMascota);
                    layoutSinFoto.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    // No hay foto → mostrar icono
                    layoutSinFoto.setVisibility(View.VISIBLE);
                });
    }

    // ─────────────────────────────────────────
    // SUBIR FOTO A STORAGE
    // ─────────────────────────────────────────
    private void subirFotoAStorage(Uri uri) {
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show();

        storageRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(this, "Foto guardada ✔",
                            Toast.LENGTH_SHORT).show();
                    cargarFotoDesdeStorage();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al subir: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─────────────────────────────────────────
    // CÁMARA
    // ─────────────────────────────────────────
    private void pedirOUsarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara();
        } else {
            pedirPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    private void lanzarCamara() {
        File foto = new File(getFilesDir(), "fotos/temp_" + idMascota + ".jpg");
        foto.getParentFile().mkdirs();
        fotoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", foto);
        tomarFotoLauncher.launch(fotoUri);
    }

    // ─────────────────────────────────────────
    // DIALOG EDITAR
    // ─────────────────────────────────────────
    private void mostrarDialogEditar(Map<String, Object> datos) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 32, 48, 32);
        scroll.addView(form);

        form.addView(label("Microchip"));
        LinearLayout rowChip = new LinearLayout(this);
        rowChip.setOrientation(LinearLayout.HORIZONTAL);
        rowChip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView tvNo = new TextView(this); tvNo.setText("No  "); tvNo.setTextColor(0xFF4E342E);
        Switch swChip = new Switch(this);
        swChip.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFBC02D));
        swChip.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFFFFECB3));
        Boolean microActual = (Boolean) datos.get("microchip");
        swChip.setChecked(microActual != null && microActual);
        TextView tvSi = new TextView(this); tvSi.setText("  Sí"); tvSi.setTextColor(0xFF4E342E);
        rowChip.addView(tvNo); rowChip.addView(swChip); rowChip.addView(tvSi);
        form.addView(rowChip);

        form.addView(label("Nombre *"));
        EditText etNombre = input(getStr(datos, "nombre"), "Nombre");
        form.addView(etNombre);

        form.addView(label("Edad"));
        Spinner spEdad = spinnerOf(EDADES);
        String edadActual = getStr(datos, "edad");
        for (int i = 0; i < EDADES.length; i++)
            if (EDADES[i].equals(edadActual)) { spEdad.setSelection(i); break; }
        form.addView(spEdad);

        form.addView(label("Especie *"));
        String[] especies = {"Perro", "Gato"};
        Spinner spEspecie = spinnerOf(especies);
        String especieActual = getStr(datos, "especie");
        spEspecie.setSelection("Gato".equals(especieActual) ? 1 : 0);
        form.addView(spEspecie);

        form.addView(label("Raza"));
        AutoCompleteTextView acRaza = new AutoCompleteTextView(this);
        acRaza.setHint("Escribe o selecciona");
        acRaza.setTextColor(0xFF4E342E);
        acRaza.setHintTextColor(0xFFBCAAA4);
        acRaza.setBackgroundColor(0xFFFFECB3);
        acRaza.setPadding(16, 12, 16, 12);
        acRaza.setThreshold(1);
        acRaza.setText(getStr(datos, "raza"));
        form.addView(acRaza);

        form.addView(label("Color del pelaje"));
        EditText etColor = input(getStr(datos, "color"), "Ej: negro, blanco...");
        form.addView(etColor);

        form.addView(label("Condiciones médicas"));
        LinearLayout layoutCond = new LinearLayout(this);
        layoutCond.setOrientation(LinearLayout.VERTICAL);
        form.addView(layoutCond);
        List<String> condActuales = (List<String>) datos.get("condiciones");
        if (condActuales != null)
            for (String c : condActuales) agregarCampoCond(layoutCond, c);
        Button btnAddCond = new Button(this);
        btnAddCond.setText("+ Añadir condición");
        btnAddCond.setAllCaps(false);
        btnAddCond.setBackgroundColor(0xFFFFECB3);
        btnAddCond.setTextColor(0xFFFBC02D);
        btnAddCond.setOnClickListener(v -> agregarCampoCond(layoutCond, ""));
        form.addView(btnAddCond);

        form.addView(label("Vacunas administradas"));
        LinearLayout layoutVac = new LinearLayout(this);
        layoutVac.setOrientation(LinearLayout.VERTICAL);
        form.addView(layoutVac);
        List<String> vacActuales = (List<String>) datos.get("vacunas");
        boolean esPerroInicial = !"Gato".equals(especieActual);
        actualizarVacunasEditar(layoutVac, esPerroInicial, vacActuales);

        spEspecie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean esPerro = pos == 0;
                actualizarRazas(acRaza, esPerro);
                actualizarVacunasEditar(layoutVac, esPerro, null);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        actualizarRazas(acRaza, esPerroInicial);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("✏️ Editar mascota")
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
            for (int i = 0; i < layoutCond.getChildCount(); i++) {
                View child = layoutCond.getChildAt(i);
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
                View child = layoutVac.getChildAt(i);
                if (child instanceof CheckBox && ((CheckBox) child).isChecked())
                    vacunas.add(((CheckBox) child).getText().toString());
            }
            Map<String, Object> update = new HashMap<>();
            update.put("nombre",      nombre);
            update.put("especie",     especies[spEspecie.getSelectedItemPosition()]);
            update.put("raza",        acRaza.getText().toString().trim());
            update.put("edad",        EDADES[spEdad.getSelectedItemPosition()]);
            update.put("color",       etColor.getText().toString().trim());
            update.put("microchip",   swChip.isChecked());
            update.put("condiciones", condiciones);
            update.put("vacunas",     vacunas);

            db.collection("mascotas").document(idMascota)
                    .set(update, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Mascota actualizada ✔",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarDatos();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        });
    }

    // ─────────────────────────────────────────
    // CONFIRMAR ELIMINAR
    // ─────────────────────────────────────────
    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar mascota")
                .setMessage("¿Estás seguro?\nEsta acción no se puede deshacer.")
                .setPositiveButton("Sí, eliminar", (d, w) -> {
                    // Borrar foto de Storage también
                    storageRef.delete(); // silencioso si no existe
                    db.collection("mascotas").document(idMascota).delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Mascota eliminada",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────
    private void actualizarRazas(AutoCompleteTextView ac, boolean esPerro) {
        ac.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                esPerro ? RAZAS_PERRO : RAZAS_GATO));
    }

    private void actualizarVacunasEditar(LinearLayout layout,
                                         boolean esPerro, List<String> marcadas) {
        layout.removeAllViews();
        for (String v : (esPerro ? VACUNAS_PERRO : VACUNAS_GATO)) {
            CheckBox cb = new CheckBox(this);
            cb.setText(v);
            cb.setTextColor(0xFF4E342E);
            cb.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFBC02D));
            if (marcadas != null) cb.setChecked(marcadas.contains(v));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 4, 0, 4);
            cb.setLayoutParams(p);
            layout.addView(cb);
        }
    }

    private void agregarCampoCond(LinearLayout layout, String valor) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 4, 0, 4);
        fila.setLayoutParams(p);
        EditText et = new EditText(this);
        et.setText(valor);
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

    private String getDocStr(DocumentSnapshot doc, String key, String def) {
        String val = doc.getString(key);
        return val != null ? val : def;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof String) ? (String) val : "";
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