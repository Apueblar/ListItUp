package com.listitup.api.service;

import com.listitup.api.model.Item;
import com.listitup.api.model.CuratedList;
import com.listitup.api.repository.ItemRepository;
import com.listitup.api.repository.CuratedListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CuratedListRepository listRepository;

    @InjectMocks
    private ItemService itemService;

    private CuratedList testList;
    private Item testItem;

    @BeforeEach
    void setUp() {
        testList = new CuratedList();
        testList.setListId(UUID.randomUUID());
        testList.setItems(new ArrayList<>());

        testItem = new Item();
        testItem.setItemId(UUID.randomUUID());
        testItem.setTitle("Original Title");
        testItem.setDescription("Original Description");
        testItem.setExternalUrl("https://example.com");
        testItem.setPositionIndex(1);
    }

    @Test
    void testAddItem_Success() {
        when(listRepository.findById(testList.getListId())).thenReturn(Optional.of(testList));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item saved = itemService.addItem(testList.getListId(), testItem);
        assertNotNull(saved);
        assertEquals(testList, saved.getList());
        assertEquals(1, saved.getPositionIndex());
    }

    @Test
    void testAddItem_ListNotFound() {
        when(listRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> itemService.addItem(UUID.randomUUID(), testItem));
    }

    @Test
    void testEditItem_Success() {
        when(itemRepository.findById(testItem.getItemId())).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item updatedData = new Item();
        updatedData.setTitle("New Title");
        updatedData.setDescription("New Desc");

        Item result = itemService.editItem(testItem.getItemId(), updatedData);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Desc", result.getDescription());
    }

    @Test
    void testEditItem_NotFound() {
        when(itemRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> itemService.editItem(UUID.randomUUID(), new Item()));
    }

    @Test
    void testDeleteItem_Success() {
        when(itemRepository.existsById(testItem.getItemId())).thenReturn(true);
        doNothing().when(itemRepository).deleteById(testItem.getItemId());

        itemService.deleteItem(testItem.getItemId());
        verify(itemRepository, times(1)).deleteById(testItem.getItemId());
    }

    @Test
    void testDeleteItem_NotFound() {
        when(itemRepository.existsById(any())).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> itemService.deleteItem(UUID.randomUUID()));
    }

    @Test
    void testReorderItems_Success() {
        Item item2 = new Item();
        item2.setItemId(UUID.randomUUID());
        item2.setPositionIndex(2);

        testList.getItems().add(testItem);
        testList.getItems().add(item2);

        when(listRepository.findById(testList.getListId())).thenReturn(Optional.of(testList));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<UUID> newOrder = List.of(item2.getItemId(), testItem.getItemId());
        List<Item> reordered = itemService.reorderItems(testList.getListId(), newOrder);

        assertEquals(2, reordered.size());
        assertEquals(1, reordered.get(0).getPositionIndex());
        assertEquals(item2.getItemId(), reordered.get(0).getItemId());
        assertEquals(2, reordered.get(1).getPositionIndex());
        assertEquals(testItem.getItemId(), reordered.get(1).getItemId());
    }

    @Test
    void testFetchOpenGraph_InvalidScheme() {
        assertThrows(IllegalArgumentException.class, () -> itemService.fetchOpenGraph("ftp://invalid-url.com"));
    }

    @Test
    void testFetchOpenGraph_LocalAddressForbidden() {
        assertThrows(SecurityException.class, () -> itemService.fetchOpenGraph("http://127.0.0.1/admin"));
    }

    @Test
    void testFetchOpenGraph_HostnameInvalid() {
        assertThrows(Exception.class, () -> itemService.fetchOpenGraph("http:///path"));
    }
}
