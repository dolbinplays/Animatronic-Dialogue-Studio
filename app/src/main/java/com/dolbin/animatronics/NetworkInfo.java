package com.dolbin.animatronics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import java.net.*;
import java.util.*;

/** Local-network diagnostics used by the Director and Performer.
 * Does not require cellular service or an Internet connection. */
public final class NetworkInfo {
    private NetworkInfo() {}

    public static List<String> ipv4Addresses() {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress a = addresses.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        found.add(a.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(found);
    }

    /** The Wi-Fi network gateway, NOT necessarily the Director if using a router. */
    public static String wifiGateway(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return null;
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                if (nc == null || !nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue;
                LinkProperties lp = cm.getLinkProperties(n);
                if (lp == null) continue;
                for (RouteInfo route : lp.getRoutes()) {
                    InetAddress gateway = route.getGateway();
                    if (route.isDefaultRoute() && gateway instanceof Inet4Address) {
                        return gateway.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static List<InetAddress> broadcastAddresses() {
        LinkedHashSet<InetAddress> list = new LinkedHashSet<>();
        try {
            list.add(InetAddress.getByName("255.255.255.255"));
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    if (addr.getBroadcast() instanceof Inet4Address) list.add(addr.getBroadcast());
                }
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(list);
    }
}
