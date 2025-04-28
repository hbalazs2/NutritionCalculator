import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tápérték információ osztály, amely tárolja egy összetevő tápértékeit
 */
class NutritionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    // Tápértékek tárolása
    private Map<String, Double> values;

    public NutritionInfo() {
        values = new HashMap<>();
        initializeDefaultValues();
    }

    private void initializeDefaultValues() {
        // Alapvető tápértékek inicializálása 0-ra
        for (String nutrient : getAllNutrients()) {
            values.put(nutrient, 0.0);
        }
    }

    public static List<String> getAllNutrients() {
        List<String> nutrients = new ArrayList<>();

        // Alapvető tápértékek
        nutrients.add("energy");        // kJ
        nutrients.add("energyKcal");    // kcal
        nutrients.add("fat");           // g
        nutrients.add("saturatedFat");  // g
        nutrients.add("monounsaturatedFat"); // g
        nutrients.add("polyunsaturatedFat"); // g
        nutrients.add("transFat");      // g
        nutrients.add("cholesterol");   // mg
        nutrients.add("carbs");         // g
        nutrients.add("sugar");         // g
        nutrients.add("starch");        // g
        nutrients.add("fiber");         // g
        nutrients.add("protein");       // g
        nutrients.add("salt");          // g
        nutrients.add("sodium");        // mg

        // Vitaminok
        nutrients.add("vitaminA");      // µg
        nutrients.add("vitaminC");      // mg
        nutrients.add("vitaminD");      // µg
        nutrients.add("vitaminE");      // mg
        nutrients.add("vitaminK");      // µg
        nutrients.add("vitaminB1");     // mg
        nutrients.add("vitaminB2");     // mg
        nutrients.add("vitaminB3");     // mg
        nutrients.add("vitaminB6");     // mg
        nutrients.add("vitaminB12");    // µg
        nutrients.add("folate");        // µg

        // Ásványi anyagok
        nutrients.add("calcium");       // mg
        nutrients.add("iron");          // mg
        nutrients.add("magnesium");     // mg
        nutrients.add("phosphorus");    // mg
        nutrients.add("potassium");     // mg
        nutrients.add("zinc");          // mg

        return nutrients;
    }

    public double getValue(String nutrient) {
        return values.getOrDefault(nutrient, 0.0);
    }

    public void setValue(String nutrient, double value) {
        values.put(nutrient, value);
    }
}

