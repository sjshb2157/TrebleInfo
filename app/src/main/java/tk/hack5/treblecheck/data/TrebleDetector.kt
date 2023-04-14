/*
 *     Treble Info
 *     Copyright (C) 2019-2023 Hackintosh Five
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

package tk.hack5.treblecheck.data

import android.util.Log
import androidx.annotation.VisibleForTesting
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import tk.hack5.treblecheck.Mock
import tk.hack5.treblecheck.compareTo
import tk.hack5.treblecheck.get
import tk.hack5.treblecheck.propertyGet
import java.io.File

data class TrebleResult(val legacy: Boolean, val lite: Boolean,
                        val vndkVersion: Int, val vndkSubVersion: Int)

object TrebleDetector {
    private val SELINUX_REGEX = Regex("""\Winit_(\d+)_(\d+)\W""")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var root: File? = null

    fun getVndkData(): TrebleResult? {
        Mock.data?.let { return it.treble.get() }

        val trebleEnabled = propertyGet("ro.treble.enabled")
        Log.v(tag, "trebleEnabled: $trebleEnabled")
        if (trebleEnabled != "true")
            return null

        val liteProp = propertyGet("ro.vndk.lite")
        Log.v(tag, "lite: $liteProp")
        if (liteProp == null)
            return null
        val lite = liteProp == "true"

        val (manifests, legacy) = locateManifestFiles()
        Log.v(tag, "manifests: ${manifests.joinToString { it.absolutePath }}, legacy: $legacy")

        val matrix = locateVendorCompatibilityMatrix()
        matrix?.let {
            Log.v(tag, "matrix: ${matrix.absolutePath}")
            parseMatrix(it)
        }?.let {
            if (it == 0 to 0) {
                return null // no treble support
            }
            Log.v(tag, "vendor matrix result: $it")
            return TrebleResult(legacy, lite, it.first, it.second)
        }

        parseSelinuxData()?.let {
            Log.v(tag, "selinux result: $it")
            return TrebleResult(legacy, lite, it.first, it.second)
        }

        manifests
            .asSequence()
            .map {
                parseManifest(it)
                    .also { res -> Log.v(tag, "manifest ${it.absolutePath}: $res") }
            }
            .filterNotNull()
            .firstOrNull()
            ?.let {
                return TrebleResult(legacy, lite, it.first, it.second)
            }

        propertyGet("ro.vndk.version")?.let {
            Log.v(tag, "vndk: $it")
            parseVersion(it)
        }?.let {
            Log.v(tag, "vndk result: $it")
            return TrebleResult(legacy, lite, it.first, it.second)
        }

        throw ParseException("No method to detect version")
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseMatrix(matrix: File) = parseXml(matrix) { xpp ->
        val versions = mutableListOf<String>()
        val versionBuilder = StringBuilder(2) // 2 is the normal size of the version number, 'xy'

        var inVndkTag = false
        var inVersionTag = false
        var event = xpp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (xpp.name == "vendor-ndk" || xpp.name == "vndk") {
                    inVndkTag = true
                } else if (inVndkTag && xpp.name == "version") {
                    inVersionTag = true
                }
            } else if (event == XmlPullParser.END_TAG) {
                if (inVersionTag) {
                    inVersionTag = false
                    versions += versionBuilder.toString()
                    versionBuilder.clear()
                } else if (inVndkTag) {
                    break
                }
            } else if (event == XmlPullParser.TEXT && inVersionTag) {
                // This is the version number
                versionBuilder.append(xpp.text.trim())
            }
            xpp.next()
            event = xpp.eventType
        }
        versions
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseManifest(manifest: File) = parseXml(manifest) { xpp ->
        val versions = mutableListOf<String>()
        val versionBuilder = StringBuilder(4) // 2 is the normal size of the version number, 'xy'

        var inVersionTag = false
        var event = xpp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && xpp.name == "sepolicy") {
                inVersionTag = true
            } else if (event == XmlPullParser.END_TAG && inVersionTag) {
                inVersionTag = false
                versions += versionBuilder.toString()
                versionBuilder.clear()
            } else if (event == XmlPullParser.TEXT && inVersionTag) {
                // This is the version number
                versionBuilder.append(xpp.text.trim())
            }
            xpp.next()
            event = xpp.eventType
        }
        versions
    }

    private fun parseXml(file: File, block: (XmlPullParser) -> List<String>): Pair<Int, Int>? {
        val versions = file.inputStream().use { inputStream ->
            inputStream.reader().use { reader ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val xpp = factory.newPullParser().apply {
                    setInput(reader)
                }

                block(xpp)
            }
        }

        return versions
            .mapNotNull { parseVersion(it) }
            .maxWithOrNull { left, right -> left.compareTo(right) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseVersion(string: String): Pair<Int, Int>? {
        val split = string.split('.').map(String::trim)
        if (split.isNotEmpty() && split.all { it == "0" }) {
            return 0 to 0 // we have a manifest but no treble
        }
        if (split.size != 1 && split.size != 2) {
            return null
        }
        if (split[0].any { it !in '0'..'9' } || split[0].isEmpty()) {
            // ASCII only by design
            return null
        }
        val first = split[0].toInt(10)
        if (split.size == 1 || split[1].any { it !in '0'..'9' } || split[1].isEmpty()) {
            // ASCII only by design
            return first to 0
        }
        val second = split[1].toInt(10)
        return first to second
    }

    /**
     * See https://cs.android.com/android/platform/superproject/+/master:system/libvintf/VintfObject.cpp;l=289;drc=0a1c02083dbd6e23f074069ebf45a87ec0757f30
     * 1. Get vendor manifest. Iff found, add vendor fragments.
     * 2. Add ODM manifest if available.
     * 3. Iff vendor manifest was found, add ODM fragments.
     * 4. Iff nothing found so far, use legacy manifest.
     * @return files to legacy
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun locateManifestFiles(): Pair<List<File>, Boolean> {
        val ret = mutableListOf<File>()
        val foundVendorManifest = locateVendorManifest(propertyGet("ro.boot.product.vendor.sku"))?.let {
            ret += it
            ret += locateVendorManifestFragments()
        } != null
        locateOdmManifest(propertyGet("ro.boot.product.hardware.sku"))?.let {
            ret += it
        }
        if (foundVendorManifest) {
            ret += locateOdmManifestFragments()
        }
        if (ret.isEmpty()) {
            locateLegacyManifest()?.let {
                return listOf(it) to true
            }
        }
        return ret to false
    }

    /**
     * See https://cs.android.com/android/platform/superproject/+/master:system/libvintf/VintfObject.cpp;l=343;drc=0a1c02083dbd6e23f074069ebf45a87ec0757f30
     */
    private fun locateVendorManifest(sku: String?): File? {
        if (!sku.isNullOrEmpty()) {
            File(root, "/vendor/etc/vintf/manifest_$sku.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        } else {
            File(root, "/vendor/etc/vintf/manifest.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        }
        return null
    }

    private fun locateVendorManifestFragments(): List<File> {
        val dir = File(root, "/vendor/etc/vintf/manifest")
        return (dir.listFiles() ?: return emptyList()).filter { it.canRead() }
    }

    /**
     * See https://cs.android.com/android/platform/superproject/+/master:system/libvintf/VintfObject.cpp;l=375;drc=0a1c02083dbd6e23f074069ebf45a87ec0757f30
     */
    private fun locateOdmManifest(sku: String?): File? {
        if (!sku.isNullOrEmpty()) {
            File(root, "/odm/etc/vintf/manifest_$sku.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        } else {
            File(root, "/odm/etc/vintf/manifest.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        }
        // legacy locations, treated as normal though
        if (!sku.isNullOrEmpty()) {
            File(root, "/odm/etc/manifest_$sku.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        } else {
            File(root, "/odm/etc/manifest.xml").let {
                if (it.exists()) {
                    return if (it.canRead()) {
                        it
                    } else {
                        Log.w(tag, "Cannot read ${it.path}")
                        null
                    }
                }
            }
        }
        return null
    }

    private fun locateOdmManifestFragments(): List<File> {
        val dir = File(root, "/odm/etc/vintf/manifest")
        return (dir.listFiles() ?: return emptyList()).toList()
    }

    private fun locateLegacyManifest(): File? {
        File(root, "/vendor/manifest.xml").let {
            if (it.exists() && it.canRead())
                return it
        }
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun locateVendorCompatibilityMatrix(): File? {
        File(root, "/vendor/etc/vintf/compatibility_matrix.xml").let {
            if (it.exists() && it.canRead()) {
                return it
            }
        }
        File(root, "/vendor/compatibility_matrix.xml").let {
            if (it.exists() && it.canRead()) {
                return it
            }
        }
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun parseSelinuxData(): Pair<Int, Int>? {
        // https://android.googlesource.com/platform/system/core/+/refs/tags/android-12.1.0_r2/init/selinux.cpp#281
        val sepolicyVersionFile = File(root, "/vendor/etc/selinux/plat_sepolicy_vers.txt")
        if (sepolicyVersionFile.exists()) {
            return parseVersion(sepolicyVersionFile.bufferedReader().readLine())
        }

        val files = File(root, "/vendor/etc/selinux/").listFiles { it -> it.canRead() && it.extension == "cil" }
        return files?.let { parseSelinuxData(it) }
    }

    private fun parseSelinuxData(files: Array<File>): Pair<Int, Int>? {
        var version = Pair(-1, -1)

        files.forEach { file ->
            file.bufferedReader().lineSequence().forEach { line ->
                SELINUX_REGEX.findAll(line).forEach { match ->
                    Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt()).let {
                        if (it > version) version = it
                    }
                }
            }
        }
        if (version <= Pair(0, 0)) return null
        return version
    }
}

private const val tag = "TrebleDetector"