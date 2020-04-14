package xyz.omnicron.apps.android.dot

// Interface marker for Destiny related Exceptions
interface DestinyException

/**
 * This exception is thrown whenever there is an authentication issue with the Bungie/Destiny 2 API.
 */
class DestinyAuthException(val error: String): DestinyException, Throwable(error)

