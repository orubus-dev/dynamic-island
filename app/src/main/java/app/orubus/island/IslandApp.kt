package app.orubus.island

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import app.orubus.island.core.IslandBus
import app.orubus.island.data.SettingsRepo
import app.orubus.island.service.IslandEngine
import app.orubus.island.ui.SetTimerActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class IslandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepo.init(this)
        installRevival()
        // «Таймер от помощника»: выключено — остров не предлагается в списке, когда просят поставить таймер
        IslandBus.scope.launch {
            SettingsRepo.state.map { it.timerFromAssistant }.distinctUntilChanged().collect { on ->
                runCatching {
                    packageManager.setComponentEnabledSetting(
                        ComponentName(this@IslandApp, SetTimerActivity::class.java),
                        if (on) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }
    }

    /**
     * Перехват необработанного сбоя: перед тем как показать системе «Dynamic Island keeps stopping»,
     * движок ставит будильник через полторы секунды — процесс просыпается и службы оживают
     * (Engine.kt). Без этого после сбоя остров оставался мёртвым до перезагрузки.
     */
    private fun installRevival() {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { IslandEngine.scheduleRevival(this@IslandApp) }
            prev?.uncaughtException(t, e)
        }
    }
}
