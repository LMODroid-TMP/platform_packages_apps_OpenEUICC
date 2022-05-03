/*
 * Copyright 2022 Peter Cai & Pierre-Hugues Husson
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.truphone.lpa

import java.lang.IllegalArgumentException
import java.lang.StringBuilder

data class LocalProfileInfo(
    val iccid: String,
    val state: State,
    val name: String,
    val nickName: String,
    val providerName: String,
    val isdpAID: String,
    val profileClass: Clazz
) {
    val iccidLittleEndian by lazy {
        iccidBigToLittle(iccid)
    }

    enum class State {
        Enabled,
        Disabled
    }

    enum class Clazz {
        Testing,
        Provisioning,
        Operational
    }

    companion object {
        fun stateFromString(state: String?): State =
            if (state == "0") {
                State.Disabled
            } else {
                State.Enabled
            }

        fun classFromString(clazz: String?): Clazz =
            when (clazz) {
                "0" -> Clazz.Testing
                "1" -> Clazz.Provisioning
                "2" -> Clazz.Operational
                else -> throw IllegalArgumentException("Unknown profile class $clazz")
            }

        private fun iccidBigToLittle(iccid: String): String {
            val builder = StringBuilder()
            for (i in 0 until iccid.length / 2) {
                builder.append(iccid[i * 2 + 1])
                if (iccid[i * 2] != 'F') builder.append(iccid[i * 2])
            }
            return builder.toString()
        }
    }
}