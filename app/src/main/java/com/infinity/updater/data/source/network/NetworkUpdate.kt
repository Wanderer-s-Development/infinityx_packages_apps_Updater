/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.infinity.updater.data.source.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import com.infinity.updater.data.Update
import com.infinity.updater.misc.Constants

// Commented out fields below are available in production, but not needed in runtime.
// If you wish to uncomment any of them, please update the README.md to indicate that.

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkUpdate(
    // @SerialName("date") val date: String,
    @SerialName("datetime") val datetime: Long,
    @SerialName("files") val files: List<NetworkUpdateFile>,
    @SerialName("type") val type: String,
    @SerialName("version") val version: String,
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkUpdateFile(
    // @SerialName("date") val date: String? = null,
    // @SerialName("datetime") val datetime: Long? = null,
    @SerialName("filename") val filename: String,
    // @SerialName("filepath") val filepath: String,
    @SerialName("os_patch_level") val osPatchLevel: String? = null,
    @SerialName("os_sdk_level") val osSdkLevel: Int? = null,
    @SerialName("ota_property_files") val otaPropertyFiles: String? = null,
    // @SerialName("sha1") val sha1: String,
    @SerialName("sha256") val sha256: String,
    @SerialName("size") val size: Long,
    // @SerialName("type") val type: String? = null,
    @SerialName("url") val url: String,
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class InfinityResponse(
    @SerialName("response") val response: List<InfinityUpdateItem> = emptyList(),
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class InfinityUpdateItem(
    @SerialName("datetime") val datetime: Long = 0L,
    @SerialName("filename") val filename: String = "",
    @SerialName("download") val download: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("md5") val md5: String? = null,
    @SerialName("sha256") val sha256: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("size") val size: Long = 0L,
    @SerialName("version") val version: String = "",
    @SerialName("romtype") val romtype: String? = null,
    @SerialName("os_sdk_level") val osSdkLevel: Int? = null,
)

fun InfinityResponse.toNetworkUpdates(defaultType: String): List<NetworkUpdate> = response.map { item ->
    val time = if (item.timestamp > 0) item.timestamp else item.datetime
    val fileUrl = item.download.ifEmpty { item.url }
    val hash = item.sha256 ?: item.id ?: item.md5 ?: item.filename
    NetworkUpdate(
        datetime = time,
        files = listOf(
            NetworkUpdateFile(
                filename = item.filename,
                sha256 = hash,
                size = item.size,
                url = fileUrl,
                osSdkLevel = item.osSdkLevel ?: DeviceInfoUtils.sdkLevel,
            )
        ),
        type = item.romtype?.uppercase() ?: defaultType,
        version = item.version,
    )
}

private data class PackageFileRange(
    val offset: Long,
    val size: Long,
)

private fun String.parsePackageFileRanges() =
    split(",").associate { token ->
        val parts = token.trim().split(":")
        parts[0].trim() to PackageFileRange(
            offset = parts[1].trim().toLong(),
            size = parts[2].trim().toLong(),
        )
    }

fun NetworkUpdate.toUpdate(): Update {
    val file = files[0]
    val packageFileRanges = file.otaPropertyFiles?.parsePackageFileRanges().orEmpty()
    val payloadMetadataRange = packageFileRanges[Constants.AB_PAYLOAD_METADATA_PATH]
    val payloadRange = packageFileRanges[Constants.AB_PAYLOAD_BIN_PATH]
    val payloadPropertiesRange = packageFileRanges[Constants.AB_PAYLOAD_PROPERTIES_PATH]

    return Update(
        downloadId = file.sha256,
        name = file.filename,
        timestamp = datetime,
        type = type,
        fileSize = file.size,
        downloadUrl = file.url,
        version = version,
        osPatchLevel = file.osPatchLevel,
        osSdkLevel = file.osSdkLevel ?: 0,
        payloadMetadataOffset = payloadMetadataRange?.offset,
        payloadMetadataSize = payloadMetadataRange?.size,
        payloadOffset = payloadRange?.offset,
        payloadSize = payloadRange?.size,
        payloadPropertiesOffset = payloadPropertiesRange?.offset,
        payloadPropertiesSize = payloadPropertiesRange?.size,
        isAvailableOnline = true,
    )
}
