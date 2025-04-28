package nutritionCalculator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Recept osztály, amely egy teljes receptet reprezentál
 */
class Recipe implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Map<String, Double> ingredients; // összetevő név -> mennyiség (g)

    public Recipe(String name) {
        this.name = name;
        this.ingredients = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addIngredient(String name, double weight) {
        ingredients.put(name, weight);
    }

    public void removeIngredient(String name) {
        ingredients.remove(name);
    }

    public void clearIngredients() {
        ingredients.clear();
    }

    public Map<String, Double> getIngredients() {
        return new HashMap<>(ingredients);
    }
}

