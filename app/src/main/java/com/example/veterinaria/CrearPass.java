package com.example.veterinaria;

import android.os.Bundle;

import android.view.View;

// Los campos de texto de la pantalla
import android.widget.EditText;

import android.widget.Toast;

// La base de toda pantalla Android
import androidx.appcompat.app.AppCompatActivity;

// Para crear cuentas con email y contraseña en Firebase
import com.google.firebase.auth.FirebaseAuth;

// Para guardar los datos del usuario en la base de datos
import com.google.firebase.firestore.FirebaseFirestore;

// HashMap es el diccionario que usamos para preparar los datos
// antes de mandarlos a Firebase. Funciona con pares clave-valor
import java.util.HashMap;
import java.util.Map;

// Para poder usar expresiones regulares (patrones de texto)
// Las usamos para validar el formato del DNI
import java.util.regex.Pattern;

// Esta clase es la pantalla de "Crear cuenta"
// Desde aquí un usuario nuevo puede registrarse en la app
// Solo permite crear cuentas de tipo USER (duenos de mascotas)
// Los MASTER (veterinarios) solo se pueden crear desde el panel del veterinario
public class CrearPass extends AppCompatActivity {

    // Los siete campos de texto del formulario de registro
    // Los declaramos aquí arriba para que sean accesibles desde cualquier
    // método de la clase, no solo desde onCreate
    private EditText etNombre, etPrefijo, etTelefono, etDni,
            etEmail, etNuevaPassword, etRepetirPassword;

    // La conexión al sistema de login de Firebase
    // "mAuth" viene de "my Authentication"
    // A través de esta variable creamos la cuenta con email y contraseña
    private FirebaseAuth mAuth;

    // La conexión a la base de datos de Firebase
    // Aquí guardamos los datos del usuario (nombre, DNI, etc.)
    // una vez que la cuenta de login ya se ha creado correctamente
    private FirebaseFirestore db;


    // ─────────────────────────────────────────
    //      ARRANQUE DE LA PANTALLA
    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cargamos el diseño XML de esta pantalla
        setContentView(R.layout.activity_main2);

        // Iniciamos las conexiones con Firebase
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Vinculamos cada variable con su campo de texto del XML
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
    // Este método se llama automáticamente cuando el usuario pulsa
    // el botón "Crear cuenta" del XML
    public void crearPassword(View view) {
        // Recogemos el contenido de cada campo de texto
        // .getText().toString() convierte el contenido a texto plano
        // .trim() elimina los espacios en blanco del principio y el final
        String nombre   = etNombre.getText().toString().trim();
        String prefijo  = etPrefijo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        // .toUpperCase() convierte el DNI a mayúsculas
        // Lo hacemos aquí para que el usuario no tenga que preocuparse
        // de si escribe la letra en mayúscula o minúscula
        String dni      = etDni.getText().toString().trim().toUpperCase();

        String email    = etEmail.getText().toString().trim();

        // A la contraseña NO le aplicamos trim()
        // Una contraseña con espacios es válida
        String pass1    = etNuevaPassword.getText().toString();
        String pass2    = etRepetirPassword.getText().toString();


        // ── VALIDACIONES ──────────────────────
        // Antes de mandar nada a Firebase, comprobamos que todo esté correcto
        // Hacemos las validaciones de más fácil a más compleja
        // Cada "return" sale del método si algo falla, sin continuar

        // Comprobamos que ningún campo esté vacío
        if (nombre.isEmpty() || telefono.isEmpty() || dni.isEmpty()
                || email.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validamos el DNI con nuestra función de más abajo
        if (!validarDni(dni)) {
            Toast.makeText(this, "DNI no válido (formato: 12345678A)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validamos el formato del email usando un patrón que viene incluido en Android
        // Patterns.EMAIL_ADDRESS es una expresión regular que comprueba que el texto
        // .matcher(email).matches() aplica ese patrón
        // tiene formato email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "El email no tiene un formato válido",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Comprobamos que la contraseña no sea igual al email ni lo contenga
        if (pass1.contains(email) || email.contains(pass1)) {
            Toast.makeText(this, "La contraseña no puede ser igual al email",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Comprobamos que las dos contraseñas escritas sean iguales.
        // No podemos usar "==" para comparar Strings en Java porque eso
        // compara la dirección de memoria, no el contenido
        if (!pass1.equals(pass2)) {
            Toast.makeText(this, "Las contraseñas no coinciden",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase exige contraseñas de mínimo 6 caracteres
        // .length() devuelve el número de caracteres del texto
        if (pass1.length() < 6) {
            Toast.makeText(this,
                    "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Montamos el número de teléfono completo con el prefijo.
        // Si el usuario dejó el prefijo vacío, usamos +34 (España) por defecto
        // El "+" lo añadimos nosotros automáticamente en el código para que
        // en el formulario el usuario solo tenga que escribir los números
        String telefonoCompleto = "+" + (prefijo.isEmpty() ? "34" : prefijo) + telefono;


        // ── CREAR CUENTA EN FIREBASE AUTH ─────────────
        // Si todas las validaciones pasaron, creamos la cuenta.
        // Este proceso tiene DOS pasos que deben ocurrir en orden:
        //   1. Crear la cuenta de login en Firebase Authentication
        //   2. Guardar los datos del usuario en Firestore

        // ya que Firebase Auth solo guarda email y contraseña
        // El nombre, DNI, teléfono, hay que guardarlos por separado en Firestore

        // Paso 1: crear la cuenta de login con email y contraseña
        // Esto es asíncrono, la app no se bloquea mientras espera
        mAuth.createUserWithEmailAndPassword(email, pass1)
                .addOnSuccessListener(authResult -> {

                    // Si Firebase creó la cuenta correctamente, llegamos aquí
                    // "authResult.getUser().getUid()" nos da el identificador único
                    // que Firebase generó para este usuario
                    // Lo necesitamos para guardar el documento en Firestore con ese mismo ID
                    // así luego podemos encontrar los datos de cualquier usuario
                    String uid = authResult.getUser().getUid();

                    // Preparamos el paquete de datos que vamos a guardar en Firestore
                    // HashMap es un diccionario: clave -> valor
                    // Esto se convertirá en un documento de Firebase con estos campos
                    Map<String, Object> user = new HashMap<>();
                    user.put("email",    email);
                    user.put("nombre",   nombre);
                    user.put("telefono", telefonoCompleto);
                    user.put("dni",      dni);

                    // Todos los usuarios creados desde esta pantalla son de tipo USER
                    // El tipo MASTER solo lo puede asignar un veterinario desde su panel
                    user.put("tipo", "USER");

                    // Paso 2: guardamos el documento en Firestore
                    // ".document(uid)" crea el documento con el UID como ID
                    // Así el ID del documento en Firestore coincide con el UID de Firebase Auth
                    // lo que nos permite buscar los datos de un usuario conociendo solo su UID
                    // ".set(user)" escribe todos los campos del Map en ese documento
                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(v -> {

                                // Si todo fue bien, avisamos al usuario y cerramos esta pantalla
                                Toast.makeText(this,
                                        "Cuenta creada correctamente",
                                        Toast.LENGTH_SHORT).show();

                                // finish() cierra esta Activity y vuelve a la pantalla anterior
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    // Si falla el guardado en Firestore avisamos con el error
                                    // Esto puede ocurrir si hay problemas de conexión o de permisos
                                    Toast.makeText(this,
                                            "Error al guardar datos: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        // Si Firebase Auth no pudo crear la cuenta avisamos con el error
                        // Ejemplos de error: el email ya está registrado, sin internet, etc
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    // ─────────────────────────────────────────
    // VALIDAR DNI ESPAÑOL
    // Comprueba que el DNI tenga el formato correcto:
    // 8 números seguidos de 1 letra, y que esa letra
    // sea la que le corresponde según la tabla oficial
    // ─────────────────────────────────────────
    // Devuelve "true" si el DNI es válido o "false" si no lo es
    //Como ampliación a futuro se podría poner NIE o PASAPORTE
    private boolean validarDni(String dni) {

        // Primero comprobamos el formato con una expresión regular
        // "[0-9]{8}" significa exactamente 8 dígitos del 0 al 9
        // [A-Z] significa"exactamente 1 letra mayúscula.
        // Si el DNI no tiene exactamente ese formato, devolvemos false directamente
        if (!Pattern.matches("[0-9]{8}[A-Z]", dni)) return false;

        // La tabla oficial de letras del DNI español.
        // el DNI se calcula dividiendo el número entre 23 y cogiendo el resto de la división
        // Ese resto (del 0 al 22) es la posición en esta cadena de letras
        String letras = "TRWAGMYFPDXBNJZSQVHLCKE";

        // Cogemos solo los 8 primeros caracteres (los números) y los convertimos a entero
        // substring(0, 8) coge desde la posición 0 hasta la 7 (el 8 no está incluido)
        int numero = Integer.parseInt(dni.substring(0, 8));

        // Calculamos cuál debería ser la letra con el operador módulo (%)
        // El módulo devuelve el RESTO de una división
        //ese numero es la posicion en "letras"
        char letraEsperada = letras.charAt(numero % 23);

        // Comparamos la letra que calculamos con la que escribió el usuario
        // dni.charAt(8) coge el carácter en la posición 8, que es la letra del DNI
        // Si coinciden devuelve true (DNI válido) si no devuelve false
        return dni.charAt(8) == letraEsperada;
    }


    // ─────────────────────────────────────────
    // BOTÓN VOLVER
    // ─────────────────────────────────────────
    // Se llama desde el XML con android:onClick="volver"
    // finish() cierra esta pantalla y vuelve al login
    public void volver(View view) {
        finish();
    }
}