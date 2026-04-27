package ni.edu.uam.tarjetaperfil

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ni.edu.uam.tarjetaperfil.ui.theme.TarjetaPerfilTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale

const val LIMITE_DESCRIPCION = 100
const val LIMITE_PUBLICACION = 200
const val LIMITE_COMENTARIO = 200

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TarjetaPerfilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFF1F7)
                ) {
                    AppPerfilSocial()
                }
            }
        }
    }
}

data class Comentario(
    val texto: String,
    val hora: String,
    val usuario: String
)

data class Publicacion(
    val id: Int,
    val texto: String,
    val imagenUri: String? = null,
    val fechaHora: String = "",
    val comentarios: List<Comentario> = emptyList(),
    val tieneMeGusta: Boolean = false,
    val compartida: Boolean = false
)

fun obtenerFechaHoraActual(): String {
    val fechaHoraActual = LocalDateTime.now()
    val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
    return fechaHoraActual.format(formato)
}

fun limpiarUsuario(usuario: String): String {
    val usuarioLimpio = usuario.trim().lowercase()

    return if (usuarioLimpio.startsWith("@")) {
        usuarioLimpio
    } else {
        "@$usuarioLimpio"
    }
}

@Composable
fun AppPerfilSocial() {
    var nombre by rememberSaveable { mutableStateOf("Nombre") }
    var usuario by rememberSaveable { mutableStateOf("@Usuario") }
    var descripcion by rememberSaveable { mutableStateOf("+ Agregar descripción.") }

    var nombreTemporal by rememberSaveable { mutableStateOf(nombre) }
    var usuarioTemporal by rememberSaveable { mutableStateOf(usuario.removePrefix("@")) }
    var descripcionTemporal by rememberSaveable { mutableStateOf("") }

    var nombreFueTocado by rememberSaveable { mutableStateOf(false) }
    var usuarioFueTocado by rememberSaveable { mutableStateOf(false) }
    var descripcionFueTocada by rememberSaveable { mutableStateOf(false) }

    var fotoPerfilUri by rememberSaveable { mutableStateOf<String?>(null) }
    var estaEditandoPerfil by rememberSaveable { mutableStateOf(false) }
    var mensajeErrorPerfil by rememberSaveable { mutableStateOf("") }

    var nuevaPublicacion by rememberSaveable { mutableStateOf("") }
    var nuevaImagenUri by rememberSaveable { mutableStateOf<String?>(null) }
    var mensajeErrorPublicacion by rememberSaveable { mutableStateOf("") }

    var publicaciones by remember {
        mutableStateOf(emptyList<Publicacion>())
    }

    val selectorFotoPerfil = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoPerfilUri = uri.toString()
        }
    }

    val selectorImagenPublicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            nuevaImagenUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EncabezadoPerfilSocial(
            estaEditandoPerfil = estaEditandoPerfil,
            onEditarClick = {
                nombreTemporal = nombre
                usuarioTemporal = usuario.removePrefix("@")
                descripcionTemporal = ""

                nombreFueTocado = false
                usuarioFueTocado = false
                descripcionFueTocada = false

                mensajeErrorPerfil = ""
                estaEditandoPerfil = true
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaPerfil(
                nombre = nombre,
                usuario = usuario,
                descripcion = descripcion,
                nombreTemporal = nombreTemporal,
                usuarioTemporal = usuarioTemporal,
                descripcionTemporal = descripcionTemporal,
                nombreFueTocado = nombreFueTocado,
                usuarioFueTocado = usuarioFueTocado,
                descripcionFueTocada = descripcionFueTocada,
                fotoPerfilUri = fotoPerfilUri,
                estaEditandoPerfil = estaEditandoPerfil,
                mensajeErrorPerfil = mensajeErrorPerfil,
                onCambiarFoto = {
                    selectorFotoPerfil.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onNombreTemporalChange = { nuevoTexto ->
                    nombreTemporal = nuevoTexto.filter { !it.isWhitespace() }
                },
                onUsuarioTemporalChange = { nuevoTexto ->
                    usuarioTemporal = nuevoTexto
                        .removePrefix("@")
                        .filter { !it.isWhitespace() }
                        .lowercase()
                },
                onDescripcionTemporalChange = { nuevoTexto ->
                    if (nuevoTexto.length <= LIMITE_DESCRIPCION) {
                        descripcionTemporal = nuevoTexto
                    }
                },
                onNombreFocus = {
                    if (!nombreFueTocado) {
                        nombreTemporal = ""
                        nombreFueTocado = true
                    }
                },
                onUsuarioFocus = {
                    if (!usuarioFueTocado) {
                        usuarioTemporal = ""
                        usuarioFueTocado = true
                    }
                },
                onDescripcionFocus = {
                    if (!descripcionFueTocada) {
                        descripcionTemporal = ""
                        descripcionFueTocada = true
                    }
                },
                onGuardarPerfil = {
                    val nombreLimpio = nombreTemporal.trim()
                    val usuarioLimpio = usuarioTemporal.trim()
                    val descripcionLimpia = descripcionTemporal.trim()

                    if (
                        nombreLimpio.isBlank() ||
                        usuarioLimpio.isBlank() ||
                        descripcionLimpia.isBlank()
                    ) {
                        mensajeErrorPerfil = "Ningún campo puede quedar vacío."
                    } else {
                        nombre = nombreLimpio
                        usuario = limpiarUsuario(usuarioLimpio)
                        descripcion = descripcionLimpia
                        mensajeErrorPerfil = ""
                        estaEditandoPerfil = false
                    }
                },
                onCancelarPerfil = {
                    nombreTemporal = nombre
                    usuarioTemporal = usuario.removePrefix("@")
                    descripcionTemporal = descripcion

                    nombreFueTocado = false
                    usuarioFueTocado = false
                    descripcionFueTocada = false

                    mensajeErrorPerfil = ""
                    estaEditandoPerfil = false
                }
            )

            if (!estaEditandoPerfil) {
                TarjetaCrearPublicacion(
                    texto = nuevaPublicacion,
                    nuevaImagenUri = nuevaImagenUri,
                    mensajeErrorPublicacion = mensajeErrorPublicacion,
                    onTextoChange = { nuevoTexto ->
                        if (nuevoTexto.length <= LIMITE_PUBLICACION) {
                            nuevaPublicacion = nuevoTexto
                            mensajeErrorPublicacion = ""
                        } else {
                            mensajeErrorPublicacion = "Límite de caracteres alcanzado"
                        }
                    },
                    onSeleccionarImagen = {
                        selectorImagenPublicacion.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAgregarPublicacion = {
                        val textoLimpio = nuevaPublicacion.trim()

                        if (textoLimpio.isNotEmpty() || nuevaImagenUri != null) {
                            val nueva = Publicacion(
                                id = publicaciones.size + 1,
                                texto = textoLimpio,
                                imagenUri = nuevaImagenUri,
                                fechaHora = obtenerFechaHoraActual()
                            )

                            publicaciones = listOf(nueva) + publicaciones
                            nuevaPublicacion = ""
                            nuevaImagenUri = null
                            mensajeErrorPublicacion = ""
                        }
                    }
                )

                Text(
                    text = "Publicaciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF880E4F)
                )

                if (publicaciones.isEmpty()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no se han realizado publicaciones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    publicaciones.forEach { publicacion ->
                        TarjetaPublicacion(
                            publicacion = publicacion,
                            fotoPerfilUri = fotoPerfilUri,
                            nombre = nombre,
                            usuario = usuario,
                            onMeGusta = {
                                publicaciones = publicaciones.map {
                                    if (it.id == publicacion.id) {
                                        it.copy(tieneMeGusta = !it.tieneMeGusta)
                                    } else {
                                        it
                                    }
                                }
                            },
                            onCompartir = {
                                publicaciones = publicaciones.map {
                                    if (it.id == publicacion.id) {
                                        it.copy(compartida = !it.compartida)
                                    } else {
                                        it
                                    }
                                }
                            },
                            onComentar = { comentario ->
                                publicaciones = publicaciones.map {
                                    if (it.id == publicacion.id) {
                                        it.copy(comentarios = it.comentarios + comentario)
                                    } else {
                                        it
                                    }
                                }
                            }
                        )
                    }
                }
            }
        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
@Composable
fun EncabezadoPerfilSocial(
    estaEditandoPerfil: Boolean,
    onEditarClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFFE91E63),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfil Social",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!estaEditandoPerfil) {
                TextButton(
                    onClick = onEditarClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Editar")
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))
            }
        }
    }
}

@Composable
fun TarjetaPerfil(
    nombre: String,
    usuario: String,
    descripcion: String,
    nombreTemporal: String,
    usuarioTemporal: String,
    descripcionTemporal: String,
    nombreFueTocado: Boolean,
    usuarioFueTocado: Boolean,
    descripcionFueTocada: Boolean,
    fotoPerfilUri: String?,
    estaEditandoPerfil: Boolean,
    mensajeErrorPerfil: String,
    onCambiarFoto: () -> Unit,
    onNombreTemporalChange: (String) -> Unit,
    onUsuarioTemporalChange: (String) -> Unit,
    onDescripcionTemporalChange: (String) -> Unit,
    onNombreFocus: () -> Unit,
    onUsuarioFocus: () -> Unit,
    onDescripcionFocus: () -> Unit,
    onGuardarPerfil: () -> Unit,
    onCancelarPerfil: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectorFotoPerfil(
                imagenUri = fotoPerfilUri,
                onSeleccionarImagen = onCambiarFoto
            )

            if (estaEditandoPerfil) {
                OutlinedTextField(
                    value = nombreTemporal,
                    onValueChange = { nuevoTexto ->
                        onNombreTemporalChange(nuevoTexto.filter { !it.isWhitespace() })
                    },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { estado ->
                            if (estado.isFocused && !nombreFueTocado) {
                                onNombreFocus()
                            }
                        },
                    singleLine = true
                )

                OutlinedTextField(
                    value = usuarioTemporal,
                    onValueChange = { nuevoTexto ->
                        onUsuarioTemporalChange(
                            nuevoTexto
                                .removePrefix("@")
                                .filter { !it.isWhitespace() }
                                .lowercase()
                        )
                    },
                    label = { Text("Usuario") },
                    prefix = { Text("@") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { estado ->
                            if (estado.isFocused && !usuarioFueTocado) {
                                onUsuarioFocus()
                            }
                        },
                    singleLine = true
                )

                OutlinedTextField(
                    value = descripcionTemporal,
                    onValueChange = { nuevoTexto ->
                        if (nuevoTexto.length <= LIMITE_DESCRIPCION) {
                            onDescripcionTemporalChange(nuevoTexto)
                        }
                    },
                    label = { Text("Descripción") },
                    supportingText = {
                        Text("${descripcionTemporal.length}/$LIMITE_DESCRIPCION")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { estado ->
                            if (estado.isFocused && !descripcionFueTocada) {
                                onDescripcionFocus()
                            }
                        },
                    minLines = 2
                )

                if (mensajeErrorPerfil.isNotBlank()) {
                    Text(
                        text = mensajeErrorPerfil,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelarPerfil,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onGuardarPerfil,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }
            } else {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF880E4F),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = usuario,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAD1457),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D4037),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TarjetaCrearPublicacion(
    texto: String,
    nuevaImagenUri: String?,
    mensajeErrorPublicacion: String,
    onTextoChange: (String) -> Unit,
    onSeleccionarImagen: () -> Unit,
    onAgregarPublicacion: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )

                Text(
                    text = "Crear publicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF880E4F)
                )
            }

            OutlinedTextField(
                value = texto,
                onValueChange = onTextoChange,
                label = { Text("¿Qué quieres compartir?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                supportingText = {
                    Column {
                        Text("${texto.length}/$LIMITE_PUBLICACION")

                        if (mensajeErrorPublicacion.isNotBlank()) {
                            Text(
                                text = mensajeErrorPublicacion,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )

            VistaPreviaImagen(
                imagenUri = nuevaImagenUri,
                descripcion = "Imagen seleccionada para la publicación"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSeleccionarImagen,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Imagen")
                }

                Button(
                    onClick = onAgregarPublicacion,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63)
                    )
                ) {
                    Text("Publicar")
                }
            }
        }
    }
}

@Composable
fun TarjetaPublicacion(
    publicacion: Publicacion,
    fotoPerfilUri: String?,
    nombre: String,
    usuario: String,
    onMeGusta: () -> Unit,
    onCompartir: () -> Unit,
    onComentar: (Comentario) -> Unit
) {
    val contexto = LocalContext.current
    var comentarioActual by rememberSaveable(publicacion.id) { mutableStateOf("") }
    var mensajeErrorComentario by rememberSaveable(publicacion.id) { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EncabezadoPublicacion(
                fotoPerfilUri = fotoPerfilUri,
                nombre = nombre,
                usuario = usuario,
                fechaHora = publicacion.fechaHora
            )

            if (publicacion.texto.isNotBlank()) {
                Text(
                    text = publicacion.texto,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3E2723)
                )
            }

            if (publicacion.imagenUri != null) {
                val imagenPublicacion by produceState<ImageBitmap?>(
                    initialValue = null,
                    key1 = publicacion.imagenUri
                ) {
                    value = cargarImagenBitmap(contexto, publicacion.imagenUri)
                }

                if (imagenPublicacion != null) {
                    Image(
                        bitmap = imagenPublicacion!!,
                        contentDescription = "Imagen de la publicación",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            val colorMeGusta by animateColorAsState(
                targetValue = if (publicacion.tieneMeGusta) {
                    Color(0xFFE91E63)
                } else {
                    Color(0xFF5D4037)
                },
                label = "colorMeGusta"
            )

            val escalaMeGusta by animateFloatAsState(
                targetValue = if (publicacion.tieneMeGusta) 1.08f else 1f,
                label = "escalaMeGusta"
            )

            val colorCompartir by animateColorAsState(
                targetValue = if (publicacion.compartida) {
                    Color(0xFFE91E63)
                } else {
                    Color(0xFF5D4037)
                },
                label = "colorCompartir"
            )

            val escalaCompartir by animateFloatAsState(
                targetValue = if (publicacion.compartida) 1.05f else 1f,
                label = "escalaCompartir"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onMeGusta,
                    modifier = Modifier
                        .weight(1f)
                        .scale(escalaMeGusta),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorMeGusta
                    )
                ) {
                    Text(
                        text = if (publicacion.tieneMeGusta) "♥ Me gusta" else "♡ Me gusta",
                        fontWeight = if (publicacion.tieneMeGusta) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextButton(
                    onClick = onCompartir,
                    modifier = Modifier
                        .weight(1f)
                        .scale(escalaCompartir),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorCompartir
                    )
                ) {
                    Text(
                        text = if (publicacion.compartida) "Compartido" else "Compartir",
                        fontWeight = if (publicacion.compartida) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF8BBD0))

            SeccionComentarios(
                comentarios = publicacion.comentarios,
                comentarioActual = comentarioActual,
                mensajeErrorComentario = mensajeErrorComentario,
                onComentarioActualChange = { nuevoTexto ->
                    if (nuevoTexto.length <= LIMITE_COMENTARIO) {
                        comentarioActual = nuevoTexto
                        mensajeErrorComentario = ""
                    } else {
                        mensajeErrorComentario = "Límite de caracteres alcanzado"
                    }
                },
                onEnviarComentario = {
                    val comentarioLimpio = comentarioActual.trim()

                    if (comentarioLimpio.isEmpty()) {
                        mensajeErrorComentario = "Debes agregar un comentario."
                    } else {
                        onComentar(
                            Comentario(
                                texto = comentarioLimpio,
                                hora = obtenerFechaHoraActual(),
                                usuario = usuario
                            )
                        )
                        comentarioActual = ""
                        mensajeErrorComentario = ""
                    }
                }
            )
        }
    }
}

@Composable
fun EncabezadoPublicacion(
    fotoPerfilUri: String?,
    nombre: String,
    usuario: String,
    fechaHora: String
) {
    val contexto = LocalContext.current

    val imagenPerfil by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = fotoPerfilUri
    ) {
        value = if (fotoPerfilUri.isNullOrBlank()) {
            null
        } else {
            cargarImagenBitmap(contexto, fotoPerfilUri)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (imagenPerfil == null) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Imagen de perfil",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE4EF))
                    .padding(8.dp),
                tint = Color(0xFFE91E63)
            )
        } else {
            Image(
                bitmap = imagenPerfil!!,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF880E4F)
            )

            Text(
                text = "$usuario · $fechaHora",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SeccionComentarios(
    comentarios: List<Comentario>,
    comentarioActual: String,
    mensajeErrorComentario: String,
    onComentarioActualChange: (String) -> Unit,
    onEnviarComentario: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Comentarios",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF880E4F)
        )

        if (comentarios.isEmpty()) {
            Text(
                text = "Aún no hay comentarios.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            comentarios.forEach { comentario ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF1F7),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = comentario.usuario,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF880E4F)
                        )

                        Text(
                            text = comentario.texto,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E342E)
                        )

                        Text(
                            text = comentario.hora,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = comentarioActual,
                onValueChange = onComentarioActualChange,
                label = { Text("Escribe un comentario") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                supportingText = {
                    Column {
                        Text("${comentarioActual.length}/$LIMITE_COMENTARIO")

                        if (mensajeErrorComentario.isNotBlank()) {
                            Text(
                                text = mensajeErrorComentario,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )

            Button(
                onClick = onEnviarComentario,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE91E63)
                ),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("+")
            }
        }
    }
}

@Composable
fun SelectorFotoPerfil(
    imagenUri: String?,
    onSeleccionarImagen: () -> Unit
) {
    val contexto = LocalContext.current

    val imagenCargada by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imagenUri
    ) {
        value = if (imagenUri.isNullOrBlank()) {
            null
        } else {
            cargarImagenBitmap(contexto, imagenUri)
        }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        if (imagenCargada == null) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE4EF))
                    .border(3.dp, Color(0xFFE91E63), CircleShape)
                    .clickable(onClick = onSeleccionarImagen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Imagen de perfil por defecto",
                    modifier = Modifier.size(70.dp),
                    tint = Color(0xFFE91E63)
                )
            }
        } else {
            Image(
                bitmap = imagenCargada!!,
                contentDescription = "Foto de perfil seleccionada",
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFE91E63), CircleShape)
                    .clickable(onClick = onSeleccionarImagen),
                contentScale = ContentScale.Crop
            )
        }

        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(0xFFE91E63),
            shadowElevation = 4.dp,
            onClick = onSeleccionarImagen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Cambiar foto de perfil",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun VistaPreviaImagen(
    imagenUri: String?,
    descripcion: String
) {
    if (imagenUri == null) return

    val contexto = LocalContext.current

    val imagenCargada by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = imagenUri
    ) {
        value = cargarImagenBitmap(contexto, imagenUri)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFC1E3)),
        contentAlignment = Alignment.Center
    ) {
        if (imagenCargada != null) {
            Image(
                bitmap = imagenCargada!!,
                contentDescription = descripcion,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "Cargando imagen...",
                color = Color(0xFF880E4F),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private suspend fun cargarImagenBitmap(
    contexto: Context,
    uriString: String
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        contexto.contentResolver.openInputStream(uriString.toUri()).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
}