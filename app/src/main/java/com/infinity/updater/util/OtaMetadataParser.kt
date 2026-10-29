/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.infinity.updater.util

import android.ota.nano.OtaPackageMetadata.OtaMetadata
import java.io.File
import java.io.IOException
import java.util.Properties
import java.util.zip.ZipFile

class OtaMetadataParser @Throws(IOException::class) constructor(file: File) {
    val sdkLevel: Int
    val securityPatchLevel: String
    val timestamp: Long
    val isABUpdate: Boolean

    init {
        var parsedSdkLevel = 35
        var parsedSecurityPatchLevel = ""
        var parsedTimestamp = System.currentTimeMillis() / 1000
        var parsedIsABUpdate = true

        ZipFile(file).use { zip ->
            val hasPayload = zip.getEntry("payload.bin") != null
            val pbEntry = zip.getEntry(METADATA_PROTO_NAME)
            if (pbEntry != null) {
                val metadata = zip.getInputStream(pbEntry).use { input ->
                    OtaMetadata.parseFrom(input.readBytes())
                }
                parsedIsABUpdate = hasPayload || when (metadata.type) {
                    OtaMetadata.AB -> true
                    OtaMetadata.BLOCK -> false
                    else -> true
                }
                val postcondition = metadata.postcondition
                parsedSdkLevel = postcondition.sdkLevel.toIntOrNull() ?: 35
                parsedSecurityPatchLevel = postcondition.securityPatchLevel
                parsedTimestamp = postcondition.timestamp
            } else {
                val txtEntry = zip.getEntry(METADATA_TEXT_NAME)
                if (txtEntry != null) {
                    val props = Properties()
                    zip.getInputStream(txtEntry).use { input ->
                        props.load(input)
                    }
                    parsedIsABUpdate = hasPayload || props.getProperty("ota-type", "AB") == "AB"
                    parsedSdkLevel = props.getProperty("post-sdk-level")?.toIntOrNull() ?: 35
                    parsedSecurityPatchLevel = props.getProperty("post-security-patch-level") ?: ""
                    parsedTimestamp = props.getProperty("post-timestamp")?.toLongOrNull()
                        ?: (System.currentTimeMillis() / 1000)
                } else if (hasPayload) {
                    parsedIsABUpdate = true
                } else {
                    throw IOException("No valid OTA metadata found in ${zip.name}")
                }
            }
        }

        sdkLevel = parsedSdkLevel
        securityPatchLevel = parsedSecurityPatchLevel
        timestamp = parsedTimestamp
        isABUpdate = parsedIsABUpdate
    }

    companion object {
        private const val METADATA_PROTO_NAME = "META-INF/com/android/metadata.pb"
        private const val METADATA_TEXT_NAME = "META-INF/com/android/metadata"
    }
}
