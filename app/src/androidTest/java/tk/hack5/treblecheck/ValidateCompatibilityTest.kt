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

package tk.hack5.treblecheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tk.hack5.treblecheck.data.TrebleDetector

@RunWith(Parameterized::class)
class ValidateCompatibilityTest(
    private val testName: String,
    private val vendorSku: String,
    private val hardwareSku: String,
    private val sepolicyVersion: Pair<Int, Int>,
    private val targetLevel: Int?,
    private val expected: Boolean?,
) {
    companion object {
        @Suppress("BooleanLiteralArgument")
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data() = listOf(
            arrayOf("vndk1a", "vendorSku", "hardwareSku", 30 to 0, 3, true),
            arrayOf("vndk3a", "vendorSku", "hardwareSku", 32 to 0, 3, true),
            arrayOf("vndk4a", "vendorSku", "hardwareSku", 30 to 0, 3, true),
            // No target-level, so the legacy matrix applies. vndk5a used to fail
            // it on missing required HALs; libvintf stopped checking those in
            // AOSP c2de8e5, so it now passes on the sepolicy version alone.
            arrayOf("vndk5a", "vendorSku", "hardwareSku", 27 to 0, null, true),
            arrayOf("vndk6a", "vendorSku", "hardwareSku", 27 to 0, null, true),
        )

        private val EXPECTED_LEVELS =
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 202404, 202504, 202604, 202704)

        private val LEVEL_ATTRIBUTE =
            Regex("""<compatibility-matrix[^>]*\blevel="([^"]+)""")
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun extractFiles(name: String) {
        println("Extracting $name $vendorSku $hardwareSku")

        extractFiles(name, allFiles(vendorSku, hardwareSku), temporaryFolder)
    }

    /** Every bundled matrix parses, declares the level we index it under, and survives libvintf. */
    @Test
    fun checkBundledMatrices() {
        extractFiles(testName)

        TrebleDetector.root = temporaryFolder.root
        val (matrices, maxLevel) = TrebleDetector.getFrameworkCompatibilityMatrices(sepolicyVersion)
        assertEquals(7, maxLevel)

        val levels = matrices.map { (level, matrix) ->
            val declared = LEVEL_ATTRIBUTE.find(matrix)?.groupValues?.get(1)
            assertEquals("matrix $level level attribute", if (level == 0) "legacy" else "$level", declared)
            val result = TrebleDetector.checkCompatibilityMatrix(matrix, vendorSku, hardwareSku)
            assertTrue("matrix $level was rejected by libvintf (result $result)", result >= 0)
            level
        }.toList()
        assertEquals(EXPECTED_LEVELS, levels)
    }

    /** The declared shipping FCM version selects exactly one matrix, per the AOSP match rules. */
    @Test
    fun checkValidateCompatibility() {
        extractFiles(testName)

        TrebleDetector.root = temporaryFolder.root
        assertEquals(expected, TrebleDetector.checkCompatibilityMatrix(targetLevel, sepolicyVersion))
    }
}
