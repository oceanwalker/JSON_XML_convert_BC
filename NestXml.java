package converter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NestXml {
    public static Element getElement(String input) {
        input = ConvertUtils.removeSpaceBetweenAngleBrackets(input);
        input = ConvertUtils.removeLine(input);
        return xmlToIntermediate(input, null);
    }

    private static Element xmlToIntermediate(String input, Element element) {
        List<Element> children = new ArrayList<>();
        List<String> tagList = findTagList(input);
        boolean inArray = isInArray(element, tagList);
        for (String tag : tagList) {
            if (hasXmlChildren(tag)) {
                String parentXml = getXmlParent(tag);
                String childrenXml = getXmlChildren(tag);
                Element fatherElement = buildeXmlToElement(parentXml, true);
                xmlToIntermediate(childrenXml, fatherElement);
                children.add(fatherElement);
            } else {
                Element singleElement = buildeXmlToElement(tag, false);
                children.add(singleElement);
            }
        }
        if (null == element) {
            element = children.get(0);
        } else {
            if (inArray) {
                element.setArray(children);
            } else {
                element.setChildren(children);
            }
        }
        return element;
    }

    private static boolean isInArray(Element element, List<String> tagList) {
        if (null == element || tagList.size() < 2) {
            return false;
        }
        boolean result = true;
        String firstName = findElementName(tagList.get(0));
        for (int i = 1; i < tagList.size(); i++) {
            if (!firstName.equals(findElementName(tagList.get(i)))) {
                result = false;
                break;
            }
        }
        return result;
    }

    public static Element buildeXmlToElement(String content, boolean hasChild) {
        //name
        String name = findElementName(content);
        Element element = new Element(name);
        //value
        if (!hasChild) {
            element.setValue(hasValue(content) ? "\"" + findValue(content) + "\"" : "null");
        }
        //attr
        LinkedHashMap<String, String> map = getAttributeMap(content);
        if (map.size() > 0) {
            element.setAttrMap(map);
        }
        return element;
    }

    public static String getXmlChildren(String tag) {
        tag = tag.substring(tag.indexOf(">") + 1);
        tag = tag.substring(0, tag.lastIndexOf("</"));
        return tag;
    }

    public static String getXmlParent(String tag) {
        return tag.substring(0, tag.indexOf(">") + 1);
    }

    private static LinkedHashMap<String, String> getAttributeMap(String tag) {
        Pattern pattern = Pattern.compile("(\\w+)(\\s*=\\s*)(['\"].*?['\"])");
        Matcher matcher = pattern.matcher(tag);
        var map = new LinkedHashMap<String, String>();
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(3).replaceAll("'", "\""));
        }
        return map;
    }

    public static boolean hasXmlChildren(String tag) {
        return tag.matches("^.*>.*<.*<.*$");
    }

    public static List<String> findTagList(String input) {
        List<String> result = new ArrayList<>();
        while (input.length() > 0) {
            if (input.contains("<?")) {
                input = input.substring(input.indexOf("?>") + 2);
            }
            String name = findElementName(input);
            final String noValueSuffix = "/>";
            final String hasValueSuffix = "/" + name + ">";
            final int noValueSuffixIndex = input.indexOf(noValueSuffix);
            final int hasValueSuffixIndex = input.indexOf(hasValueSuffix);
            final boolean hasChild = hasXmlChildren(input);
            final boolean isEntireContent = hasValueSuffixIndex == input.length() - hasValueSuffix.length();
            final boolean isOnlyOneSameNameTag = input.lastIndexOf("<" + name) == input.indexOf("<" + name);
            if ((!hasChild || isEntireContent) && isOnlyOneSameNameTag) {
                result.add(input);
                break;
            } else {
                int indexOfFirstEnd = 0;
                final boolean isFindHasValueSuffix = hasValueSuffixIndex > 0;
                if (!isOnlyOneSameNameTag) {
                    indexOfFirstEnd = findXmlEndIndexInMuiltySameNameTag(input, name);
                } else if (isFindHasValueSuffix) {
                    indexOfFirstEnd = hasValueSuffixIndex + 2 + name.length();
                } else {
                    indexOfFirstEnd = noValueSuffixIndex + 2;
                }
                result.add(input.substring(0, indexOfFirstEnd));
                input = input.substring(indexOfFirstEnd).trim();
            }
        }
        return result;
    }

    private static int findXmlEndIndexInMuiltySameNameTag(String input, String name) {

        final String openTag = "<" + name + "[\\s\"'=\\d\\w.]*?>";
        final String closeTag = "</" + name + ">";
        final String singleTag = "<" + name + "[\\s\"\\d\\w.]*?/>";
        Pattern pattern = Pattern.compile("(" + openTag + ")|(" + closeTag + ")|(" + singleTag + ")", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        int endIndex = -1;
        Deque<Boolean> stack = new LinkedList<>();
        while (matcher.find()) {
            endIndex = matcher.end();
            boolean isCloseTag = null != matcher.group(2);
            final boolean isSingleTag = null != matcher.group(3);
            if (isSingleTag) {
                if (stack.isEmpty()) {
                    break;
                } else {
                    continue;
                }
            }
            if (stack.isEmpty()) {
                stack.offer(isCloseTag);
            } else {
                if (false == stack.getLast() && isCloseTag) {
                    stack.pollLast();
                    if (stack.isEmpty()) {
                        break;
                    }
                } else {
                    stack.offer(isCloseTag);
                }
            }
        }
        return endIndex;
    }

    public static String findElementName(String input) {
        Pattern pattern = Pattern.compile("(<)([\\w\\d]+)(\\s|>|/)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    public static String findValue(String input) {
        return hasValue(input) ? input.substring(input.indexOf(">") + 1, input.indexOf("</")) : "";
    }

    private static boolean hasValue(String input) {
        return !input.contains("/>");
    }
}
