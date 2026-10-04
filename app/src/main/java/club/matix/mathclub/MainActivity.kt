package club.matix.mathclub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import club.matix.mathclub.data.Store
import club.matix.mathclub.ui.MatixApp

/** Fully native (Jetpack Compose) host for Matix the Math Club. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        setContent { MatixApp(store) }
    }
}
