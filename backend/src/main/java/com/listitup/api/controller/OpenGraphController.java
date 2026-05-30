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
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL scheme"));
            }
            
            String host = uri.getHost();
            if (host == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL"));
            }
            
            java.net.InetAddress inetAddress = java.net.InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || 
                inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access to local network resources is forbidden"));
            }

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
