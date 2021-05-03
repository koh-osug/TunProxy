package tun.proxy.api;

import tun.proxy.api.IStartStopCallback;

interface ITunProxyRemoteService {

    /**
     * Starts the service with a list of allowed applications.
     * @return <code>true</code> if the proxy was started.
     */
    void startAllowed(String ip, int port, in List allowedApps, in IStartStopCallback cb);

    /**
     * Starts the service with a list of denied applications.
     * @return <code>true</code> if the proxy was started.
     */
    void startDenied(String ip, int port, in List deniedApps, in IStartStopCallback cb);

    /**
     * Stops the service
     */
    void stop(in IStartStopCallback cb);

}