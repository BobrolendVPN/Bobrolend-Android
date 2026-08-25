package org.bobrolend.vpn.manager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;

public class ConfigParser {

    public static String convertToXrayConfig(String link) throws Exception {
        String cleanLink = link.split("#")[0];
        String raw = cleanLink.replace("vless://", "");

        // limit=2 — на случай если сам UUID/пароль содержит "@" (крайне маловероятно, но дёшево подстраховаться)
        String[] parts = raw.split("@", 2);
        if (parts.length != 2) {
            throw new Exception("Некорректная vless-ссылка: не найден разделитель '@'");
        }
        String uuid = parts[0];

        String[] hostAndParams = parts[1].split("\\?", 2);
        String hostPort = hostAndParams[0];

        // lastIndexOf(':') вместо split(":") — чтобы корректно работать с IPv6-адресами вида [::1]:443,
        // у которых внутри самого адреса уже есть несколько ":"
        int portSeparator = hostPort.lastIndexOf(':');
        if (portSeparator == -1) {
            throw new Exception("Некорректная vless-ссылка: не найден порт");
        }

        String address = hostPort.substring(0, portSeparator);
        if (address.startsWith("[") && address.endsWith("]")) {
            address = address.substring(1, address.length() - 1); // [::1] -> ::1
        }

        int port;
        try {
            port = Integer.parseInt(hostPort.substring(portSeparator + 1));
        }
        catch (NumberFormatException e) {
            throw new Exception("Некорректная vless-ссылка: порт не является числом");
        }

        // Defaults
        String network = "tcp";
        String security = "none";

        String path = "/";
        String host = "";

        // TLS
        String sni = "";

        // Reality
        String fp = "chrome";
        String pbk = "";
        String sid = "";
        String flow = "";
        String spx = "/";

        if (hostAndParams.length > 1) {
            String params = hostAndParams[1];

            for (String param : params.split("&")) {
                // limit=2 — чтобы значения, содержащие "=", не резались на лишние куски
                String[] kv = param.split("=", 2);
                if (kv.length != 2) continue;

                String key = kv[0];
                String value = URLDecoder.decode(kv[1], "UTF-8");

                switch (key) {
                    case "type":
                        network = value;
                        break;

                    case "security":
                        security = value;
                        break;

                    case "path":
                        path = value;
                        break;

                    case "host":
                        host = value;
                        break;

                    case "sni":
                        sni = value;
                        break;

                    case "fp":
                        fp = value;
                        break;

                    case "pbk":
                        pbk = value;
                        break;

                    case "sid":
                        sid = value;
                        break;

                    case "flow":
                        flow = value;
                        break;

                    case "spx":
                        spx = value;
                        break;
                }
            }
        }

        // Validation
        if ("reality".equals(security)) {
            if (pbk.isEmpty() || sid.isEmpty() || sni.isEmpty()) {
                throw new Exception("Invalid Reality config: missing pbk/sid/sni");
            }
        }

        // --- Сборка JSON через org.json ---
        // Раньше JSON собирался конкатенацией строк вручную, без экранирования.
        // Если host/sni/uuid и т.п. содержали символы " или \, итоговый JSON
        // ломался. JSONObject делает экранирование всех значений сам.

        JSONObject config = new JSONObject();

        JSONObject inbound = new JSONObject();
        inbound.put("port", 10808);
        inbound.put("protocol", "socks");

        JSONObject inboundSettings = new JSONObject();
        inboundSettings.put("auth", "noauth");
        inboundSettings.put("udp", true);
        inbound.put("settings", inboundSettings);

        config.put("inbounds", new JSONArray().put(inbound));

        JSONObject outbound = new JSONObject();
        outbound.put("protocol", "vless");

        JSONObject user = new JSONObject();
        user.put("id", uuid);
        user.put("encryption", "none");
        if (!flow.isEmpty()) {
            user.put("flow", flow);
        }

        JSONObject vnext = new JSONObject();
        vnext.put("address", address);
        vnext.put("port", port);
        vnext.put("users", new JSONArray().put(user));

        JSONObject outboundSettings = new JSONObject();
        outboundSettings.put("vnext", new JSONArray().put(vnext));
        outbound.put("settings", outboundSettings);

        JSONObject streamSettings = new JSONObject();
        streamSettings.put("network", network);
        streamSettings.put("security", security);

        // TLS
        if ("tls".equals(security)) {
            JSONObject tlsSettings = new JSONObject();
            tlsSettings.put("serverName", host);
            streamSettings.put("tlsSettings", tlsSettings);
        }

        // Reality
        if ("reality".equals(security)) {
            JSONObject realitySettings = new JSONObject();
            realitySettings.put("serverName", sni);
            realitySettings.put("publicKey", pbk);
            realitySettings.put("shortId", sid);
            realitySettings.put("fingerprint", fp);
            realitySettings.put("spiderX", spx);
            streamSettings.put("realitySettings", realitySettings);
        }

        // WS
        if ("ws".equals(network)) {
            JSONObject wsSettings = new JSONObject();
            wsSettings.put("path", path);

            JSONObject headers = new JSONObject();
            headers.put("Host", host);
            wsSettings.put("headers", headers);

            streamSettings.put("wsSettings", wsSettings);
        }

        outbound.put("streamSettings", streamSettings);
        config.put("outbounds", new JSONArray().put(outbound));

        return config.toString(2);
    }
}

//package org.bobrolend.vpn.manager;
//
//public class ConfigParser {
//    public static String convertToXrayConfig(String link) throws Exception {
//        String cleanLink = link.split("#")[0];
//        String raw = cleanLink.replace("vless://", "");
//
//        String[] parts = raw.split("@");
//        String uuid = parts[0];
//
//        String[] hostAndParams = parts[1].split("\\?");
//        String hostPort = hostAndParams[0];
//
//        String[] hp = hostPort.split(":");
//        String address = hp[0];
//        int port = Integer.parseInt(hp[1]);
//
//        // Defaults
//        String network = "tcp";
//        String security = "none";
//
//        String path = "/";
//        String host = "";
//
//        // TLS
//        String sni = "";
//
//        // Reality
//        String fp = "chrome";
//        String pbk = "";
//        String sid = "";
//        String flow = "";
//        String spx = "/";
//
//        if (hostAndParams.length > 1) {
//            String params = hostAndParams[1];
//
//            for (String param : params.split("&")) {
//                String[] kv = param.split("=");
//                if (kv.length != 2) continue;
//
//                String key = kv[0];
//                String value = java.net.URLDecoder.decode(kv[1], "UTF-8");
//
//                switch (key) {
//                    // Default
//                    case "type":
//                        network = value;
//                        break;
//
//                    case "security":
//                        security = value;
//                        break;
//
//                    case "path":
//                        path = value;
//                        break;
//
//                    case "host":
//                        host = value;
//                        break;
//
//                    // TLS / Reality SNI
//                    case "sni":
//                        sni = value;
//                        break;
//
//                    // Reality params
//                    case "fp":
//                        fp = value;
//                        break;
//
//                    case "pbk":
//                        pbk = value;
//                        break;
//
//                    case "sid":
//                        sid = value;
//                        break;
//
//                    case "flow":
//                        flow = value;
//                        break;
//
//                    case "spx":
//                        spx = value;
//                        break;
//                }
//            }
//        }
//
//        // Validation
//
//        if ("reality".equals(security)) {
//            if (pbk.isEmpty() || sid.isEmpty() || sni.isEmpty()) {
//                throw new Exception("Invalid Reality config: missing pbk/sid/sni");
//            }
//        }
//
//        // JSON Build
//
//        StringBuilder json = new StringBuilder();
//
//        json.append("{\n")
//            .append("  \"inbounds\": [{")
//            .append("\"port\":10808,")
//            .append("\"protocol\":\"socks\",")
//            .append("\"settings\":{\"auth\":\"noauth\",\"udp\":true}")
//            .append("}],\n")
//
//            .append("  \"outbounds\": [{\n")
//            .append("    \"protocol\":\"vless\",\n")
//
//            .append("    \"settings\": {\n")
//            .append("      \"vnext\": [{\n")
//            .append("        \"address\": \"").append(address).append("\",\n")
//            .append("        \"port\": ").append(port).append(",\n")
//            .append("        \"users\": [{\n")
//            .append("          \"id\": \"").append(uuid).append("\",\n")
//            .append("          \"encryption\": \"none\"");
//
//        if (!flow.isEmpty()) {
//            json.append(",\n          \"flow\": \"").append(flow).append("\"");
//        }
//
//        json.append("\n        }]\n")
//            .append("      }]\n")
//            .append("    },\n")
//
//            .append("    \"streamSettings\": {\n")
//            .append("      \"network\": \"").append(network).append("\",\n")
//            .append("      \"security\": \"").append(security).append("\"");
//
//        // TLS
//        if ("tls".equals(security)) {
//            json.append(",\n      \"tlsSettings\": {\n")
//                .append("        \"serverName\": \"").append(host).append("\"\n")
//                .append("      }");
//        }
//
//        // Reality
//        if ("reality".equals(security)) {
//            json.append(",\n      \"realitySettings\": {\n")
//                .append("        \"serverName\": \"").append(sni).append("\",\n")
//                .append("        \"publicKey\": \"").append(pbk).append("\",\n")
//                .append("        \"shortId\": \"").append(sid).append("\",\n")
//                .append("        \"fingerprint\": \"").append(fp).append("\",\n")
//                .append("        \"spiderX\": \"").append(spx).append("\"\n")
//                .append("      }");
//        }
//
//        // WS
//        if ("ws".equals(network)) {
//            json.append(",\n      \"wsSettings\": {\n")
//                .append("        \"path\": \"").append(path).append("\",\n")
//                .append("        \"headers\": {\n")
//                .append("          \"Host\": \"").append(host).append("\"\n")
//                .append("        }\n")
//                .append("      }");
//        }
//
//        json.append("\n    }\n")
//            .append("  }]\n")
//            .append("}");
//
//        return json.toString();
//    }
//}
