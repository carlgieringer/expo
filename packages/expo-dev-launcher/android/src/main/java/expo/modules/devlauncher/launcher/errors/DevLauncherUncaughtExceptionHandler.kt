package expo.modules.devlauncher.launcher.errors

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Process
import android.util.Log
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

class DevLauncherUncaughtExceptionHandler(
  application: Application,
  private val defaultUncaughtHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
  private val applicationHolder = WeakReference(application)
  private var exceptionWasReported = false
  private var timerTask: TimerTask? = null

  init {
    application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (exceptionWasReported && activity is DevLauncherErrorActivity) {
          timerTask?.cancel()
          timerTask = null
          exceptionWasReported = false
          return
        }
      }

      override fun onActivityStarted(activity: Activity) = Unit

      override fun onActivityResumed(activity: Activity) = Unit

      override fun onActivityPaused(activity: Activity) = Unit

      override fun onActivityStopped(activity: Activity) = Unit

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

      override fun onActivityDestroyed(activity: Activity) = Unit
    })

  }

  override fun uncaughtException(thread: Thread, exception: Throwable) {
    // The same exception can be reported multiple times.
    // We handle only the first one.
    if (exceptionWasReported) {
      return
    }

    exceptionWasReported = true
    Log.e("DevLauncher", "DevLauncher tries to handle uncaught exception.", exception)
    applicationHolder.get()?.let {
      DevLauncherErrorActivity.showFatalError(
        it,
        DevLauncherAppError(exception.message, exception)
      )
    }

    // We don't know if the error screen will show up.
    // That's why we schedule a simple function which will check
    // if the error was handle properly or will fallback
    // to the default exception handler.
    timerTask = Timer().schedule(2000) {
      if (!exceptionWasReported) {
        // Exception was handle, we should suppress error here
        return@schedule
      }

      // The error screen didn't appear in time.
      // We fallback to the default exception handler.
      if (defaultUncaughtHandler != null) {
        defaultUncaughtHandler.uncaughtException(thread, exception)
      } else {
        // This scenario should never occur. It can only happen if there was no defaultUncaughtHandler when the handler was set up.
        Log.e("UNCAUGHT_EXCEPTION", "exception", exception) // print exception in 'Logcat' tab.
        Process.killProcess(Process.myPid())
        exitProcess(0)
      }
    }
  }
}
