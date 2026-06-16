package com.retail.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.server.common.Result;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/menu")
public class AdminMenuController {

    private static List<Map<String, Object>> cachedMenu = null;
    private static Map<String, List<String>> buttonPermissions = Map.of();

    private final ObjectMapper objectMapper;

    public AdminMenuController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        if (cachedMenu == null) {
            try {
                ClassPathResource resource = new ClassPathResource("menu.json");
                cachedMenu = objectMapper.readValue(resource.getInputStream(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } catch (Exception e) {
                return Result.fail(500, "菜单加载失败");
            }
        }
        return Result.success(cachedMenu);
    }

    @GetMapping("/buttons")
    public Result<Map<String, List<String>>> buttons() {
        return Result.success(buttonPermissions);
    }
}
