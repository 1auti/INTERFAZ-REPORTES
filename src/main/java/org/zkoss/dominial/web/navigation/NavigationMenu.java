package org.zkoss.dominial.web.navigation;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NavigationMenu {
    private String label;
    private String icon;
    private String path;
    private int counter;
    private List<NavigationMenu> subMenus = new ArrayList<>();
    private boolean expanded = false;
    private String title;

    public NavigationMenu(String label) {
        this.label = label;
    }

    public NavigationMenu(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public String toString() {
        return "Menu{" +
                "label='" + label + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}