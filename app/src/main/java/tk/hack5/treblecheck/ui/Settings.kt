/*
 *     Treble Info
 *     Copyright (C) 2023 Hackintosh Five
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: GPL-3.0-or-later

package tk.hack5.treblecheck.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import tk.hack5.treblecheck.R

enum class ThemeMode(@param:StringRes val label: Int) {
    System(R.string.theme_mode_system),
    Light(R.string.theme_mode_light),
    Dark(R.string.theme_mode_dark),
}

val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Passing null gives an in-memory instance, for previews and mocks.
 */
@Stable
class Settings(private val prefs: SharedPreferences?) {
    private var themeModeState by mutableStateOf(
        prefs?.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.System
    )

    private var dynamicColorState by mutableStateOf(
        prefs?.getBoolean(KEY_DYNAMIC_COLOUR, true) ?: true
    )

    private var pureBlackState by mutableStateOf(
        prefs?.getBoolean(KEY_PURE_BLACK, false) ?: false
    )

    var themeMode: ThemeMode
        get() = themeModeState
        set(value) {
            themeModeState = value
            prefs?.edit { putString(KEY_THEME_MODE, value.name) }
        }

    var dynamicColour: Boolean
        get() = dynamicColorAvailable && dynamicColorState
        set(value) {
            dynamicColorState = value
            prefs?.edit { putBoolean(KEY_DYNAMIC_COLOUR, value) }
        }

    var pureBlack: Boolean
        get() = pureBlackState
        set(value) {
            pureBlackState = value
            prefs?.edit { putBoolean(KEY_PURE_BLACK, value) }
        }

    companion object {
        private const val FILE = "settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOUR = "dynamic_colour"
        private const val KEY_PURE_BLACK = "pure_black"

        fun from(context: Context) =
            Settings(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }
}
