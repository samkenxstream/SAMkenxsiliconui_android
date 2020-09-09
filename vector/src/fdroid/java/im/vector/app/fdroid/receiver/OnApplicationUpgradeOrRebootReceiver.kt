/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.fdroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import im.vector.app.core.di.HasVectorInjector
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.settings.BackgroundSyncMode
import timber.log.Timber

class OnApplicationUpgradeOrRebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.v("## onReceive() ${intent.action}")
        val appContext = context.applicationContext
        if (appContext is HasVectorInjector) {
            val activeSession = appContext.injector().activeSessionHolder().getSafeActiveSession()
            val preferences = appContext.vectorComponent().vectorPreferences()
            if (activeSession != null) {
                when (preferences.getFdroidSyncBackgroundMode()) {
                    BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY  -> {
                        Timber.i("## Sync: OnBoot Work scheduled to periodically sync")
                        activeSession.startAutomaticBackgroundSync(
                                preferences.backgroundSyncTimeOut().toLong(),
                                preferences.backgroundSyncDelay().toLong()
                        )
                    }
                    BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME -> {
                        AlarmSyncBroadcastReceiver.scheduleAlarm(context, activeSession.sessionId, preferences.backgroundSyncDelay())
                    }
                    BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED     -> {
                        // nop
                    }
                }
            }
        }
    }
}
