package org.zkoss.reporte.web.navigation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavigationController {
    @GetMapping("/")
    public String home() {
        return "forward:/index.zul";
    }

    @GetMapping("/*")
    public String homePage() {
        return "forward:/index.zul";
    }
}