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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.*
import com.mikepenz.aboutlibraries.util.withContext
import tk.hack5.treblecheck.*
import tk.hack5.treblecheck.R
import tk.hack5.treblecheck.ui.listVerticalPadding
import tk.hack5.treblecheck.ui.pageHorizontalPadding

@Composable
fun Licenses(
    innerPadding: PaddingValues,
    scrollConnection: NestedScrollConnection,
) {
    val libraries = remember { mutableStateOf<Libs?>(null) }

    val context = LocalContext.current
    LaunchedEffect(libraries) {
        libraries.value = Libs.Builder().withContext(context).build()
    }

    val licenses = setOf(
        License(
            SpdxLicense.GPL_3_0_or_later.fullName,
            SpdxLicense.GPL_3_0_or_later.getUrl(),
            null,
            SpdxLicense.GPL_3_0_or_later.id,
            context.resources.openRawResource(R.raw.license).bufferedReader().readText(),
            SpdxLicense.GPL_3_0_or_later.id + "-TrebleInfo"
        )
    )
    val thisLibrary = Library(
        "tk.hack5:treblecheck",
        BuildConfig.VERSION_NAME,
        stringResource(R.string.title),
        stringResource(R.string.this_app),
        "https://hack5.dev/about/projects/TrebleInfo",
        listOf(Developer("hackintosh5", "https://hack5.dev/about")),
        null,
        Scm(
            "https://hack5.dev/about/projects/TrebleInfo",
            "scm:git:ssh://git@gitlab.com/TrebleInfo/TrebleInfo.git",
            "scm:git:https://gitlab.com/TrebleInfo/TrebleInfo.git"
        ),
        licenses,
        setOf(),
        null
    )

    val newLibraries = libraries.value?.let { Libs(listOf(thisLibrary) + it.libraries, licenses + it.licenses) }

    newLibraries?.let { Libraries(innerPadding, scrollConnection, it) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Libraries(innerPadding: PaddingValues, scrollConnection: NestedScrollConnection, libraries: Libs) {
    var openLicenseIndex by rememberSaveable { mutableStateOf<String?>(null) }
    val openLicense = openLicenseIndex?.let { libraries.licenses.first { license -> license.hash == it } }

    openLicense?.let {
        AlertDialog(
            onDismissRequest = { openLicenseIndex = null },
            confirmButton = {
                TextButton(onClick = { openLicenseIndex = null }) {
                    Text(stringResource(R.string.close_dialog))
                }
            },
            title = {
                Text(it.name)
            },
            text = {
                it.licenseContent?.let { content ->
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(content)
                    }
                }
            }
        )
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection)
            .consumeWindowInsets(innerPadding),
        contentPadding = innerPadding
    ) {
        items(libraries.libraries) { library ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = listVerticalPadding, horizontal = pageHorizontalPadding)
                    .safeDrawingPadding()
            ) {
                Row {
                    Text(
                        library.name,
                        Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                    library.artifactVersion?.let {
                        Text(
                            it,
                            Modifier
                                .padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                (library.organization?.name ?: library.developers.firstOrNull()?.name)?.let {
                    Text(
                        it
                    )
                }
                FlowRow {
                    library.licenses.forEach {
                        OutlinedButton(
                            onClick = { openLicenseIndex = it.hash }
                        ) {
                            Text(it.name)
                        }
                    }
                }
            }
        }
    }
}