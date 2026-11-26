package com.jpbott.bible
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jpbott.bible.ui.screens.BibleScreen
import com.jpbott.bible.ui.theme.BibleTheme
import com.jpbott.bible.viewmodel.ReaderViewModel
class MainActivity : ComponentActivity() {
    private val model: ReaderViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BibleTheme { BibleScreen(model.state.collectAsStateWithLifecycle().value, model) } }
    }
}
