/*
 *     TunProxy is a proxy forwarding tool using Android's VPNService.
 *     Copyright (C) 2021 raise.isayan@gmail.com / Karsten Ohme
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tun.proxy.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.text.TextUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DnsService {

    private ConnectivityManager cm;

    @Inject
    public DnsService(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private static native String jni_getprop(String name);

    public List<String> getDefaultDNS() {
        String dns1 = null;
        String dns2 = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            Network an = cm.getActiveNetwork();
            if (an != null) {
                LinkProperties lp = cm.getLinkProperties(an);
                if (lp != null) {
                    List<InetAddress> dns = lp.getDnsServers();
                    if (dns != null) {
                        if (dns.size() > 0)
                            dns1 = dns.get(0).getHostAddress();
                        if (dns.size() > 1)
                            dns2 = dns.get(1).getHostAddress();
                    }
                }
            }
        } else {
            dns1 = jni_getprop("net.dns1");
            dns2 = jni_getprop("net.dns2");
        }

        List<String> listDns = new ArrayList<>();
        listDns.add(TextUtils.isEmpty(dns1) ? "8.8.8.8" : dns1);
        listDns.add(TextUtils.isEmpty(dns2) ? "8.8.4.4" : dns2);
        return listDns;
    }
}
