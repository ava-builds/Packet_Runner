import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import com.avabuilds.packetrunner.data.KvNotebookRepository
import com.avabuilds.packetrunner.data.KvSaveRepository
import com.avabuilds.packetrunner.data.KvWorkDataStore
import com.avabuilds.packetrunner.platform.PlatformServices
import com.avabuilds.packetrunner.ui.PacketRunnerApp
import com.avabuilds.packetrunner.web.generated.Res
import kotlinx.browser.document

/** The browser version of Network Town: the same game, saved in the browser's localStorage. Add "?debug" to the link for the level button. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val storage = LocalStorageKeyValueStore
    val services = PlatformServices(
        workStore = KvWorkDataStore(storage),
        sound = WebSound,
        callAlerts = WebCallAlerts,
        isDebug = jsLocationSearch().contains("debug"),
    )
    ComposeViewport(document.getElementById("app")!!) {
        // The page has no Korean font of its own, so a small one is loaded first (see tools/make_korean_font.py)
        var fontsReady by remember { mutableStateOf(false) }
        val fonts = LocalFontFamilyResolver.current
        LaunchedEffect(Unit) {
            try {
                val korean = Res.readBytes("files/NotoSansKR-subset.ttf")
                fonts.preload(FontFamily(Font("NotoSansKR", korean)))
            } catch (e: Throwable) {
                // the game works without it; Korean text then shows as empty boxes
            }
            fontsReady = true
        }
        if (fontsReady) {
            PacketRunnerApp(
                services = services,
                localSaves = KvSaveRepository(storage),
                notebook = KvNotebookRepository(storage),
            )
        }
    }
}
