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

package tk.hack5.treblecheck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tk.hack5.treblecheck.R
import tk.hack5.treblecheck.horizontal
import tk.hack5.treblecheck.ui.Settings
import tk.hack5.treblecheck.ui.ThemeMode
import tk.hack5.treblecheck.ui.dynamicColorAvailable
import tk.hack5.treblecheck.ui.pageHorizontalPadding

@Composable
fun Contribute(
    innerPadding: PaddingValues,
    scrollConnection: NestedScrollConnection,
    settings: Settings,
    darkTheme: Boolean,
    reportBug: () -> Unit,
    contributeCode: () -> Unit,
    projectPage: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .nestedScroll(scrollConnection)
            .padding(innerPadding.horizontal())
            .consumeWindowInsets(innerPadding)
    ) {
        Spacer(Modifier.height(innerPadding.calculateTopPadding()))

        SectionHeader(stringResource(R.string.section_appearance))

        Column(Modifier.padding(horizontal = pageHorizontalPadding, vertical = 8.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { settings.themeMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) {
                        Text(stringResource(mode.label), maxLines = 1)
                    }
                }
            }
        }

        if (dynamicColorAvailable) {
            SwitchRow(
                title = stringResource(R.string.dynamic_colour),
                summary = stringResource(R.string.dynamic_colour_summary),
                checked = settings.dynamicColour,
                onCheckedChange = { settings.dynamicColour = it },
            )
        }

        SwitchRow(
            title = stringResource(R.string.pure_black),
            summary = stringResource(R.string.pure_black_summary),
            checked = settings.pureBlack,
            onCheckedChange = { settings.pureBlack = it },
            enabled = darkTheme,
        )

        HorizontalDivider(Modifier.padding(horizontal = pageHorizontalPadding, vertical = 8.dp))

        SectionHeader(stringResource(R.string.section_contribute))

        LinkRow(stringResource(R.string.report_a_bug), reportBug)
        LinkRow(stringResource(R.string.contribute_code), contributeCode)
        LinkRow(stringResource(R.string.project_page), projectPage)

        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = pageHorizontalPadding, end = pageHorizontalPadding, top = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    // The whole row toggles; the switch is the affordance, not the only target.
    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = pageHorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = pageHorizontalPadding, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
            )
            Icon(
                painterResource(R.drawable.open_in_new),
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
