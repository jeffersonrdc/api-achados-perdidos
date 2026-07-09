package br.com.achadosperdidos.util;

public final class IpAddressUtil {

    private IpAddressUtil() {}

    public static String normalize(String ip) {
        if (ip == null || ip.isBlank()) return null;
        String value = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) return "127.0.0.1";
        return value;
    }
}
