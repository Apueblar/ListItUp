package com.listitup.api.service;

import com.listitup.api.model.Item;
import com.listitup.api.model.CuratedList;
import com.listitup.api.repository.ItemRepository;
import com.listitup.api.repository.CuratedListRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.InetAddress;
import java.util.*;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CuratedListRepository listRepository;

    public ItemService(ItemRepository itemRepository, CuratedListRepository listRepository) {
        this.itemRepository = itemRepository;
        this.listRepository = listRepository;
    }

    @Transactional
    public Item addItem(UUID listId, Item item) {
        CuratedList list = listRepository.findById(listId)
                .orElseThrow(() -> new NoSuchElementException("List not found"));
        item.setList(list);
        if (item.getPositionIndex() == null) {
            int currentMax = list.getItems() != null ? list.getItems().size() : 0;
            item.setPositionIndex(currentMax + 1);
        }
        return itemRepository.save(item);
    }

    @Transactional
    public Item editItem(UUID itemId, Item updatedData) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));
        
        if (updatedData.getTitle() != null && !updatedData.getTitle().trim().isEmpty()) {
            item.setTitle(updatedData.getTitle().trim());
        }
        item.setDescription(updatedData.getDescription());
        item.setExternalUrl(updatedData.getExternalUrl());
        item.setPhoto(updatedData.getPhoto());
        
        if (updatedData.getPositionIndex() != null) {
            item.setPositionIndex(updatedData.getPositionIndex());
        }

        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new NoSuchElementException("Item not found");
        }
        itemRepository.deleteById(itemId);
    }

    @Transactional
    public List<Item> reorderItems(UUID listId, List<UUID> orderedItemIds) {
        CuratedList list = listRepository.findById(listId)
                .orElseThrow(() -> new NoSuchElementException("List not found"));

        List<Item> items = list.getItems();
        if (items == null) {
            return Collections.emptyList();
        }

        Map<UUID, Item> itemMap = new HashMap<>();
        for (Item item : items) {
            itemMap.put(item.getItemId(), item);
        }

        List<Item> updatedItems = new ArrayList<>();
        int index = 1;
        for (UUID id : orderedItemIds) {
            Item item = itemMap.get(id);
            if (item != null) {
                item.setPositionIndex(index++);
                updatedItems.add(itemRepository.save(item));
            }
        }
        return updatedItems;
    }

    public Map<String, String> fetchOpenGraph(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Invalid URL scheme");
        }
        
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Invalid URL");
        }
        
        InetAddress inetAddress = InetAddress.getByName(host);
        if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || 
            inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress()) {
            throw new SecurityException("Access to local network resources is forbidden");
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
        return ogData;
    }
}
