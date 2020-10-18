package xyz.omnicron.apps.android.dot.api.models

import org.joda.time.LocalDateTime

data class BungieAuthenticationData(var accessToken: String, var refreshToken: String,
                                    var accessExpiresAt: LocalDateTime,
                                    var refreshExpiresAt: LocalDateTime) {
}