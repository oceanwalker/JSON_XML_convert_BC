package converter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NestJson {
    static class JsonElement {
        String content;
        int endIndex;
    }

    public static Element getElement(String json) {
        json = json.replaceAll("\r\n", "\n");
        json = ConvertUtils.removeSpaceAndLine(json);
        Element root = null;
        final Map<String, String> elementMap = getJsonElementMap(json);
        if (elementMap.size() > 1) {
            root = new Element("root");
        }
        return jsonToIntermediate(json, root);
    }

    public static Element jsonToIntermediate(String json, Element father) {
        List<Element> children = new ArrayList<>();
        Map<String, String> elementMap = getJsonElementMap(json);
        for (Map.Entry<String, String> entry : elementMap.entrySet()) {
            String elementName = entry.getKey();
            if (isIllegalElementName(elementName)) {
                continue;
            }
            if (elementName.startsWith("#") || elementName.startsWith("@")) {
                elementName = elementName.substring(1);
            }
            Element element = new Element(elementName);
            String content = entry.getValue();
            if (isArray(content)) {
                if (isBlankWrapWithBrace(content)) {
                    element.setValue("");
                } else {
                    buildJsonArray(content, element);
                }
            } else {
                if (isASingleValue(content)) {
                    element.setValue(content);
                } else if (isAnObjectWithAttributes(elementName, content)) {
                    buildJsonWithAttrToElement(element, content);
                } else {
                    jsonToIntermediate(content, element);
                }
            }
            children.add(element);
        }
        if (null != father) {
            father.setChildren(children);
        } else {
            father = children.get(0);
        }
        return father;
    }

    private static void buildJsonArray(String content, Element father) {
        content = content.substring(1, content.length() - 1);
        List<Element> array = new ArrayList<>();
        for (String elementContent : getArrayElementMap(content)) {
            Element element = new Element("element");
            if (isBlankWrapWithBrace(elementContent) || "\"\"".equals(elementContent) || "".equals(elementContent)) {
                element.setValue("");
            } else if (isArray(elementContent)) {
                buildJsonArray(elementContent, element);
            } else if (isJson(elementContent)) {
                element = jsonToIntermediate("\"element\":" + elementContent, null);
                element.setName("");
            } else if (!"null".equals(elementContent)) {
                element.setValue(elementContent);
            }
            array.add(element);
        }
        father.setArray(array);
    }

    private static void buildJsonWithAttrToElement(Element element, String jsonWithAttr) {
        String value = "";
        Map<String, String> attrMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : getJsonElementMap(jsonWithAttr).entrySet()) {
            String pairKey = entry.getKey();
            String pairValue = entry.getValue();
            if (("#" + element.getName()).equals(pairKey)) {
                value = pairValue;
            } else {
                if (pairKey.startsWith("@")) {
                    pairKey = pairKey.substring(1);
                }
                if (pairValue.equals("null") || isBlankWrapWithBrace(pairValue)) {
                    pairValue = "\"\"";
                }
                attrMap.put(pairKey, pairValue);
            }
        }
        final boolean valueHasChild = isJson(value);
        final boolean valueHasArray = isArray(value);
        if (value.length() > 0 && !valueHasChild) {
            element.setValue(value);
        }
        if (attrMap.size() > 0) {
            element.setAttrMap(attrMap);
        }
        if (valueHasChild) {
            jsonToIntermediate(value, element);
        }
        if (valueHasArray) {
            //put all array to his child
            buildJsonArray(value, element);
        }
    }

    public static List<String> getArrayElementMap(String arrayContent) {
        List<String> array = new ArrayList<>();
        int signMatchStart = 0;
        while (signMatchStart < arrayContent.length() - 1) {
            while (',' == arrayContent.charAt(signMatchStart)) {
                signMatchStart++;
            }
            JsonElement element = getContent(arrayContent, signMatchStart);
            String content = element.content.trim();
            array.add(content);
            signMatchStart = element.endIndex;
        }
        return array;
    }

    private static Map<String, String> getJsonElementMap(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        Pattern startPattern = Pattern.compile("\"(.*?)\"\\s*:");
        Matcher startMatcher = startPattern.matcher(json);
        while (startMatcher.find()) {
            String[] split = startMatcher.group().split("\"");
            final String elementName = split[split.length - 2];
            int signMatchStart = startMatcher.end();

            JsonElement element = getContent(json, signMatchStart);
            String content = element.content.trim();

            if (isNumber(content) || isBooleanString(content)) {
                content = "\"" + content + "\"";
            }
            if (content.startsWith("{") && getLegalElementCount(content) == 0) {
                content = "\"\"";
            }
            result.put(elementName, content);
            if (content.startsWith("{") || content.startsWith("[")) {
                startMatcher.region(element.endIndex, json.length());
            }
        }
        removeRepeatElement(result);
        return result;
    }

    private static int getLegalElementCount(String content) {
        int result = 0;
        Map<String, String> elementMap = getJsonElementMap(content);
        for (Map.Entry<String, String> entry : elementMap.entrySet()) {
            String elementName = entry.getKey();
            if (isIllegalElementName(elementName)) {
                continue;
            }
            result++;
        }
        return result;
    }

    public static boolean isAnObjectWithAttributes(String key, String value) {
        //case 1
        boolean hasObjKey = false;
        //case 2
        boolean allAttrKeyRight = true;
        //case 3
        boolean hasNoCurlyBraceInAttrValue = true;
        //case 4
        boolean hasNoBlankBraceInAttrValue = true;
        Map<String, String> elementMap = getJsonElementMap(value);
        for (Map.Entry<String, String> entry : elementMap.entrySet()) {
            String elementName = entry.getKey();
            String content = entry.getValue();
            if (("#" + key).equals(elementName)) {
                hasObjKey = true;
            } else {
                if (!elementName.startsWith("@") || elementName.length() <= 1) {
                    allAttrKeyRight = false;
                    break;
                }
                if (content.contains("{")) {
                    hasNoCurlyBraceInAttrValue = false;
                    break;
                }
                if (elementName.startsWith("@") && isArray(content) && !isBlankWrapWithBrace(content)) {
                    hasNoBlankBraceInAttrValue = false;
                }
            }
        }
        return hasObjKey && allAttrKeyRight && hasNoCurlyBraceInAttrValue && hasNoBlankBraceInAttrValue;
    }

    public static void removeRepeatElement(Map<String, String> result) {
        Iterator<Map.Entry<String, String>> iterator = result.entrySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            if (key.startsWith("@") || key.startsWith("#")) {
                if (result.containsKey(key.substring(1))) {
                    iterator.remove();
                }
            }
        }
    }

    public static JsonElement getContent(String json, int signMatchStart) {
        JsonElement result = new JsonElement();
        Pattern signPattern;
        signPattern = Pattern.compile("[\"{}\\w,\\d\\[\\]]");
        Matcher signMatcher = signPattern.matcher(json);
        signMatcher.region(signMatchStart, json.length());
        if (signMatcher.find()) {
            int valueStartIndex = signMatcher.start();
            String sign = signMatcher.group();
            if ("{".equals(sign) || "[".equals(sign)) {
                final int braceMatcherStart = signMatcher.end();
                final int braceMatcherEnd = getBraceMatcherEnd(json, braceMatcherStart, sign);
                String content = json.substring(signMatchStart, braceMatcherEnd);
                result.content = content;
                result.endIndex = braceMatcherEnd;
            } else if ("\"".equals(sign)) {
                while (signMatcher.find()) {
                    sign = signMatcher.group();
                    if ("\"".equals(sign)) {
                        break;
                    }
                }
                String value = json.substring(valueStartIndex, signMatcher.end());
                result.content = value;
                result.endIndex = signMatcher.end();
            } else {
                int endIndex = -1;
                while (signMatcher.find()) {
                    sign = signMatcher.group();
                    endIndex = signMatcher.end();
                    if (",".equals(sign) || "}".equals(sign) || "]".equals(sign)) {
                        endIndex--;
                        break;
                    }
                }
                if (endIndex == -1 && valueStartIndex == json.length() - 1) {
                    endIndex = json.length();
                }
                String value = json.substring(valueStartIndex, endIndex);
                result.content = value;
                result.endIndex = endIndex;
            }
        }
        result.content = null == result.content ? null : result.content.trim();
        return result;
    }

    public static int getBraceMatcherEnd(String json, int braceMatcherStart, String startSign) {
        Pattern bracePattern = Pattern.compile("[{}\\[\\]]");
        Matcher braceMatcher = bracePattern.matcher(json);
        braceMatcher.region(braceMatcherStart, json.length());
        Deque<String> stack = new ArrayDeque<>();
        stack.offer(startSign);
        while (braceMatcher.find()) {
            final String sign = braceMatcher.group();
            if (("}".equals(sign) && "{".equals(stack.getLast())) || ("]".equals(sign) && "[".equals(stack.getLast()))) {
                stack.pollLast();
            } else {
                stack.offer(sign);
            }
            if (stack.isEmpty()) {
                break;
            }
        }
        return braceMatcher.end();
    }


    private static boolean isJson(String content) {
        return content.startsWith("{");
    }

    private static boolean isBlankWrapWithBrace(String content) {
        return content.matches("\\{\\s*}") || content.matches("\\[\\s*]");
    }

    private static boolean isArray(String content) {
        return content.startsWith("[");
    }

    private static boolean isIllegalElementName(String elementName) {
        return "@".equals(elementName) || "#".equals(elementName) || elementName.length() < 1;
    }

    public static boolean isASingleValue(String content) {
        return isBlankWrapWithCurlyBrace(content)
                || "null".equals(content)
                || isBooleanString(content)
                || content.startsWith("\"");
    }

    private static boolean isBooleanString(String content) {
        return "false".equals(content)
                || "true".equals(content);
    }

    private static boolean isNumber(String content) {
        return content.matches("\\d+.?\\d*");
    }

    private static boolean isBlankWrapWithCurlyBrace(String content) {
        return content.matches("\\{\\s*}");
    }
}
