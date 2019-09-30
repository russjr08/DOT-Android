package xyz.omnicron.apps.android.dot.api.models

import com.squareup.moshi.Json

open class ApiResponseBase {

    @Json(name = "ErrorStatus")
    lateinit var status: String

    @Json(name = "Message")
    lateinit var message: String

    @Json(name = "ErrorCode")
    var errorCode: Int = 0


}