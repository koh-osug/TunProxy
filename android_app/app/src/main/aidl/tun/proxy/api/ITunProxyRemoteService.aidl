package tun.proxy.api;

interface ITunProxyRemoteService {

    /**
     * Starts the service with a list of allowed applications.
     */
    void startAllowed(String ip, int port, in List allowedApps);

    /**
     * Starts the service with a list of denied applications.
     */
    void startDenied(String ip, int port, in List deniedApps);

    /**
     * Stops the service
     */
    void stop();

}