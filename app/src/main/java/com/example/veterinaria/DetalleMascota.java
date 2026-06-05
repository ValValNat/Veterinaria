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

// Pantalla de detalle de una mascota concreta
// MASTER puede editar, borrar y tomar fotos; USER solo puede ver
public class DetalleMascota extends AppCompatActivity {

    TextView tvNombre, tvDatosBasicos, tvEdad, tvColor,
            tvMicrochip, tvVacunas, tvCondiciones, tvObservaciones;
    ImageView imgMascota;

    // Se muestra cuando no hay foto; al pulsarlo abre la camara (solo MASTER)
    LinearLayout layoutSinFoto;

    // Ocultos en el XML, se hacen visibles solo si el usuario es MASTER
    Button btnEditar;
    Button btnEliminar;

    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseStorage storage;

    // Ruta en Firebase Storage: mascotas/{idMascota}/foto.jpg
    StorageReference storageRef;

    String idMascota;
    boolean esMaster = false;

    // Uri donde la camara guardara la foto antes de subirla a Firebase
    private Uri fotoUri;

    // Los launchers deben registrarse en onCreate, no en el momento de usarlos
    private ActivityResultLauncher<Uri> tomarFotoLauncher;
    private ActivityResultLauncher<String> pedirPermisoCamara;


    // Razas, vacunas y edades para el formulario de edicion
    // static final porque nunca cambian
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

    // Genera ["Menos de 1 año", "1 año", ..., "15 años", "Mas de 15 años"]
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

        // El ID de la mascota llega desde la pantalla anterior via Intent
        idMascota = getIntent().getStringExtra("idMascota");
        if (idMascota == null) { finish(); return; }

        // Siempre el mismo nombre de archivo: subir una foto nueva sobreescribe la anterior
        storageRef = storage.getReference()
                .child("mascotas")
                .child(idMascota)
                .child("foto.jpg");

        // Launcher de camara: success = true si el usuario tomo la foto, false si cancelo
        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && fotoUri != null) {
                        subirFotoAStorage(fotoUri);
                    }
                });

        // Launcher de permiso: granted = true si lo acepto, false si lo rechazo
        pedirPermisoCamara = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) lanzarCamara();
                    else Toast.makeText(this,
                            "Permiso de cámara denegado",
                            Toast.LENGTH_SHORT).show();
                });

        // Primero detectamos el rol y luego cargamos datos
        // El orden importa porque el rol determina que botones mostrar
        detectarRolYCargar();
    }


    // Consulta el tipo del usuario logueado y despues llama a cargarDatos()
    private void detectarRolYCargar() {
        String uid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;

        // Sin UID cargamos igual pero esMaster seguira false (sin botones de edicion)
        if (uid == null) { cargarDatos(); return; }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    // Forma segura de comparar: evita NullPointerException si getString devuelve null
                    esMaster = "MASTER".equals(doc.getString("tipo"));
                    cargarDatos();
                })
                .addOnFailureListener(e -> cargarDatos());
    }


    // Descarga los datos de la mascota y rellena todos los campos de la pantalla
    private void cargarDatos() {
        db.collection("mascotas").document(idMascota).get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) { finish(); return; }

                    String nombre  = getDocStr(doc, "nombre",  "Sin nombre");
                    String especie = getDocStr(doc, "especie", "-");
                    String raza    = getDocStr(doc, "raza",    "-");
                    String edad    = getDocStr(doc, "edad",    "-");
                    String color   = getDocStr(doc, "color",   "-");

                    // Boolean con mayuscula para poder recibir null si el campo no existe
                    Boolean micro  = doc.getBoolean("microchip");
                    String obs     = getDocStr(doc, "observaciones", "Sin observaciones");

                    // Las vacunas y condiciones son listas en Firebase
                    // Necesitan cast desde Object a List<String>
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

                    // Operador ternario anidado: si micro es null muestra "-", si no "Si"/"No"
                    tvMicrochip.setText("📡 Microchip: "
                            + (micro != null ? (micro ? "Sí" : "No") : "-"));

                    tvVacunas.setText(vacunasStr.toString().trim());
                    tvCondiciones.setText(condStr.toString().trim());
                    tvObservaciones.setText(obs);

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


    // Intenta cargar la foto desde Storage
    // Si existe la muestra con Glide; si no, muestra el icono de camara
    private void cargarFotoDesdeStorage() {
        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // Glide gestiona descarga en segundo plano, cache y gestion de memoria
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(imgMascota);
                    layoutSinFoto.setVisibility(View.GONE);
                })
                .addOnFailureListener(e ->
                        layoutSinFoto.setVisibility(View.VISIBLE));
    }


    // Sube la foto tomada a Firebase Storage
    // Como storageRef apunta siempre al mismo archivo, sobreescribe la foto anterior
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


    // Comprueba el permiso de camara y abre la camara o lo solicita segun corresponda
    private void pedirOUsarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara();
        } else {
            pedirPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    // Crea el archivo temporal y lanza la camara
    private void lanzarCamara() {
        // Nombre unico por mascota para que varias fichas abiertas no se sobreescriban
        File foto = new File(getFilesDir(), "fotos/temp_" + idMascota + ".jpg");
        foto.getParentFile().mkdirs();

        // FileProvider convierte la ruta en una Uri segura para compartir con la camara
        // La cadena ".fileprovider" debe coincidir con lo declarado en AndroidManifest
        fotoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", foto);
        tomarFotoLauncher.launch(fotoUri);
    }


    // Formulario de edicion de la mascota (solo MASTER)
    // Recibe los datos actuales del documento para pre-rellenar los campos
    private void mostrarDialogEditar(Map<String, Object> datos) {
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
        Boolean microActual = (Boolean) datos.get("microchip");
        // "microActual != null && microActual" es seguro si microActual es null
        swChip.setChecked(microActual != null && microActual);
        TextView tvSi = new TextView(this);
        tvSi.setText("  Sí");
        tvSi.setTextColor(0xFF4E342E);
        rowChip.addView(tvNo);
        rowChip.addView(swChip);
        rowChip.addView(tvSi);
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

        // Al cambiar la especie actualizamos razas y vacunas dinamicamente
        spEspecie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean esPerro = pos == 0;
                actualizarRazas(acRaza, esPerro);
                // null porque al cambiar especie no tiene sentido mantener marcas anteriores
                actualizarVacunasEditar(layoutVac, esPerro, null);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        actualizarRazas(acRaza, esPerroInicial);

        // El listener de "Guardar" va despues del show() para controlar
        // si el dialogo se cierra o no segun haya errores de validacion
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

            // Recorremos cada fila del layoutCond buscando el EditText dentro
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

            // merge() para no borrar campos no incluidos en el update (como ownerId)
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


    // Popup de confirmacion de borrado
    // Borra tanto el documento de Firestore como la foto de Storage
    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar mascota")
                .setMessage("¿Estás seguro?\nEsta acción no se puede deshacer.")
                .setPositiveButton("Sí, eliminar", (d, w) -> {
                    // Si no hay foto Firebase lo ignora sin error
                    storageRef.delete();
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


    // ── Helpers ──────────────────────────────────────────────

    // Actualiza las sugerencias de raza segun la especie
    private void actualizarRazas(AutoCompleteTextView ac, boolean esPerro) {
        ac.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                esPerro ? RAZAS_PERRO : RAZAS_GATO));
    }

    // Reemplaza los checkboxes de vacunas segun la especie
    // Si marcadas es null todos los checkboxes empiezan desmarcados
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

    // Añade una fila [campo de texto] [boton X] al contenedor de condiciones
    // valor es el texto inicial; vacio para condiciones nuevas
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

    // Lee un campo de un DocumentSnapshot; devuelve "def" si no existe
    private String getDocStr(DocumentSnapshot doc, String key, String def) {
        String val = doc.getString(key);
        return val != null ? val : def;
    }

    // Lee un campo de un Map; devuelve cadena vacia si no existe o no es String
    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof String) ? (String) val : "";
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