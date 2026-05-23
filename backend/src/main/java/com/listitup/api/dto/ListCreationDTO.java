package com.listitup.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListCreationDTO {
    private String title;
    private String description;
    private UUID categoryId;
    private String coverPhoto;
    private List<ItemCreationDTO> items = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    
    public String getCoverPhoto() { return coverPhoto; }
    public void setCoverPhoto(String coverPhoto) { this.coverPhoto = coverPhoto; }
    
    public List<ItemCreationDTO> getItems() { return items; }
    public void setItems(List<ItemCreationDTO> items) { this.items = items; }
}
