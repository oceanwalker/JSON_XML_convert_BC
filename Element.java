package converter;

import java.util.*;

public class Element {

    private String value;
    private String name;
    private Map<String, String> attrMap;
    private List<Element> children;
    private List<Element> array;

    public Element(String name) {
        this.name = name;
        attrMap = new LinkedHashMap<>();
        children = new ArrayList<>();
        array = new ArrayList<>();
    }

    public Map<String, String> getAttrMap() {
        return attrMap;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setAttrMap(Map<String, String> attrMap) {
        this.attrMap = attrMap;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setChildren(List<Element> children) {
        this.children = children;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public boolean hasArray() {
        return array.size() > 0;
    }

    public List<Element> getChildren() {
        return children;
    }

    public boolean hasAttr() {
        return attrMap.size() > 0;
    }

    public boolean hasValue() {
        return null != value;
    }

    public void setArray(List<Element> array) {
        this.array = array;
    }

    public List<Element> getArray() {
        return array;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Element element = (Element) o;

        if (value != null ? !value.equals(element.value) : element.value != null) return false;
        if (name != null ? !name.equals(element.name) : element.name != null) return false;
        if (attrMap != null ? !attrMap.equals(element.attrMap) : element.attrMap != null) return false;
        if (children != null ? !children.equals(element.children) : element.children != null) return false;
        return array != null ? array.equals(element.array) : element.array == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (attrMap != null ? attrMap.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        result = 31 * result + (array != null ? array.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Element{" +
                "value='" + value + '\'' +
                ", name='" + name + '\'' +
                ", attrMap=" + attrMap +
                ", children=" + children +
                ", array=" + array +
                '}';
    }
}
