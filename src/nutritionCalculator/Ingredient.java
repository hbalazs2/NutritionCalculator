package nutritionCalculator;

/**
 * Összetevő osztály, amely egy összetevőt reprezentál a receptben
 */
class Ingredient {
    private String name;
    private double weight;  // grammban
    private NutritionInfo nutritionInfo;

    public Ingredient(String name, double weight, NutritionInfo nutritionInfo) {
        this.name = name;
        this.weight = weight;
        this.nutritionInfo = nutritionInfo;
    }

    public String getName() { return name; }
    public double getWeight() { return weight; }
    public NutritionInfo getNutritionInfo() { return nutritionInfo; }
}

