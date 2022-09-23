package com.evgenii.jsevaluator;

import java.util.Map;

/* loaded from: classes6.dex */
public class JsFunctionCallFormatter {
    public static String paramToString(Object obj) {
        if (obj instanceof String) {
            return String.format("\"%s\"", ((String) obj).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"));
        }
        try {
            Double.parseDouble(obj.toString());
            return obj.toString();
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public static String toString(String str, Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : map.keySet()) {
            String str3 = map.get(str2);
            if (str3 == null) {
                str3 = "";
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(paramToString(str3));
        }
        return String.format("%s(%s)", str, sb);
    }
}
