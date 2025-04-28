package nutritionCalculator;

import java.util.HashMap;
import java.util.Map;

/**
 * Tápérték adatbázis osztály, amely tárolja az összetevők tápérték adatait
 */
class NutritionDatabase {
    private Map<String, NutritionInfo> database;

    public NutritionDatabase() {
        database = new HashMap<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        // Alapvető összetevők hozzáadása

        // BL80 liszt (100g-ra vonatkoztatott értékek)
        NutritionInfo bl80 = new NutritionInfo();
        bl80.setValue("energy", 1470);
        bl80.setValue("energyKcal", 347);
        bl80.setValue("fat", 1.3);
        bl80.setValue("saturatedFat", 0.2);
        bl80.setValue("carbs", 70.6);
        bl80.setValue("sugar", 1.3);
        bl80.setValue("fiber", 3.2);
        bl80.setValue("protein", 10.1);
        bl80.setValue("salt", 0.01);

        // Részletesebb adatok
        bl80.setValue("iron", 1.3);
        bl80.setValue("magnesium", 25);
        bl80.setValue("potassium", 130);
        bl80.setValue("calcium", 15);
        bl80.setValue("vitaminB1", 0.3);
        bl80.setValue("vitaminB3", 3.0);

        database.put("BL80 liszt", bl80);

        // Víz (100g)
        NutritionInfo water = new NutritionInfo();
        database.put("Víz", water);

        // Só (100g)
        NutritionInfo salt = new NutritionInfo();
        salt.setValue("salt", 100);
        salt.setValue("sodium", 39000);
        database.put("Só", salt);

        // Kovász (50% víz, 50% BL80 liszt)
        NutritionInfo sourdough = new NutritionInfo();
        sourdough.setValue("energy", bl80.getValue("energy") * 0.5);
        sourdough.setValue("energyKcal", bl80.getValue("energyKcal") * 0.5);
        sourdough.setValue("fat", bl80.getValue("fat") * 0.5);
        sourdough.setValue("saturatedFat", bl80.getValue("saturatedFat") * 0.5);
        sourdough.setValue("carbs", bl80.getValue("carbs") * 0.5);
        sourdough.setValue("sugar", bl80.getValue("sugar") * 0.5);
        sourdough.setValue("fiber", bl80.getValue("fiber") * 0.5);
        sourdough.setValue("protein", bl80.getValue("protein") * 0.5);
        sourdough.setValue("salt", bl80.getValue("salt") * 0.5);
        database.put("Kovász", sourdough);

        // Cukor (100g)
        NutritionInfo sugar = new NutritionInfo();
        sugar.setValue("energy", 1700);
        sugar.setValue("energyKcal", 400);
        sugar.setValue("carbs", 100);
        sugar.setValue("sugar", 100);
        database.put("Cukor", sugar);

        // Vaj (100g)
        NutritionInfo butter = new NutritionInfo();
        butter.setValue("energy", 3050);
        butter.setValue("energyKcal", 735);
        butter.setValue("fat", 81);
        butter.setValue("saturatedFat", 50.5);
        butter.setValue("monounsaturatedFat", 21);
        butter.setValue("polyunsaturatedFat", 3);
        butter.setValue("transFat", 3.3);
        butter.setValue("cholesterol", 215);
        butter.setValue("carbs", 0.6);
        butter.setValue("sugar", 0.6);
        butter.setValue("protein", 0.7);
        butter.setValue("salt", 0.1);
        butter.setValue("vitaminA", 750);
        butter.setValue("vitaminD", 1.3);
        butter.setValue("vitaminE", 2.3);
        database.put("Vaj", butter);

        // Tojás (100g)
        NutritionInfo egg = new NutritionInfo();
        egg.setValue("energy", 606);
        egg.setValue("energyKcal", 145);
        egg.setValue("fat", 10.3);
        egg.setValue("saturatedFat", 3.1);
        egg.setValue("monounsaturatedFat", 4.1);
        egg.setValue("polyunsaturatedFat", 1.4);
        egg.setValue("cholesterol", 372);
        egg.setValue("carbs", 0.4);
        egg.setValue("sugar", 0.4);
        egg.setValue("protein", 12.6);
        egg.setValue("salt", 0.37);
        egg.setValue("vitaminA", 160);
        egg.setValue("vitaminD", 1.8);
        egg.setValue("vitaminE", 1.1);
        egg.setValue("vitaminB12", 1.3);
        egg.setValue("iron", 1.9);
        database.put("Tojás", egg);

        // Teljes kiőrlésű liszt (100g)
        NutritionInfo wholeWheatFlour = new NutritionInfo();
        wholeWheatFlour.setValue("energy", 1420);
        wholeWheatFlour.setValue("energyKcal", 339);
        wholeWheatFlour.setValue("fat", 2.5);
        wholeWheatFlour.setValue("saturatedFat", 0.4);
        wholeWheatFlour.setValue("carbs", 63);
        wholeWheatFlour.setValue("sugar", 2.3);
        wholeWheatFlour.setValue("fiber", 10.7);
        wholeWheatFlour.setValue("protein", 13.2);
        wholeWheatFlour.setValue("salt", 0.01);
        wholeWheatFlour.setValue("iron", 3.9);
        wholeWheatFlour.setValue("magnesium", 120);
        wholeWheatFlour.setValue("zinc", 2.9);
        database.put("Teljes kiőrlésű liszt", wholeWheatFlour);

        // Élesztő (100g)
        NutritionInfo yeast = new NutritionInfo();
        yeast.setValue("energy", 412);
        yeast.setValue("energyKcal", 98);
        yeast.setValue("fat", 1.1);
        yeast.setValue("carbs", 3.9);
        yeast.setValue("protein", 16.9);
        yeast.setValue("salt", 0.04);
        yeast.setValue("vitaminB1", 1.5);
        yeast.setValue("vitaminB2", 1.9);
        yeast.setValue("vitaminB3", 12.0);
        yeast.setValue("vitaminB6", 0.6);
        yeast.setValue("folate", 1250);
        database.put("Élesztő", yeast);
    }

    public String[] getAvailableIngredients() {
        return database.keySet().toArray(new String[0]);
    }

    public NutritionInfo getNutritionInfo(String ingredient) {
        return database.get(ingredient);
    }

    public void addIngredient(String name, NutritionInfo info) {
        database.put(name, info);
    }

    public void removeIngredient(String name) {
        database.remove(name);
    }
}

