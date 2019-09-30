package xyz.omnicron.apps.android.dot.api.interfaces

import retrofit2.Call
import retrofit2.Response

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