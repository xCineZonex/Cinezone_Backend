package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.ComboRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRecipeRepository extends JpaRepository<ComboRecipe, Long> {
    List<ComboRecipe> findByComboProductId(Long comboProductId);
    void deleteByComboProductId(Long comboProductId);
    void deleteByIngredientProductId(Long ingredientProductId);
}
