package com.example.moviles_eventia.ui

import android.app.KeyguardManager
import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.ExternalResource

/**
 * Algunos dispositivos MIUI bloquean el arranque de ActivityScenario desde
 * segundo plano. Abre primero la actividad vacía del manifiesto de pruebas
 * mediante el shell autorizado de UiAutomation, sin iniciar MainActivity,
 * acceder a una cuenta ni modificar permisos o datos del dispositivo.
 */
class PrimerPlanoTestRule : ExternalResource() {
    private var pantallaEncendida: ActivityLifecycleCallback? = null

    override fun before() {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val bloqueo = instrumentacion.targetContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        check(!bloqueo.isKeyguardLocked) {
            "Desbloquea el teléfono antes de ejecutar las pruebas de interfaz."
        }
        val paquete = instrumentacion.targetContext.packageName
        val callback = ActivityLifecycleCallback { actividad, etapa ->
            if (actividad.packageName == paquete &&
                etapa in listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED)) {
                // Solo la ventana de prueba: no cambia el tiempo de bloqueo global.
                actividad.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        pantallaEncendida = callback
        instrumentacion.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback)
        }
        val comando = "am start -W -a android.intent.action.MAIN " +
            "-c android.intent.category.LAUNCHER -n $paquete/androidx.activity.ComponentActivity"
        val resultado = instrumentacion.uiAutomation.executeShellCommand(comando).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }
        check(resultado.contains("Status: ok")) {
            "No se pudo abrir la actividad de prueba en primer plano: $resultado"
        }
        instrumentacion.waitForIdleSync()
    }

    override fun after() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            pantallaEncendida?.let {
                ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(it)
            }
            pantallaEncendida = null
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filter { it.javaClass == ComponentActivity::class.java }
                .toList().forEach { it.finish() }
        }
    }
}
