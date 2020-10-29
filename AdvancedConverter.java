package converter;

import static converter.ConvertUtils.*;

import java.util.List;
import java.util.Map;

public class AdvancedConverter {
    public static String convert(String content) {
        if (content.startsWith("{")) {
            return intermediateToXML(NestJson.getElement(content));
        } else {
            return intermediateToJSON(NestXml.getElement(content));
        }
    }

    public static String intermediateToJSON(Element element) {
        return assemblyJsonBody(element, true, false);
    }

    private static String assemblyJsonBody(Element element, boolean needWrapWithCurlyBrace, boolean inArray) {
        StringBuilder body = new StringBuilder();
        if (element.hasAttr()) {
            for (Map.Entry<String, String> entry : element.getAttrMap().entrySet()) {
                joinPair(body, "@" + entry.getKey(), entry.getValue());
                body.append(",");
            }
        }
        if (element.hasChildren()) {
            StringBuilder childBuilder = new StringBuilder();
            final List<Element> children = element.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                childBuilder.append(assemblyJsonBody(child, false, false));
                if (i != children.size() - 1) {
                    childBuilder.append(",");
                }
            }
            if (element.hasAttr()) {
                warpWithCurlyBrace(childBuilder);
                joinPair(body, "#" + element.getName(), childBuilder.toString());
            } else {
                body.append(childBuilder);
            }
        } else if (element.hasArray()) {
            StringBuilder arrayBuilder = new StringBuilder();
            final List<Element> array = element.getArray();
            for (int i = 0; i < array.size(); i++) {
                Element arrayElement = array.get(i);
                arrayBuilder.append(assemblyJsonBody(arrayElement, false, true));
                if (i != array.size() - 1) {
                    arrayBuilder.append(",");
                }
            }
            warpWithSuareBrace(arrayBuilder);
            if (element.hasAttr()) {
                joinPair(body, "#" + element.getName(), arrayBuilder.toString());
            } else {
                body.append(arrayBuilder);
            }
        } else if (element.hasValue()) {
            if (element.hasAttr()) {
                joinPair(body, "#" + element.getName(), element.getValue());
            } else {
                body.append(element.getValue());
            }
        }
        if (element.hasAttr() && !element.hasValue() && !element.hasChildren() && !element.hasArray()) {
            body.deleteCharAt(body.length() - 1);
        }
        StringBuilder result = new StringBuilder();
        if (element.hasAttr() || element.hasChildren()) {
            warpWithCurlyBrace(body);
        }
        if (inArray) {
            result = body;
        } else {
            joinPair(result, element.getName(), body.toString());
        }
        if (needWrapWithCurlyBrace) {
            warpWithCurlyBrace(result);
        }
        return result.toString();
    }

    public static String intermediateToXML(Element element) {
        StringBuilder result = new StringBuilder();
        //head
        result.append(element.getName());
        if (element.hasAttr()) {
            for (var pair : element.getAttrMap().entrySet()) {
                result.append(" ").append(pair.getKey()).append("=").append(pair.getValue());
            }
        }
        //tail
        StringBuilder tailBuilder = new StringBuilder().append("/").append(element.getName());
        wrapWithAngleBrackets(tailBuilder);
        //middle
        if (!element.hasChildren() && null == element.getValue() && !element.hasArray()) {
            wrapWithAngleBrackets(result.append(" ").append("/"));
        } else {
            if (element.hasChildren()) {
                wrapWithAngleBrackets(result);
                final List<Element> children = element.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    result.append(intermediateToXML(children.get(i)));
                }
                result.append(tailBuilder);
            } else if (element.hasArray()) {
                wrapWithAngleBrackets(result);
                final List<Element> array = element.getArray();
                for (int i = 0; i < array.size(); i++) {
                    Element arrayElement = array.get(i);
                    if ("".equals(arrayElement.getName())) {
                        arrayElement.setName("element");
                    }
                    result.append(intermediateToXML(arrayElement));
                }
                result.append(tailBuilder);
            } else {
                if (null != element.getValue() && !"null".equals(element.getValue())) {
                    wrapWithAngleBrackets(result);
                    result.append(removeDoubleQuotesIfExist(element.getValue()));
                    result.append(tailBuilder);
                } else {
                    result.append("/");
                    wrapWithAngleBrackets(result);
                }
            }
        }
        return result.toString();
    }
}