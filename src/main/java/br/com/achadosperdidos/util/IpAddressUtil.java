package br.com.achadosperdidos.util;

public final class IpAddressUtil {

    private IpAddressUtil() {}

    public static String normalize(String ip) {
        if (ip == null || ip.isBlank()) return null;
        String value = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) return "127.0.0.1";
        return value;
    }

    /**
     * Remove a porta de um endereço no formato {@code IP:porta}.
     *
     * <p>O {@code CloudFront-Viewer-Address} chega como {@code 203.0.113.10:52432} (IPv4)
     * ou {@code 2001:db8::1:52432} (IPv6, sem colchetes). Como o IPv6 usa ':' como
     * separador, só cortamos quando o resultado continua sendo um endereço plausível —
     * caso contrário devolvemos o valor original (ex.: IPv6 puro, sem porta).</p>
     */
    public static String stripPort(String address) {
        if (address == null || address.isBlank()) return null;
        String value = address.trim();
        if (value.startsWith("[")) {
            int fecha = value.indexOf(']');
            return fecha > 1 ? value.substring(1, fecha) : value;
        }
        int ultimo = value.lastIndexOf(':');
        if (ultimo <= 0 || ultimo == value.length() - 1) {
            return value;
        }
        String porta = value.substring(ultimo + 1);
        if (porta.isEmpty() || porta.length() > 5 || !porta.chars().allMatch(Character::isDigit)) {
            return value;
        }
        String host = value.substring(0, ultimo);
        // IPv6 sem porta ("2001:db8::1") viraria "2001:db8::" — nesse caso não cortamos.
        if (host.endsWith(":")) {
            return value;
        }
        return host;
    }
}
