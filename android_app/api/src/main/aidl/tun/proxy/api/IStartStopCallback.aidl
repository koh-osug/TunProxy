package tun.proxy.api;

/**
* Callback triggered when starting or stopping the service.
*/
interface IStartStopCallback {

    /**
     * Callback in case of success.
     */
    oneway void onSuccess();

   /**
     * Callback in case the VPN permission was denied.
     */
    oneway void onPermissionDenied();

   /**
     * Callback in case of an error.
     */
    oneway void onError(String errorMsg);

}
