package converter;

public class ConvertUtils {

    public static void joinPair(StringBuilder result, String key, String value) {
        result.append("\"").append(key).append("\": ").append(value);
    }

    public static void warpWithCurlyBrace(StringBuilder result) {
        result.insert(0, "{").append("}");
    }

    public static void wrapWithAngleBrackets(StringBuilder result) {
        result.insert(0, "<").append(">");
    }

    public static void warpWithSuareBrace(StringBuilder result) {
        result.insert(0, "[").append("]");
    }

    public static String removeDoubleQuotesIfExist(String value) {
        return value.startsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    public static String removeSpaceAndLine(String value) {
        value = value.replaceAll("\\s", "").replaceAll("\r\n", "").replaceAll("\n", "");
        return value;
    }

    public static String removeSpaceBetweenAngleBrackets(String str) {
        return str.replaceAll(">\\s+<", "><");
    }

    public static String removeLine(String json) {
        return json.replaceAll("\r\n", "").replaceAll("\n", "");
    }
}
