import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import com.naman14.timberx.playback.MediaSessionConnection
import com.naman14.timberx.playback.RealMediaSessionConnection
import com.naman14.timberx.playback.TimberMusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import org.koin.core.qualifier.named

const val MAIN = "main"

val mainModule = module {

    factory<ContentResolver> {
        get<Application>().contentResolver
    }

    single<MediaSessionConnection> {
        val component = ComponentName(get(), TimberMusicService::class.java)
        RealMediaSessionConnection(get(), component)
    }

    factory(named(MAIN)) {
        AndroidSchedulers.mainThread()
    }
}