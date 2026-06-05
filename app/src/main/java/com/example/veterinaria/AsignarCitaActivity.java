package com.example.veterinaria;

// El calendario que aparece cuando pulsas "Seleccionar fecha"
import android.app.DatePickerDialog;
// Obligatorio para que cualquier pantalla Android pueda arrancar
import android.os.Bundle;
//todos los elementos visuales
import android.widget.*;

// La "plantilla" base de la que heredan todas las pantallas Android
import androidx.appcompat.app.AppCompatActivity;
// para conectarse a la base de datos en la nube (Firebase)
import com.google.firebase.firestore.FirebaseFirestore;
// Cuando Firebase nos devuelve varios resultados, cada resultado
// es un "QueryDocumentSnapshot". Es como una fila de una tabla Excel
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;


// Esta clase ES la pantalla de asignar citas
// "extends AppCompatActivity" significa que hereda todo el comportamiento
// de una pantalla normal de Android, si no, sería solo un archivo Java
// sin ninguna capacidad de mostrarse en el móvil
public class AsignarCitaActivity extends AppCompatActivity {


    // ─────────────────────────────────────────
    // VARIABLES DE PANTALLA
    // ────────────────────────────────────────
    // El desplegable de mascotas. "Spinner" es el nombre que Android
    // le da a los desplegables
    private Spinner spinnerPaciente;
    // El desplegable de horas disponibles
    private Spinner spinnerHora;
    // El campo donde aparece el email del dueño
    // se rellena solo cuando eliges una mascota
    private EditText etCliente;

    // El campo donde aparece la fecha elegida en el calendario
    // también está bloqueado, no puedes escribir a mano,
    // solo se rellena cuando usas el DatePicker
    private EditText etFecha;

    //motivo de la cita
    private EditText etMotivo;

    // Los dos botones de la pantalla
    private Button btnSeleccionarFecha;
    private Button btnGuardarCita;
    // conexión a Firebase como database
    private FirebaseFirestore db;


    // ─────────────────────────────────────────
    // LISTAS PARA LAS MASCOTAS
    // ─────────────────────────────────────────
    // Cuando el vet elige una mascota del desplegable,
    // necesitamos saber TRES cosas de esa mascota:
    //   1. Su nombre  (para mostrarlo en el spinner)
    //   2. Su ID de Firebase  (para guardar la cita correctamente)
    //   3. El UID de su dueño  (para buscar su email después)
    //
    // No podemos guardar esas tres cosas en una sola lista,
    // así que uso TRES listas
    // Ejemplo:
    //
    //   posición 0 de cada lista →  "Maxy"    /  "abc123"  /  "uid_dueño_1"
    //   posición 1 de cada lista  →  "Lunita"  /  "def456"  /  "uid_dueño_2"
    //   posición 2 de cada lista  →  "Tobysito"  /  "ghi789"  /  "uid_dueño_2"
   //es final para que siempre exista, aunque le podemos añadir y quitar elementos
    private final List<String> nombresMascotas = new ArrayList<>();
    private final List<String> idsMascotas     = new ArrayList<>();
    private final List<String> emailsDuenos    = new ArrayList<>();


    // adapter es el intermediario entre los datos y el elemento visual
    // porque android no sabe mostrar una lista de Java directamente en un Spinner
    //el adapter es como un traductor entre listas y spinner
    private ArrayAdapter<String> adapterMascotas;


    // recordemos que el onCreate solo se ejecuta UNA vez, al abrir la pantalla
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // "super" llama al método del padre (AppCompatActivity)
        // es obligatorio
        super.onCreate(savedInstanceState);

       //asignamos el xml
        setContentView(R.layout.activity_asignar_cita);

        // Iniciamos la conexión con Firebase
        // "getInstance()" significa: "dame la conexión que ya existe, no crees una nueva".
        //asi no creamos una conexión nueva
        db = FirebaseFirestore.getInstance();

        //vincular las variables java con los elementos del xml por su id
        spinnerPaciente     = findViewById(R.id.spinnerPaciente);
        spinnerHora         = findViewById(R.id.spinnerHora);
        etCliente           = findViewById(R.id.etCliente);
        etFecha             = findViewById(R.id.etFecha);
        etMotivo            = findViewById(R.id.etMotivo);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnGuardarCita      = findViewById(R.id.btnGuardarCita);

        // Rellenamos el spinner de horas con los horarios disponibles
        configurarSpinnerHoras();

        // Cargamos las mascotas desde Firebase y las metemos en el spinner
        // Esto es asíncrono, es decir la app sigue funcionando mientras espera la respuesta
        cargarMascotas();

        // Cuando el usuario pulse "Seleccionar fecha", abrimos el calendario
        // v -> es una forma corta de decir: "cuando pase esto, haz esto otro"
        //al pulsar en selecionar fecha, abre el calendario
        btnSeleccionarFecha.setOnClickListener(v -> abrirCalendario());

        // Cuando pulse Guardar cita, ejecutamos la lógica de guardar.
        btnGuardarCita.setOnClickListener(v -> guardarCita());
    }


    // ─────────────────────────────────────────
    // SPINNER DE HORAS
    // Rellena el desplegable con franjas de 9:00 a 17:00
    // cada 30 minutos
    // ─────────────────────────────────────────
    private void configurarSpinnerHoras() {

        // Creo la lista donde meter todas las horas
        List<String> horas = new ArrayList<>();

        // Recorro los números del 9 al 17 (las horas de apertura de PetCare)
        //si fuera para una empresa real sería más conveniente crear una constante con las horas
        //para que pueda cambiarse en el código más fácilmente en caso de que el negocio
        //modifique sus horas de apertura
        for (int h = 9; h <= 17; h++) {

            // Añadimos la hora en punto
            // String.format con "%02d" significa: muestra el número con 2 dígitos
            // rellenando con cero si hace falta. Así el 9 se convierte en 09
            horas.add(String.format(Locale.getDefault(), "%02d:00", h));

            // Añadimos la media hora, PERO no la del 17:30
            if (h < 17) {
                horas.add(String.format(Locale.getDefault(), "%02d:30", h));
            }
        }

        // Creamos el adaptador que conecta la lista de horas con el spinner visual
        // "android.R.layout.simple_spinner_item" es un diseño de fila que ya
        // viene incluido en Android
        ArrayAdapter<String> adapterHoras = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                horas
        );

        // Le decimos qué diseño usar para el desplegable cuando se abre hacia abajo
        // Es lo mismo que el anterior pero con más espacio para que sea cómodo de pulsar
        adapterHoras.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        // Asignamos el adaptador al spinner. En este momento el spinner
        // ya muestra las horas correctamente en pantalla
        spinnerHora.setAdapter(adapterHoras);
    }


    // ─────────────────────────────────────────
    // CARGAR MASCOTAS
    // Descarga todas las mascotas de Firebase y rellena
    // el spinner de pacientes con sus nombres.
    // ─────────────────────────────────────────
    private void cargarMascotas() {

        // Creamos el adaptador VACÍO y lo asignamos ya al spinner.
        // ¿Por qué vacío si todavía no tenemos datos? Porque así el spinner
        // ya existe y funciona. Cuando lleguen los datos de Firebase,
        // simplemente notificamos al adaptador y se actualiza solo
        adapterMascotas = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                nombresMascotas
        );
        adapterMascotas.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerPaciente.setAdapter(adapterMascotas);

        // Pedimos a Firebase TODOS los documentos de la colección "mascotas".
        // ".get()" lanza la consulta (como en sql un GET * FROM mascotas)
        // La respuesta llega de forma asíncrona:
        // el programa no se queda bloqueado esperando, sigue ejecutándose
        // y cuando llega la respuesta se ejecuta el bloque "addOnSuccessListener"
        db.collection("mascotas")
                .get()
                .addOnSuccessListener(snapshot -> {

                    // Vaciamos las tres listas antes de rellenarlas.
                    // Si no lo hiciéramos y el método se llama dos veces
                    // los datos se acumularían y habría duplicados
                    nombresMascotas.clear();
                    idsMascotas.clear();
                    emailsDuenos.clear();

                    // "snapshot" contiene todos los documentos devueltos
                    // Lo recorremos uno a uno como si fuera un excel fila a fila
                    //recordemos que Firebase es de tipo noSql
                    for (QueryDocumentSnapshot doc : snapshot) {

                        // Leemos el campo "nombre" del documento actual
                        String nombre  = doc.getString("nombre");

                        // Leemos el campo "ownerId" que es el UID del dueño
                        // y lo uso después para buscar su email
                        String ownerId = doc.getString("ownerId");

                        // Solo añadimos la mascota si tiene nombre
                        if (nombre != null) {
                            nombresMascotas.add(nombre);

                            // doc.getId() devuelve el ID único que Firebase
                            // genera automáticamente para cada documento
                            // Lo necesitaremos al guardar la cita
                            idsMascotas.add(doc.getId());

                            // Si la mascota no tiene dueño asignado, guardamos
                            // una cadena vacía para no romper la lista paralela
                            // porque las tres listas SIEMPRE deben tener
                            // el mismo número de elementos como vimos en el ejemplo
                            //de arriba
                            emailsDuenos.add(ownerId != null ? ownerId : "");
                        }
                    }

                    // Le decimos al adaptador: "los datos han cambiado, actualízate".
                    // Sin esta línea el spinner no se enteraría de que hay datos nuevos
                    // y seguiría mostrándose vacío aunque las listas estén llenas
                    adapterMascotas.notifyDataSetChanged();

                    // Ahora que el spinner ya tiene datos le ponemos el detector
                    // de selección. Lo hacemos aquí y no antes porque si el usuario
                    // eligiera una mascota antes de que carguen los datos,
                    // las listas estarían vacías y el programa petaría
                    spinnerPaciente.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {

                                // Este método se ejecuta CADA VEZ que el usuario
                                // elige una opción diferente del desplegable
                                @Override
                                public void onItemSelected(AdapterView<?> parent,
                                                           android.view.View view, int position, long id) {

                                    // "position" es el número de fila elegida (0, 1, 2...).
                                    // Con ese número sacamos el UID del dueño de nuestra
                                    // lista paralela
                                    String ownerId = emailsDuenos.get(position);

                                    // Con ese UID buscamos el email real del dueño
                                    // en la colección "users" de Firebase.
                                    buscarEmailDueno(ownerId);
                                }

                                // Este método es obligatorio aunque no lo usemos
                                // Se llamaría si el spinner se quedara sin ninguna
                                // opción seleccionada, cosa que no ocurre en nuestro caso
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                })

                // Si algo falla (sin internet, error de Firebase, etc.)
                // mostramos un Toast con el mensaje de error
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error cargando mascotas: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    // ─────────────────────────────────────────
    // BUSCAR EMAIL DEL DUEÑO
    // Dado el UID del dueño, busca su email en la
    // colección "users" y lo pone en el campo etCliente
    // ─────────────────────────────────────────
    private void buscarEmailDueno(String uid) {

        // Si la mascota no tiene dueño asignado (ownerId vacío o nulo)
        // simplemente dejamos el campo vacío y no hacemos nada más
        if (uid == null || uid.isEmpty()) {
            etCliente.setText("");
            return;
        }

        // Buscamos en Firebase el documento de la colección "users"
        // cuyo ID sea exactamente ese UID
        // cuando se registra un usuario, guardamos su documento con su UID como ID
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {

                    // Leemos el campo "email" del documento encontrado
                    String email = doc.getString("email");

                    // Si encontramos el email lo ponemos en el campo
                    // Si por algún motivo no hay email
                    // ponemos el UID como valor de emergencia para que
                    // el campo no quede vacío sin explicación
                    etCliente.setText(email != null ? email : uid);
                })

                // Si falla la consulta (sin internet u otra cosa)
                // ponemos el UID en el campo como fallback
                .addOnFailureListener(e -> etCliente.setText(uid));
    }


    // ─────────────────────────────────────────
    // ABRIR CALENDARIO
    // Muestra el selector de fecha de Android
    // Cuando el usuario elige un día, lo escribe en etFecha
    // ─────────────────────────────────────────
    private void abrirCalendario() {

        // Cogemos la fecha de hoy para que el calendario
        // arranque en el mes y día actuales por defecto
        // "getInstance()" nos devuelve un objeto Calendar con la fecha/hora actual
        Calendar cal = Calendar.getInstance();

        // Creamos el diálogo del calendario
        // Los tres últimos parámetros son el año, mes y día iniciales
        // La lambda "(view, year, month, day) ->" es la función que se ejecuta
        // cuando el usuario confirma una fecha
        new DatePickerDialog(this,
                (view, year, month, day) ->

                        // Formateamos la fecha como "dia/mes/año" y la metemos en etFecha
                        //  "month" empieza en 0, no en 1.
                        // Enero es 0, por eso sumo 1
                        etFecha.setText(day + "/" + (month + 1) + "/" + year),

                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)

                // Sin este .show() el diálogo se crearía pero no aparecería en pantalla.
        ).show();
    }


    // ─────────────────────────────────────────
    // GUARDAR CITA
    // Recoge todos los datos del formulario, los valida
    // y los guarda en la colección "citas" de Firebase
    // ─────────────────────────────────────────
    private void guardarCita() {

        // Comprobación previa: si no hay mascotas cargadas no tiene sentido
        // continuar porque no habría nada seleccionado en el spinner
        // Esto puede pasar si Firebase tardó mucho en responder y el
        // veterinario pulsó guardar demasiado rápido
        if (nombresMascotas.isEmpty()) {
            Toast.makeText(this, "No hay mascotas disponibles",
                    Toast.LENGTH_SHORT).show();
            // "return" sale del método aquí. No ejecuta nada de lo que viene después.
            return;
        }

        // Obtenemos el número de posición que está seleccionada en el spinner
        // Si el usuario eligió la segunda opción, valdrá 1 (empieza en 0)
        int pos = spinnerPaciente.getSelectedItemPosition();

        // Con esa posición buscamos el nombre y el ID de la mascota
        // en las listas paralelas que rellenamos antes
        String paciente  = nombresMascotas.get(pos);
        String mascotaId = idsMascotas.get(pos);

        // Recogemos el resto de campos del formulario
        // .getText().toString() convierte el contenido del campo a texto
        // .trim() elimina los espacios en blanco del principio y el final
        // porque si el usuario escribe " hola " queremos guardar "hola"
        String cliente = etCliente.getText().toString().trim();
        String fecha   = etFecha.getText().toString().trim();
        String hora    = spinnerHora.getSelectedItem().toString();
        String motivo  = etMotivo.getText().toString().trim();

        // Validación: fecha y motivo son obligatorios
        // La mascota y la hora siempre tienen valor porque son spinners
        // (siempre hay algo seleccionado)
        // La fecha y el motivo sí pueden
        // estar vacíos si el usuario no los rellenó
        if (fecha.isEmpty() || motivo.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Preparamos el "paquete de datos" que vamos a guardar en Firebase
        //  Map es un diccionario: cada dato tiene una clave y un valor
        // Firebase guarda este Map como un documento con esos campos
        Map<String, Object> cita = new HashMap<>();
        cita.put("pacienteNombre", paciente);
        cita.put("mascotaId",      mascotaId);
        cita.put("clienteEmail",   cliente);
        cita.put("fecha",          fecha);
        cita.put("hora",           hora);
        cita.put("motivo",         motivo);

        // Guardamos el documento en la colección "citas" de Firebase
        // ".add(cita)" crea un documento nuevo con un ID automático
        // No usamos ".document(id).set()" porque no queremos elegir el ID
        // preferimos que Firebase lo genere solo para evitar duplicados
        db.collection("citas").add(cita)
                .addOnSuccessListener(ref -> {

                    // Si todo fue bien mostramos un mensaje de éxito
                    Toast.makeText(this, "Cita guardada correctamente",
                            Toast.LENGTH_LONG).show();

                    // Limpiamos el formulario para que el veterinario
                    // pueda crear otra cita sin borrar todo a mano
                    limpiarCampos();
                })

                // Si algo falla mostramos el mensaje de error de Firebase
                // Así si hay algún problema podemos saber qué pasó exactamente
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    // ─────────────────────────────────────────
    // LIMPIAR CAMPOS
    // Resetea el formulario después de guardar una cita
    // dejándolo listo para crear la siguiente
    // ─────────────────────────────────────────
    private void limpiarCampos() {

        // Vaciamos los campos de texto
        etFecha.setText("");
        etMotivo.setText("");
        etCliente.setText("");

        // Volvemos los spinners a la primera opción (posición 0)
        // para que no se quede seleccionada la mascota u hora anterior
        spinnerPaciente.setSelection(0);
        spinnerHora.setSelection(0);
    }
}