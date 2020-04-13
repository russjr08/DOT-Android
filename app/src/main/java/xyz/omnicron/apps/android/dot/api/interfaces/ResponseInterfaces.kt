package xyz.omnicron.apps.android.dot.api.interfaces

import retrofit2.Call
import retrofit2.Response

/**
 * Used for wrapping callbacks from auto-wrapped Retrofit network/API calls
 */
interface IResponseReceiver<T> {

    /**
     * A callback for receiving a copy of a response from the
     * [xyz.omnicron.apps.android.dot.api.Destiny] API.
     *
     * @param response The response from the Bungie API.
     * @param request The original request sent to the Bungie API.
     */
    fun onNetworkTaskFinished(response: Response<T>, request: Call<T>)

    /**
     * A callback for being notified of a failure to access the
     * [xyz.omnicron.apps.android.dot.api.Destiny] API.
     *
     * @param request The original request sent to the Bungie API.
     */
    fun onNetworkFailure(request: Call<T>, error: Throwable)

}

/**
 * Used for wrapping callbacks via requests that don't have auto-wrapped Retrofit network/API calls
 */
interface IApiResponseCallback<T> {
    /**
     * This function is called when a request completes successfully from the [xyz.omnicron.apps.android.dot.api.Destiny] API.
     *
     * @param data The serialized data expected from the Bungie API.
     */
    fun onRequestSuccess(data: T)

    /**
     * This function is called when a request from the [xyz.omnicron.apps.android.dot.api.Destiny] API fails for some reason
     *
     * @param error The error returned from the request to the Bungie API.
     */
    fun onRequestFailed(error: Throwable)
}