
package com.example.veterinaria;

// Esta es la pantalla de login, la primera que ve el usuario al abrir la app

// Para poder navegar a otras pantallas (como el panel del veterinario)
import android.content.Intent;

// SharedPreferences es el bloc de notas del móvil
// Sirve para guardar datos pequeños de forma permanente en el dispositivo
// Los usamos para recordar cuántos intentos fallidos lleva el usuario
// y si la cuenta está bloqueada, incluso si cierras y vuelves a abrir la app
import android.content.SharedPreferences;

// Obligatorio para que la pantalla arranque
import android.os.Bundle;

// Para usar "View" como parámetro en métodos llamados desde botones del XML
import android.view.View;

// Los elementos visuales que usamos en esta pantalla
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// EdgeToEdge permite que el contenido de la app llegue hasta los bordes
// de la pantalla, por debajo de la barra de estado y la barra de navegación
import androidx.activity.EdgeToEdge;

// La base de toda pantalla Android
import androidx.appcompat.app.AppCompatActivity;

// Insets son los "márgenes" que ocupa el sistema (barra de estado, barra de navegación)
// Los necesitamos para que nuestro contenido no quede tapado por ellos
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Para el login con email y contraseña de Firebase
import com.google.firebase.auth.FirebaseAuth;

// Para consultar en la base de datos si el usuario es MASTER o USER
import com.google.firebase.firestore.FirebaseFirestore;


// Esta clase ES la pantalla de inicio de sesión
// Es la primera pantalla que ve el usuario al abrir la app
// Desde aquí se puede iniciar sesión o ir a crear una cuenta nueva
public class Inicio extends AppCompatActivity {

    // La conexión al sistema de login de Firebase
    private FirebaseAuth mAuth;

    // El "bloc de notas" donde guardamos los intentos fallidos y el bloqueo
    // SharedPreferences guarda datos en el propio dispositivo (no en la nube)
    // Es como un archivo de configuración que persiste aunque cierres la app
    private SharedPreferences prefs;

    // El número máximo de intentos antes de bloquear la cuenta
    // "static final" significa que es una constante: nunca cambia y es compartida
    // por toda la clase
    private static final int MAX_INTENTOS = 3;

    // Cuánto tiempo dura el bloqueo en milisegundos
    // son milisegundos (1 segundo = 1000 milisegundos).
    // 5 * 60 * 1000 = 300.000 milisegundos = 5 minutos.
    private static final long TIEMPO_BLOQUEO_MS = 5 * 60 * 1000;


    // ─────────────────────────────────────────
    // ARRANQUE DE LA PANTALLA
    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge.enable hace que la app se extienda por toda la pantalla,
        // incluyendo la zona de la barra de estado (arriba) y la de navegación (abajo)
        EdgeToEdge.enable(this);

        // el xml
        setContentView(R.layout.activity_main);

        // Esto ajusta el padding (espacio interior) del layout principal
        // para que el contenido no quede tapado por la barra de estado
        // o la barra de navegación del sistema. Sin esto
        // el título podría quedar a medias debajo de la barra de batería
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Iniciamos la conexión con Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Iniciamos SharedPreferences con el nombre "login_prefs".
        // Este nombre es como el nombre del archivo donde se guardarán los datos
        // MODE_PRIVATE significa que solo nuestra app puede leer estos datos
        // ninguna otra app del móvil tiene acceso a ellos
        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);

        // Vinculamos el botón de "Primera vez / Crear cuenta" con su elemento del XML
        Button btnPrimeraVez = findViewById(R.id.btnPrimeraVez);

        // Al pulsarlo, abrimos la pantalla de crear cuenta
        // desde la pantalla Inicio, ve a la pantalla CrearPass
        btnPrimeraVez.setOnClickListener(v -> {
            startActivity(new Intent(Inicio.this, CrearPass.class));
        });
    }


    // ─────────────────────────────────────────
    // INICIAR SESIÓN
    // Se llama desde el XML con android:onClick="iniciarSesion"
    // cuando el usuario pulsa el botón de entrar
    // ─────────────────────────────────────────
    public void iniciarSesion(View view) {

        // Cogemos los campos de email y contraseña de la pantalla
        // Los cogemos aquí dentro del método y no arriba como variables de clase
        // porque solo los necesitamos en este método, no en ningún otro
        EditText etUsuario  = findViewById(R.id.etUsuario);
        EditText etPassword = findViewById(R.id.etPassword);

        String email    = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Si alguno de los campos está vacío, avisamos y salimos
        // No tiene sentido intentar el login con campos vacíos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introduce usuario y contraseña",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // System.currentTimeMillis() devuelve los milisegundos transcurridos
        // desde el 1 de enero de 1970. Es la forma estándar en Java de
        // trabajar con el tiempo
        long ahora = System.currentTimeMillis();

        // Leemos de SharedPreferences hasta cuándo está bloqueada la cuenta
        // getLong("bloqueado_hasta", 0) busca el valor guardado con esa clave
        // Si no hay nada guardado (primera vez), devuelve 0 por defecto
        // Guardamos el tiempo de bloqueo como un número de milisegundos
        // para poder compararlo directamente con "ahora"
        long bloqueadoHasta = prefs.getLong("bloqueado_hasta", 0);

        // Comprobamos si la cuenta sigue bloqueada
        // Si "ahora" es menor que "bloqueadoHasta", significa que aún no
        // ha pasado el tiempo de bloqueo y no podemos intentar el login
        if (ahora < bloqueadoHasta) {

            // Calculamos cuántos segundos faltan para que se desbloquee
            // (bloqueadoHasta-ahora) nos da los milisegundos que faltan
            // Dividir entre 1000 lo convierte a segundos
            long segundos = (bloqueadoHasta - ahora) / 1000;

            Toast.makeText(
                    this,
                    "Cuenta bloqueada. Inténtalo en " + segundos + " segundos",
                    Toast.LENGTH_LONG
            ).show();

            // Salimos del método sin intentar el login
            return;
        }

        // Si llegamos aquí, la cuenta no está bloqueada y podemos intentar el login
        // Mandamos el email y la contraseña a Firebase para que los verifique
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    // El login fue correcto. Reseteamos los contadores de intentos
                    // para que la próxima vez que falle empiece desde cero
                    // .edit() abre las prefs en modo edición
                    // .remove() borra las claves indicadas
                    // .apply() guarda los cambios de forma asíncrona (sin bloquear la app)
                    prefs.edit()
                            .remove("intentos_fallidos")
                            .remove("bloqueado_hasta")
                            .apply();

                    // Obtenemos el UID del usuario que acaba de iniciar sesión
                    // Lo necesitamos para buscar en Firestore si es MASTER o USER
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // Consultamos en Firestore el documento del usuario para
                    // saber su tipo de cuenta y redirigirle a la pantalla correcta
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {

                                // Leemos el campo "tipo" del documento del usuario
                                // Puede ser "MASTER" (veterinario) o "USER" (dueño de mascota)
                                String tipo = doc.getString("tipo");

                                // Esta variable "uid1" está declarada pero no se usa
                                // Es un resto de código antiguo que se puede borrar sin problema
                                String uid1 = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                // Redirigimos al usuario a la pantalla que le corresponde según su rol
                                // Si es MASTER va al panel del veterinario con todas las funciones
                                // Si es USER (o cualquier otro tipo) va a la pantalla de sus mascotas
                                if ("MASTER".equals(tipo)) {
                                    startActivity(new Intent(this, PanelVeterinario.class));
                                } else {
                                    startActivity(new Intent(this, Mascotas_usuario.class));
                                }

                                // finish() cierra la pantalla de login para que el usuario
                                // no pueda volver atrás con el botón "Atrás" del móvil
                                // Si no lo pusiéramos, al pulsar atrás volvería al login
                                // aunque ya estuviera dentro de la app
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                        "ERROR Firestore: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })

                .addOnFailureListener(e -> {

                    // El login falló (contraseña incorrecta, usuario no existe, etc.)
                    // Leemos cuántos intentos fallidos lleva acumulados
                    // getInt("intentos_fallidos", 0) devuelve el número guardado
                    // o 0 si es la primera vez que falla
                    int intentos = prefs.getInt("intentos_fallidos", 0);

                    // Sumamos 1 al contador de intentos fallidos
                    intentos++;

                    // Abrimos el editor de SharedPreferences para guardar el nuevo valor
                    // Es como abrir un documento de Word antes de poder escribir en él
                    SharedPreferences.Editor editor = prefs.edit();

                    // Comprobamos si ya alcanzó el máximo de intentos permitidos (3)
                    if (intentos >= MAX_INTENTOS) {

                        // Calculamos hasta cuándo estará bloqueada la cuenta
                        // "ahora + TIEMPO_BLOQUEO_MS" es "el momento actual + 5 minutos"
                        // Guardamos este número en SharedPreferences para recordarlo
                        // aunque el usuario cierre la app y la vuelva a abrir
                        long bloquearHasta = System.currentTimeMillis() + TIEMPO_BLOQUEO_MS;
                        editor.putLong("bloqueado_hasta", bloquearHasta);

                        // Borramos el contador de intentos porque ya no lo necesitamos
                        // la cuenta está bloqueada y el contador empieza de cero cuando se desbloquee
                        editor.remove("intentos_fallidos");

                        // .apply() guarda los cambios en el "bloc de notas" del móvil
                        // Usamos apply() y no commit() porque apply() es asíncrono
                        // (no bloquea la app mientras guarda) y en este caso no necesitamos
                        // esperar a que termine para continuar
                        editor.apply();

                        Toast.makeText(
                                this,
                                "Has fallado 3 veces.\nCuenta bloqueada durante 5 minutos",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        // Todavía no llegó al máximo. Guardamos el nuevo número de intentos
                        // y avisamos al usuario de cuántos le quedan.
                        editor.putInt("intentos_fallidos", intentos);
                        editor.apply();

                        // Mostramos cuántos intentos lleva de los 3 permitidos
                        Toast.makeText(
                                this,
                                "Contraseña incorrecta (" + intentos + " de " + MAX_INTENTOS + ")",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
