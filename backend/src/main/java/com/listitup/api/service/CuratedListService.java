package com.listitup.api.service;

import com.listitup.api.model.CuratedList;
import com.listitup.api.repository.CuratedListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CuratedListService {

    private final CuratedListRepository listRepository;

    public CuratedListService(CuratedListRepository listRepository) {
        this.listRepository = listRepository;
    }

    @Transactional(readOnly = true)
    public List<CuratedList> getAllLists() {
        return listRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CuratedList> getListById(UUID id) {
        return listRepository.findById(id);
    }

    @Transactional
    public CuratedList saveList(CuratedList list) {
        return listRepository.save(list);
    }
    
    @Transactional
    public void deleteList(UUID id) {
        listRepository.deleteById(id);
    }
}
