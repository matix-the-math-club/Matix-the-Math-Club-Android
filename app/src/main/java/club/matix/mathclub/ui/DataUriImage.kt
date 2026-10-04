package club.matix.mathclub.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import club.matix.mathclub.data.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DataUriImage(data: String, modifier: Modifier = Modifier, description: String? = null, contentScale: ContentScale = ContentScale.Crop) {
    var bitmap by remember(data) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(data) {
        bitmap = withContext(Dispatchers.IO) { ImageCodec.decodeDataUri(data)?.asImageBitmap() }
    }
    bitmap?.let { Image(it, contentDescription = description, modifier = modifier, contentScale = contentScale) }
}
