package nutritionCalculator;

/**
 * OpenFoodFacts API-ból beolvasott termék reprezentálása
 */
class FoodProduct {
    private String id;
    private String name;
    private NutritionInfo nutritionInfo;

    public FoodProduct(String id, String name, NutritionInfo nutritionInfo) {
        this.id = id;
        this.name = name;
        this.nutritionInfo = nutritionInfo;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public NutritionInfo getNutritionInfo() { return nutritionInfo; }
}
