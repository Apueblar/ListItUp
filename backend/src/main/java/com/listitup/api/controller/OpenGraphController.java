package com.listitup.api.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class OpenGraphController {

    @GetMapping("/og")
    public ResponseEntity<?> fetchOpenGraph(@RequestParam String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
            String title = doc.select("meta[property=og:title]").attr("content");
            if (title == null || title.isEmpty()) {
                title = doc.title();
            }
            String description = doc.select("meta[property=og:description]").attr("content");
            String image = doc.select("meta[property=og:image]").attr("content");

            Map<String, String> ogData = new HashMap<>();
            ogData.put("title", title);
            ogData.put("description", description);
            ogData.put("image", image);
            return ResponseEntity.ok(ogData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch OpenGraph metadata"));
        }
    }
}
