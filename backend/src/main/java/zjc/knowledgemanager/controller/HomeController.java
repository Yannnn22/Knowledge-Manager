package zjc.knowledgemanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 */
@Controller
public class HomeController {

    /**
     * 根路径重定向到Swagger UI
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui/index.html";
    }

    /**
     * API文档页面重定向到Swagger UI
     */
    @GetMapping("/api-docs")
    public String apiDocs() {
        return "redirect:/swagger-ui/index.html";
    }

    /**
     * 静态API文档页面
     */
    @GetMapping("/docs")
    public String staticDocs() {
        return "redirect:/api-docs.html";
    }
}