package xyz.omnicron.apps.android.dot.api.models

import com.squareup.moshi.Json

class ManifestResponse(
    @Json(name = "Response") val data: Manifest
): ApiResponseBase()


class Manifest (
    @Json(name = "version") val version: String,
    @Json(name = "mobileWorldContentPaths") val contentPaths: Map<String, String>
)