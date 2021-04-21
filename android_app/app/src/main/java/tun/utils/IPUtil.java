package tun.utils;

public class IPUtil {

    private IPUtil() {
    }

    public static boolean isValidIPv4Address(String address) {
        if (address.isEmpty()) {
            return false;
        }
        String parts[] = address.split(":");
        int port = 0;
        if (parts.length > 1) {
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }
            if (!(0 < port && port < 65536)) {
                return false;
            }
        }
        String[] ipParts = parts[0].split("\\.");
        if (ipParts.length != 4) {
            return false;
        } else {
            for (int i = 0; i < ipParts.length; i++) {
                int ipPart = -1;
                try {
                    ipPart = Integer.parseInt(ipParts[i]);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (!(0 <= ipPart && ipPart <= 255)) {
                    return false;
                }
            }
        }
        return true;
    }

}
