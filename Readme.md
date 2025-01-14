# Android Network Traffic Proxy Tool

This tool is a proxy forwarding tool that takes advantage of Android VPNService feature.
The communication from specified applications can be proxied to a SOCKS proxy.

Most of the C based code was copied from [NetGuard](https://github.com/M66B/NetGuard) and is the work is also released under the GPL 3.0.

## How to use

When you start the TunProxy application, the following screen will be launched.

![Tun Proxy](images/TunProxy.png)

* Proxy address (ipv4:port)
  * Specify the destination proxy server in the format **IPv4 address:port number**.
    The IP address must be described in IPv4 format.

* [Start] button
  * Start the VPN service.
* [Stop] button
  * Stop the VPN service.

## Menu

Application settings can be made from the menu icon (![Menu](images/Menu.png)) at the top of the screen.

### Settings

Configure VPN service settings.

![Menu Settings](images/Menu-Settings.png) ⇒ ![Menu Settings](images/Menu-Settings-app.png)

There are two modes, Disallowed Application and Allowed Application, but you can not specify them at the same time.
Because of this you will have to choose whether you want to run in either mode.
The default is **Disallowed Application** selected.

* Disallowed Application
  * Select the application you want to exclude from VPN service.
    The selected application will no longer go through VPN service and behave the same as if you do not use VPN.

* Allowed Application
  * Select the application for which you want to perform VPN service.
    The selected application will now go through VPN service.
    Applications that are not selected behave the same as when not using VPN.
    In addition, if none of them are selected, communication of all applications will go through VPN.

* Clear all selection
  * Clear all selections of Allowed / Disallowed application list.

### Settings Search

![Menu Settings](images/Menu-Settings-Search.png) / ![Menu Settings](images/Menu-Settings-SortBy.png)

You can narrow down the applications from the search icon.(![Menu](images/Search.png))
Only applications that contain the keyword specified in the application name or package name will be displayed.

The application list can be sorted from the menu icon  (![Menu](images/Menu.png)) at the top of the screen.

* order by asc
  * Sorting in ascending order

* order by desc
  * Sorting in descending order

* filter by app name
  * Search for the application name that contains the keyword you specified.

* filter by package name
  * Search for the package name that contains the keyword you specified.

* sort by app name
  * Sort application list by application name

* sort by package name
  * Sort application list by package name

### MITM (TLS Decryption)

TunProxy acts like a transparent proxy.
To perform TLS decryption, configure the IP and port of a TLS decryptable SOCKS proxy in TunProxy.
SOCKS offer a wider support for protocol and it not limited to HTTPs.

The following are local proxy tools that can decrypt TLS.

If the proxy you want to use is only supporting HTTP and not SOCKS, you need a SOCKS to HTTP proxy.
This [StackExchange question](https://superuser.com/questions/443160/is-there-a-socks-proxy-server-program-that-supports-a-http-parent-proxy) lists some possible solutions.

Some HTTP based proxies are:

* [mitmproxy](https://mitmproxy.org)
* [Burp suite](https://portswigger.net/burp)
* [Fiddler](https://www.telerik.com/fiddler)
* [ZAP Proxy](https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project)


Some SOCKS based proxies are:

* [Netty in the Middle](https://github.com/chhsiao90/nitmproxy) __NOTE:__ This is a library only and must be included in a developed app to log the data traffic.

To decrypt TLS, install the local proxy tool CA certificate in the Android device user certificate.
However, in Android 7.0 and later, the application no longer trusts user certificates by default.

* https://android-developers.googleblog.com/2016/07/changes-to-trusted-certificate.html

Please refer to the following web site as a solution

* Android 7 Nougat and certificate authorities
  * https://blog.jeroenhd.nl/article/android-7-nougat-and-certificate-authorities
* An alternative is using [apktool](https://ibotpeaches.github.io/Apktool/), including a network security configuration and repackage and sign the application.

### About

Display application version

## Operating environment

* Android 5.0 (API Level 21) or later

### Build

~~~shell
./gradlew build
 ~~~~

The used icon was purchased from iconfinder.
