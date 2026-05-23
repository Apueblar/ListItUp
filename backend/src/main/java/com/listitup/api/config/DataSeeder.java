package com.listitup.api.config;

import com.listitup.api.model.Category;
import com.listitup.api.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = List.of(
                    createCategory("Movies", "🎬"),
                    createCategory("Books", "📚"),
                    createCategory("Tech", "💻"),
                    createCategory("Travel", "✈️"),
                    createCategory("Food", "🍔"),
                    createCategory("Music", "🎵"),
                    createCategory("Games", "🎮"),
                    createCategory("Sports", "⚽")
            );
            categoryRepository.saveAll(defaultCategories);
        }
    }

    private Category createCategory(String name, String icon) {
        Category category = new Category();
        category.setName(name);
        category.setIcon(icon);
        return category;
    }
}
