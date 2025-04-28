import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import org.json.simple.JSONArray;
//import org.json.JSONObject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Továbbfejlesztett Sütemények és Pékáruk Tápérték Kalkulátora
 * - OpenFoodFacts API integráció
 * - Receptek mentése/betöltése
 * - Részletesebb tápérték adatok
 * - Felhasználói összetevők hozzáadása
 */
public class NutritionCalculator extends JFrame {

    // GUI komponensek
    private final JTable ingredientTable;
    private final DefaultTableModel tableModel;
    private final JPanel resultPanel;
    private final JTabbedPane tabbedPane;

    // Adatbázis és egyéb globális objektumok
    private final NutritionDatabase nutritionDb;
    private Recipe currentRecipe;
    private final DecimalFormat df = new DecimalFormat("#.##");

    // Menüpontok
    private JMenuItem saveMenuItem;
    private JMenuItem loadMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem searchApiMenuItem;
    private JMenuItem addCustomIngredientMenuItem;

    public NutritionCalculator() {
        // Alap beállítások
        setTitle("Tápérték Kalkulátor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Adatbázis inicializálása
        nutritionDb = new NutritionDatabase();
        currentRecipe = new Recipe("Új recept");

        // Menüsor létrehozása
        createMenuBar();

        // Táblázat modell létrehozása
        String[] columnNames = {"Összetevő", "Mennyiség (g)"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) {
                    return Double.class;
                }
                return String.class;
            }
        };

        // Táblázat inicializálása
        ingredientTable = new JTable(tableModel);

        // Fül panel létrehozása
        tabbedPane = new JTabbedPane();

        // Recept panel létrehozása
        JPanel recipePanel = createRecipePanel();
        tabbedPane.addTab("Recept szerkesztés", recipePanel);

        // Eredmény panel létrehozása
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Tápérték (100g termékre)"));

        JScrollPane resultScrollPane = new JScrollPane(resultPanel);
        resultScrollPane.setPreferredSize(new Dimension(250, 600));

        // Fő panelba elemek hozzáadása
        add(tabbedPane, BorderLayout.CENTER);
        add(resultScrollPane, BorderLayout.EAST);

        // Kezdeti állapot beállítása
        updateRecipeNameDisplay();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Fájl menü
        JMenu fileMenu = new JMenu("Fájl");
        saveMenuItem = new JMenuItem("Recept mentése");
        loadMenuItem = new JMenuItem("Recept betöltése");
        exitMenuItem = new JMenuItem("Kilépés");

        fileMenu.add(saveMenuItem);
        fileMenu.add(loadMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        // Adatbázis menü
        JMenu databaseMenu = new JMenu("Adatbázis");
        searchApiMenuItem = new JMenuItem("Keresés OpenFoodFacts-ben");
        addCustomIngredientMenuItem = new JMenuItem("Saját összetevő hozzáadása");

        databaseMenu.add(searchApiMenuItem);
        databaseMenu.add(addCustomIngredientMenuItem);

        // Menüelemek hozzáadása
        menuBar.add(fileMenu);
        menuBar.add(databaseMenu);
        setJMenuBar(menuBar);

        // Eseménykezelők hozzáadása
        saveMenuItem.addActionListener(e -> saveRecipe());
        loadMenuItem.addActionListener(e -> loadRecipe());
        exitMenuItem.addActionListener(e -> System.exit(0));
        searchApiMenuItem.addActionListener(e -> searchOpenFoodFacts());
        addCustomIngredientMenuItem.addActionListener(e -> addCustomIngredient());
    }

    private JPanel createRecipePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Név panel
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel nameLabel = new JLabel("Recept neve:");
        JTextField nameField = new JTextField(20);
        nameField.setText(currentRecipe.getName());
        JButton updateNameBtn = new JButton("Név frissítése");

        namePanel.add(nameLabel);
        namePanel.add(nameField);
        namePanel.add(updateNameBtn);

        updateNameBtn.addActionListener(e -> {
            currentRecipe.setName(nameField.getText());
            updateRecipeNameDisplay();
        });

        panel.add(namePanel, BorderLayout.NORTH);

        // Összetevők táblázata
        JScrollPane scrollPane = new JScrollPane(ingredientTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Gomb panel
        JPanel buttonPanel = new JPanel();
        JButton addIngredientBtn = new JButton("Összetevő hozzáadása");
        JButton removeIngredientBtn = new JButton("Kijelölt összetevő törlése");
        JButton calculateBtn = new JButton("Tápérték számítása");

        buttonPanel.add(addIngredientBtn);
        buttonPanel.add(removeIngredientBtn);
        buttonPanel.add(calculateBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Eseménykezelők
        addIngredientBtn.addActionListener(e -> addIngredient());
        removeIngredientBtn.addActionListener(e -> removeIngredient());
        calculateBtn.addActionListener(e -> calculateNutrition());

        return panel;
    }

    private void updateRecipeNameDisplay() {
        SwingUtilities.invokeLater(() -> {
            setTitle("Tápérték Kalkulátor - " + currentRecipe.getName());
        });
    }

    private void addIngredient() {
        String[] availableIngredients = nutritionDb.getAvailableIngredients();

        if (availableIngredients.length == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nincs elérhető összetevő az adatbázisban. Kérlek, adj hozzá összetevőket először.",
                    "Hiba",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ingredient = (String) JOptionPane.showInputDialog(
                this,
                "Válassz összetevőt:",
                "Összetevő hozzáadása",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableIngredients,
                availableIngredients[0]);

        if (ingredient != null) {
            String weightStr = JOptionPane.showInputDialog(
                    this,
                    "Add meg a mennyiséget grammban:",
                    "Mennyiség",
                    JOptionPane.QUESTION_MESSAGE);

            try {
                double weight = Double.parseDouble(weightStr);
                tableModel.addRow(new Object[]{ingredient, weight});

                // Hozzáadjuk a recepthez is
                currentRecipe.addIngredient(ingredient, weight);

                // Frissítjük a táblázatot
                tableModel.fireTableDataChanged();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Érvénytelen szám. Kérlek, csak számot adj meg.",
                        "Hiba",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeIngredient() {
        int selectedRow = ingredientTable.getSelectedRow();
        if (selectedRow != -1) {
            String ingredientName = (String) tableModel.getValueAt(selectedRow, 0);
            currentRecipe.removeIngredient(ingredientName);
            tableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Kérlek, válassz ki egy összetevőt a törléshez.",
                    "Figyelmeztetés",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Recept kalkuláció módosítása sütési korrekciós faktorral
     */
    private void calculateNutrition() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Adj hozzá legalább egy összetevőt a számításhoz.",
                    "Figyelmeztetés",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Összetevők összegyűjtése a táblázatból
        List<Ingredient> ingredients = new ArrayList<>();
        double totalWeight = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            double weight = (Double) tableModel.getValueAt(i, 1);

            NutritionInfo info = nutritionDb.getNutritionInfo(name);
            if (info != null) {
                ingredients.add(new Ingredient(name, weight, info));
                totalWeight += weight;
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Nem található tápérték információ a következő összetevőhöz: " + name,
                        "Hiányzó adat",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // A sütés előtti tápérték számítása
        NutritionInfo rawResult = calculateTotalNutrition(ingredients, totalWeight);

        // Sütés utáni korrekciós tényező meghatározása
        boolean applyBakingCorrection = false;
        double bakedWeight = totalWeight;

        // Megkérdezzük a felhasználót a korrekciós faktorról
        int option = JOptionPane.showConfirmDialog(
                this,
                "Szeretnéd alkalmazni a sütési korrekciót a tápértékekre?",
                "Sütési korrekció",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            applyBakingCorrection = true;

            // Bekérjük a sütés utáni súlyt
            String bakedWeightStr = JOptionPane.showInputDialog(
                    this,
                    "Add meg a végtermék súlyát sütés után (gramm):",
                    "Sütés utáni súly",
                    JOptionPane.QUESTION_MESSAGE);

            try {
                bakedWeight = Double.parseDouble(bakedWeightStr);

                if (bakedWeight <= 0 || bakedWeight > totalWeight) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Érvénytelen súly! A sütés utáni súly nem lehet nagyobb, mint a sütés előtti.",
                            "Hiba",
                            JOptionPane.ERROR_MESSAGE);
                    bakedWeight = totalWeight;
                    applyBakingCorrection = false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Érvénytelen szám. A korrekció nem lesz alkalmazva.",
                        "Hiba",
                        JOptionPane.ERROR_MESSAGE);
                applyBakingCorrection = false;
            }
        }

        // Tápérték számítása korrekció alkalmazásával vagy anélkül
        NutritionInfo finalResult;

        if (applyBakingCorrection) {
            // Korrekciós faktor számítása (sütés előtti / sütés utáni súly)
            double correctionFactor = totalWeight / bakedWeight;
            finalResult = applyCorrectionFactor(rawResult, correctionFactor);

            // Eredmények megjelenítése külön paneleken
            displayBothResults(rawResult, finalResult, totalWeight, bakedWeight);
        } else {
            // Csak a nyers eredményt jelenítjük meg
            displayResults(rawResult);
        }
    }

    private NutritionInfo calculateTotalNutrition(List<Ingredient> ingredients, double totalWeight) {
        // Alapértelmezett tápérték értékek
        Map<String, Double> totalValues = new HashMap<>();

        // Az összes lehetséges tápérték inicializálása 0-ra
        for (String nutrient : NutritionInfo.getAllNutrients()) {
            totalValues.put(nutrient, 0.0);
        }

        // Összetevők tápértékeinek összegzése
        for (Ingredient ingredient : ingredients) {
            NutritionInfo info = ingredient.getNutritionInfo();
            double weight = ingredient.getWeight();

            // Minden tápanyag összegzése
            for (String nutrient : NutritionInfo.getAllNutrients()) {
                double currentValue = totalValues.get(nutrient);
                double ingredientValue = info.getValue(nutrient) * weight / 100.0;
                totalValues.put(nutrient, currentValue + ingredientValue);
            }
        }

        // Átszámítás 100g-ra
        double factor = 100.0 / totalWeight;

        NutritionInfo result = new NutritionInfo();
        for (String nutrient : NutritionInfo.getAllNutrients()) {
            result.setValue(nutrient, totalValues.get(nutrient) * factor);
        }

        return result;
    }

    /**
     * Korrekciós faktor alkalmazása a tápértékekre
     */
    private NutritionInfo applyCorrectionFactor(NutritionInfo original, double factor) {
        NutritionInfo corrected = new NutritionInfo();

        // Minden tápanyagra alkalmazzuk a korrekciós faktort
        for (String nutrient : NutritionInfo.getAllNutrients()) {
            // A víztartalom kivétel lehet, de mivel nem számoljuk külön, így minden értékre alkalmazzuk
            corrected.setValue(nutrient, original.getValue(nutrient) * factor);
        }

        return corrected;
    }

    /**
     * Mind a sütés előtti, mind a sütés utáni eredmények megjelenítése
     */
    private void displayBothResults(NutritionInfo rawResult, NutritionInfo bakedResult,
                                    double rawWeight, double bakedWeight) {
        // Töröljük a korábbi eredményeket
        resultPanel.removeAll();

        // Fő cím
        JLabel titleLabel = new JLabel("Tápérték összehasonlítás");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(titleLabel);
        resultPanel.add(Box.createVerticalStrut(15));

        // Két panel létrehozása egymás mellett (vagy alatt)
        JPanel rawPanel = new JPanel();
        rawPanel.setLayout(new BoxLayout(rawPanel, BoxLayout.Y_AXIS));
        rawPanel.setBorder(BorderFactory.createTitledBorder("Sütés előtt (100g)"));

        JPanel bakedPanel = new JPanel();
        bakedPanel.setLayout(new BoxLayout(bakedPanel, BoxLayout.Y_AXIS));
        bakedPanel.setBorder(BorderFactory.createTitledBorder("Sütés után (100g)"));

        // Információs címkék
        JLabel rawInfoLabel = new JLabel(String.format("Összsúly: %.0fg", rawWeight));
        rawInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rawPanel.add(rawInfoLabel);

        JLabel bakedInfoLabel = new JLabel(String.format("Összsúly: %.0fg", bakedWeight));
        bakedInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bakedPanel.add(bakedInfoLabel);

        JLabel weightLossLabel = new JLabel(String.format("Súlyveszteség: %.1f%%",
                (1 - bakedWeight/rawWeight) * 100));
        weightLossLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bakedPanel.add(weightLossLabel);

        rawPanel.add(Box.createVerticalStrut(10));
        bakedPanel.add(Box.createVerticalStrut(10));

        // Energia értékek
        addComparisonRow(rawPanel, bakedPanel, "Energia (kJ)",
                rawResult.getValue("energy"), bakedResult.getValue("energy"));
        addComparisonRow(rawPanel, bakedPanel, "Energia (kcal)",
                rawResult.getValue("energyKcal"), bakedResult.getValue("energyKcal"));

        rawPanel.add(Box.createVerticalStrut(5));
        bakedPanel.add(Box.createVerticalStrut(5));

        // Makrotápanyagok
        addComparisonRow(rawPanel, bakedPanel, "Zsír (g)",
                rawResult.getValue("fat"), bakedResult.getValue("fat"));
        addComparisonRow(rawPanel, bakedPanel, "Szénhidrát (g)",
                rawResult.getValue("carbs"), bakedResult.getValue("carbs"));
        addComparisonRow(rawPanel, bakedPanel, "- ebből cukor (g)",
                rawResult.getValue("sugar"), bakedResult.getValue("sugar"));
        addComparisonRow(rawPanel, bakedPanel, "Rost (g)",
                rawResult.getValue("fiber"), bakedResult.getValue("fiber"));
        addComparisonRow(rawPanel, bakedPanel, "Fehérje (g)",
                rawResult.getValue("protein"), bakedResult.getValue("protein"));
        addComparisonRow(rawPanel, bakedPanel, "Só (g)",
                rawResult.getValue("salt"), bakedResult.getValue("salt"));

        // Panelek hozzáadása a fő panelhez
        JPanel comparisonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        comparisonPanel.add(rawPanel);
        comparisonPanel.add(bakedPanel);

        resultPanel.add(comparisonPanel);

        // További részletes tápérték adatok (opcionális)
        JButton showDetailsBtn = new JButton("További részletek megjelenítése");
        showDetailsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(Box.createVerticalStrut(15));
        resultPanel.add(showDetailsBtn);

        showDetailsBtn.addActionListener(e -> {
            // Új ablak a részletes tápérték adatokhoz
            JDialog detailsDialog = new JDialog(this, "Részletes tápérték adatok", true);
            detailsDialog.setSize(700, 500);
            detailsDialog.setLayout(new BorderLayout());

            JTabbedPane detailsTabs = new JTabbedPane();

            // Sütés előtti részletek panel
            JPanel rawDetailsPanel = new JPanel();
            rawDetailsPanel.setLayout(new BoxLayout(rawDetailsPanel, BoxLayout.Y_AXIS));
            JScrollPane rawScrollPane = new JScrollPane(rawDetailsPanel);

            // Sütés utáni részletek panel
            JPanel bakedDetailsPanel = new JPanel();
            bakedDetailsPanel.setLayout(new BoxLayout(bakedDetailsPanel, BoxLayout.Y_AXIS));
            JScrollPane bakedScrollPane = new JScrollPane(bakedDetailsPanel);

            // Részletes tápérték adatok hozzáadása
            addDetailedNutrition(rawDetailsPanel, rawResult);
            addDetailedNutrition(bakedDetailsPanel, bakedResult);

            detailsTabs.addTab("Sütés előtt", rawScrollPane);
            detailsTabs.addTab("Sütés után", bakedScrollPane);

            detailsDialog.add(detailsTabs, BorderLayout.CENTER);

            // Bezárás gomb
            JButton closeBtn = new JButton("Bezárás");
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeBtn);
            detailsDialog.add(buttonPanel, BorderLayout.SOUTH);

            closeBtn.addActionListener(event -> detailsDialog.dispose());

            // Megjelenítés
            detailsDialog.setLocationRelativeTo(this);
            detailsDialog.setVisible(true);
        });

        // Panel frissítése
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    /**
     * Részletes tápérték adatok hozzáadása egy panelhez
     */
    private void addDetailedNutrition(JPanel panel, NutritionInfo info) {
        // Főbb tápértékek
        JLabel titleLabel = new JLabel("Tápérték 100g termékre");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));

        // Az összes tápanyag hozzáadása
        for (String nutrient : NutritionInfo.getAllNutrients()) {
            double value = info.getValue(nutrient);
            // Csak azokat az értékeket jelenítjük meg, amelyek nem nullák
            if (value > 0.001) {
                addResultRow(panel, getNutrientDisplayName(nutrient), value);
            }
        }
    }

    /**
     * Tápanyag megjelenítési nevének megszerzése
     */
    private String getNutrientDisplayName(String nutrient) {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("energy", "Energia (kJ)");
        displayNames.put("energyKcal", "Energia (kcal)");
        displayNames.put("fat", "Zsír (g)");
        displayNames.put("saturatedFat", "- ebből telített zsírsav (g)");
        displayNames.put("monounsaturatedFat", "- ebből egyszeresen telítetlen (g)");
        displayNames.put("polyunsaturatedFat", "- ebből többszörösen telítetlen (g)");
        displayNames.put("transFat", "- ebből transz-zsír (g)");
        displayNames.put("cholesterol", "Koleszterin (mg)");
        displayNames.put("carbs", "Szénhidrát (g)");
        displayNames.put("sugar", "- ebből cukor (g)");
        displayNames.put("starch", "- ebből keményítő (g)");
        displayNames.put("fiber", "Rost (g)");
        displayNames.put("protein", "Fehérje (g)");
        displayNames.put("salt", "Só (g)");
        displayNames.put("sodium", "Nátrium (mg)");
        displayNames.put("vitaminA", "A-vitamin (µg)");
        displayNames.put("vitaminC", "C-vitamin (mg)");
        displayNames.put("vitaminD", "D-vitamin (µg)");
        displayNames.put("vitaminE", "E-vitamin (mg)");
        displayNames.put("vitaminK", "K-vitamin (µg)");
        displayNames.put("vitaminB1", "B1-vitamin (mg)");
        displayNames.put("vitaminB2", "B2-vitamin (mg)");
        displayNames.put("vitaminB3", "B3-vitamin (mg)");
        displayNames.put("vitaminB6", "B6-vitamin (mg)");
        displayNames.put("vitaminB12", "B12-vitamin (µg)");
        displayNames.put("folate", "Folát (µg)");
        displayNames.put("calcium", "Kalcium (mg)");
        displayNames.put("iron", "Vas (mg)");
        displayNames.put("magnesium", "Magnézium (mg)");
        displayNames.put("phosphorus", "Foszfor (mg)");
        displayNames.put("potassium", "Kálium (mg)");
        displayNames.put("zinc", "Cink (mg)");

        return displayNames.getOrDefault(nutrient, nutrient);
    }

    /**
     * Összehasonlító sor hozzáadása két panelhez
     */
    private void addComparisonRow(JPanel leftPanel, JPanel rightPanel, String label,
                                  double leftValue, double rightValue) {
        DecimalFormat df = new DecimalFormat("#.##");

        JPanel leftRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel leftLabel = new JLabel(label + ":");
        leftLabel.setPreferredSize(new Dimension(120, 20));
        JLabel leftValueLabel = new JLabel(df.format(leftValue));
        leftRow.add(leftLabel);
        leftRow.add(leftValueLabel);
        leftPanel.add(leftRow);

        JPanel rightRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel rightLabel = new JLabel(label + ":");
        rightLabel.setPreferredSize(new Dimension(120, 20));
        JLabel rightValueLabel = new JLabel(df.format(rightValue));
        rightRow.add(rightLabel);
        rightRow.add(rightValueLabel);
        rightPanel.add(rightRow);
    }


    private void displayResults(NutritionInfo result) {
        // Töröljük a korábbi eredményeket
        resultPanel.removeAll();

        // Főbb tápértékek
        JLabel titleLabel = new JLabel("Tápérték 100g termékre");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultPanel.add(titleLabel);
        resultPanel.add(Box.createVerticalStrut(15));

        // Energia
        addResultRow(resultPanel, "Energia (kJ)", result.getValue("energy"));
        addResultRow(resultPanel, "Energia (kcal)", result.getValue("energyKcal"));
        resultPanel.add(Box.createVerticalStrut(10));

        // Makrotápanyagok
        addResultRow(resultPanel, "Zsír (g)", result.getValue("fat"));
        addResultRow(resultPanel, "- ebből telített zsírsav (g)", result.getValue("saturatedFat"));
        addResultRow(resultPanel, "- ebből egyszeresen telítetlen (g)", result.getValue("monounsaturatedFat"));
        addResultRow(resultPanel, "- ebből többszörösen telítetlen (g)", result.getValue("polyunsaturatedFat"));
        addResultRow(resultPanel, "- ebből transz-zsír (g)", result.getValue("transFat"));
        addResultRow(resultPanel, "Koleszterin (mg)", result.getValue("cholesterol"));
        resultPanel.add(Box.createVerticalStrut(10));

        addResultRow(resultPanel, "Szénhidrát (g)", result.getValue("carbs"));
        addResultRow(resultPanel, "- ebből cukor (g)", result.getValue("sugar"));
        addResultRow(resultPanel, "- ebből keményítő (g)", result.getValue("starch"));
        addResultRow(resultPanel, "Rost (g)", result.getValue("fiber"));
        resultPanel.add(Box.createVerticalStrut(10));

        addResultRow(resultPanel, "Fehérje (g)", result.getValue("protein"));
        addResultRow(resultPanel, "Só (g)", result.getValue("salt"));
        resultPanel.add(Box.createVerticalStrut(15));

        // Vitaminok és ásványi anyagok
        JLabel vitaminsLabel = new JLabel("Vitaminok és ásványi anyagok");
        vitaminsLabel.setFont(new Font("Arial", Font.BOLD, 13));
        vitaminsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(vitaminsLabel);
        resultPanel.add(Box.createVerticalStrut(10));

        addResultRow(resultPanel, "A-vitamin (µg)", result.getValue("vitaminA"));
        addResultRow(resultPanel, "C-vitamin (mg)", result.getValue("vitaminC"));
        addResultRow(resultPanel, "D-vitamin (µg)", result.getValue("vitaminD"));
        addResultRow(resultPanel, "E-vitamin (mg)", result.getValue("vitaminE"));
        addResultRow(resultPanel, "K-vitamin (µg)", result.getValue("vitaminK"));
        addResultRow(resultPanel, "B1-vitamin (mg)", result.getValue("vitaminB1"));
        addResultRow(resultPanel, "B2-vitamin (mg)", result.getValue("vitaminB2"));
        addResultRow(resultPanel, "B3-vitamin (mg)", result.getValue("vitaminB3"));
        addResultRow(resultPanel, "B6-vitamin (mg)", result.getValue("vitaminB6"));
        addResultRow(resultPanel, "B12-vitamin (µg)", result.getValue("vitaminB12"));
        addResultRow(resultPanel, "Folát (µg)", result.getValue("folate"));
        resultPanel.add(Box.createVerticalStrut(10));

        addResultRow(resultPanel, "Kalcium (mg)", result.getValue("calcium"));
        addResultRow(resultPanel, "Vas (mg)", result.getValue("iron"));
        addResultRow(resultPanel, "Magnézium (mg)", result.getValue("magnesium"));
        addResultRow(resultPanel, "Foszfor (mg)", result.getValue("phosphorus"));
        addResultRow(resultPanel, "Kálium (mg)", result.getValue("potassium"));
        addResultRow(resultPanel, "Nátrium (mg)", result.getValue("sodium"));
        addResultRow(resultPanel, "Cink (mg)", result.getValue("zinc"));

        // Panel frissítése
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    /**
     * Eredmény sor hozzáadása panelhez
     */
    private void addResultRow(JPanel panel, String label, double value) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowPanel.setMaximumSize(new Dimension(250, 25));

        JLabel nameLabel = new JLabel(label + ":");
        nameLabel.setPreferredSize(new Dimension(180, 20));

        JLabel valueLabel = new JLabel(df.format(value));

        rowPanel.add(nameLabel);
        rowPanel.add(valueLabel);

        panel.add(rowPanel);
    }

    private void saveRecipe() {
        // Ellenőrizzük, hogy van-e összetevő
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "A recept nem tartalmaz összetevőket.",
                    "Mentés sikertelen",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Frissítsük a receptet a táblázatból
        updateRecipeFromTable();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Recept mentése");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Ha nem .recipe kiterjesztéssel választotta, hozzáadjuk
            if (!file.getName().toLowerCase().endsWith(".recipe")) {
                file = new File(file.getAbsolutePath() + ".recipe");
            }

            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(currentRecipe);
                JOptionPane.showMessageDialog(
                        this,
                        "A recept sikeresen el lett mentve.",
                        "Mentés sikeres",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Hiba történt a mentés során: " + e.getMessage(),
                        "Mentés sikertelen",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void updateRecipeFromTable() {
        currentRecipe.clearIngredients();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            double weight = (Double) tableModel.getValueAt(i, 1);
            currentRecipe.addIngredient(name, weight);
        }
    }

    private void loadRecipe() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Recept betöltése");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                Recipe loadedRecipe = (Recipe) in.readObject();

                // Ellenőrizzük, hogy minden összetevő megtalálható-e az adatbázisban
                boolean allIngredientsFound = true;
                StringBuilder missingIngredients = new StringBuilder();

                for (Map.Entry<String, Double> entry : loadedRecipe.getIngredients().entrySet()) {
                    if (nutritionDb.getNutritionInfo(entry.getKey()) == null) {
                        allIngredientsFound = false;
                        missingIngredients.append(entry.getKey()).append(", ");
                    }
                }

                if (!allIngredientsFound) {
                    JOptionPane.showMessageDialog(
                            this,
                            "A következő összetevők hiányoznak az adatbázisból: " +
                                    missingIngredients.substring(0, missingIngredients.length() - 2) +
                                    ". Kérlek, add hozzá ezeket az összetevőket az adatbázishoz.",
                            "Hiányzó összetevők",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Recipe betöltése
                currentRecipe = loadedRecipe;
                updateRecipeNameDisplay();
                updateTableFromRecipe();

                JOptionPane.showMessageDialog(
                        this,
                        "A recept sikeresen be lett töltve.",
                        "Betöltés sikeres",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Hiba történt a betöltés során: " + e.getMessage(),
                        "Betöltés sikertelen",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void updateTableFromRecipe() {
        // Táblázat törlése
        tableModel.setRowCount(0);

        // Összetevők hozzáadása a táblázathoz
        for (Map.Entry<String, Double> entry : currentRecipe.getIngredients().entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    private void searchOpenFoodFacts() {
        String query = JOptionPane.showInputDialog(
                this,
                "Adj meg egy kifejezést a kereséshez az OpenFoodFacts adatbázisban:",
                "OpenFoodFacts keresés",
                JOptionPane.QUESTION_MESSAGE);

        if (query != null && !query.trim().isEmpty()) {
            try {
                // Háttérszálon futtatjuk a keresést
                SwingWorker<List<FoodProduct>, Void> worker = new SwingWorker<>() {
                    @Override
                    protected List<FoodProduct> doInBackground() throws Exception {
                        return searchProducts(query);
                    }

                    @Override
                    protected void done() {
                        try {
                            List<FoodProduct> products = get();
                            if (products.isEmpty()) {
                                JOptionPane.showMessageDialog(
                                        NutritionCalculator.this,
                                        "Nem találtunk termékeket a keresésre.",
                                        "Nincs találat",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                showProductSelectionDialog(products);
                            }
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(
                                    NutritionCalculator.this,
                                    "Hiba történt a keresés során: " + e.getMessage(),
                                    "Keresési hiba",
                                    JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    }
                };

                // Várakozó cursor beállítása
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Indítjuk a keresést
                worker.execute();

                // Visszaállítjuk a normál cursort a worker.done() metódusban
                worker.addPropertyChangeListener(evt -> {
                    if ("state".equals(evt.getPropertyName()) &&
                            SwingWorker.StateValue.DONE == evt.getNewValue()) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                });

            } catch (Exception e) {
                setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(
                        this,
                        "Hiba történt a keresés során: " + e.getMessage(),
                        "Keresési hiba",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private List<FoodProduct> searchProducts(String query) throws IOException, ParseException {
        List<FoodProduct> results = new ArrayList<>();

        // URL kódolás
        String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        URL url = new URL("https://world.openfoodfacts.org/cgi/search.pl?search_terms=" +
                encodedQuery + "&json=1&page_size=10");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Válasz olvasása
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // JSON feldolgozása
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
        JSONArray products = (JSONArray) jsonResponse.get("products");

        // Termékek feldolgozása
        for (Object productObj : products) {
            JSONObject product = (JSONObject) productObj;

            // Termék neve és azonosítója
            String productName = product.containsKey("product_name") ?
                    (String) product.get("product_name") : "Ismeretlen termék";

            // Ha üres a név, próbáljuk a másik mezőt
            if (productName == null || productName.trim().isEmpty()) {
                productName = product.containsKey("generic_name") ?
                        (String) product.get("generic_name") : "Ismeretlen termék";
            }

            String productId = (String) product.get("code");

            // Tápérték adatok kinyerése (ha vannak)
            NutritionInfo nutritionInfo = new NutritionInfo();

            // Energia
            if (product.containsKey("nutriments")) {
                JSONObject nutriments = (JSONObject) product.get("nutriments");

                nutritionInfo.setValue("energy", getDoubleValue(nutriments, "energy-kj_100g"));
                nutritionInfo.setValue("energyKcal", getDoubleValue(nutriments, "energy-kcal_100g"));
                nutritionInfo.setValue("fat", getDoubleValue(nutriments, "fat_100g"));
                nutritionInfo.setValue("saturatedFat", getDoubleValue(nutriments, "saturated-fat_100g"));
                nutritionInfo.setValue("carbs", getDoubleValue(nutriments, "carbohydrates_100g"));
                nutritionInfo.setValue("sugar", getDoubleValue(nutriments, "sugars_100g"));
                nutritionInfo.setValue("fiber", getDoubleValue(nutriments, "fiber_100g"));
                nutritionInfo.setValue("protein", getDoubleValue(nutriments, "proteins_100g"));
                nutritionInfo.setValue("salt", getDoubleValue(nutriments, "salt_100g"));
                nutritionInfo.setValue("sodium", getDoubleValue(nutriments, "sodium_100g"));

                // További adatok (ha vannak)
                nutritionInfo.setValue("cholesterol", getDoubleValue(nutriments, "cholesterol_100g"));
                nutritionInfo.setValue("transFat", getDoubleValue(nutriments, "trans-fat_100g"));
                nutritionInfo.setValue("monounsaturatedFat", getDoubleValue(nutriments, "monounsaturated-fat_100g"));
                nutritionInfo.setValue("polyunsaturatedFat", getDoubleValue(nutriments, "polyunsaturated-fat_100g"));

                // Vitaminok és ásványi anyagok
                nutritionInfo.setValue("vitaminA", getDoubleValue(nutriments, "vitamin-a_100g"));
                nutritionInfo.setValue("vitaminC", getDoubleValue(nutriments, "vitamin-c_100g"));
                nutritionInfo.setValue("calcium", getDoubleValue(nutriments, "calcium_100g"));
                nutritionInfo.setValue("iron", getDoubleValue(nutriments, "iron_100g"));
            }

            // Termékobjektum létrehozása és hozzáadása a listához
            FoodProduct foodProduct = new FoodProduct(productId, productName, nutritionInfo);
            results.add(foodProduct);
        }

        return results;
    }

    private double getDoubleValue(JSONObject obj, String key) {
        if (obj.containsKey(key)) {
            Object value = obj.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }
        return 0.0;
    }

    private void showProductSelectionDialog(List<FoodProduct> products) {
        // Terméknevek összegyűjtése
        String[] productNames = new String[products.size()];
        for (int i = 0; i < products.size(); i++) {
            productNames[i] = products.get(i).getName();
        }

        // Dialógus létrehozása a termék kiválasztásához
        String selectedProductName = (String) JOptionPane.showInputDialog(
                this,
                "Válassz egy terméket:",
                "Termék kiválasztása",
                JOptionPane.QUESTION_MESSAGE,
                null,
                productNames,
                productNames[0]);

        if (selectedProductName != null) {
            // Kiválasztott termék keresése
            FoodProduct selectedProduct = null;
            for (FoodProduct product : products) {
                if (product.getName().equals(selectedProductName)) {
                    selectedProduct = product;
                    break;
                }
            }

            if (selectedProduct != null) {
                // Termék adatainak megjelenítése és hozzáadás az adatbázishoz
                // Kérdezzük meg, hogy milyen néven szeretné a termékünket menteni
                String ingredientName = JOptionPane.showInputDialog(
                        this,
                        "Add meg az összetevő nevét az adatbázisban való tároláshoz:",
                        selectedProduct.getName());

                if (ingredientName != null && !ingredientName.trim().isEmpty()) {
                    // Összetevő hozzáadása az adatbázishoz
                    nutritionDb.addIngredient(ingredientName, selectedProduct.getNutritionInfo());

                    JOptionPane.showMessageDialog(
                            this,
                            "Az összetevő sikeresen hozzáadva az adatbázishoz.",
                            "Sikeres hozzáadás",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private void addCustomIngredient() {
        // Dialógus létrehozása
        JDialog dialog = new JDialog(this, "Saját összetevő hozzáadása", true);
        dialog.setSize(600, 700);
        dialog.setLayout(new BorderLayout(10, 10));

        // Panel a mezőkhöz
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // Összetevő név
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel nameLabel = new JLabel("Összetevő neve:");
        nameLabel.setPreferredSize(new Dimension(150, 25));
        JTextField nameField = new JTextField(30);
        namePanel.add(nameLabel);
        namePanel.add(nameField);
        formPanel.add(namePanel);

        // Tápérték mezők
        Map<String, JTextField> nutrientFields = new HashMap<>();

        // Energia
        addNutrientField(formPanel, nutrientFields, "energy", "Energia (kJ):");
        addNutrientField(formPanel, nutrientFields, "energyKcal", "Energia (kcal):");

        formPanel.add(Box.createVerticalStrut(10));
        JLabel macroLabel = new JLabel("Makrotápanyagok:");
        macroLabel.setFont(new Font("Arial", Font.BOLD, 14));
        formPanel.add(macroLabel);

        // Makrotápanyagok
        addNutrientField(formPanel, nutrientFields, "fat", "Zsír (g):");
        addNutrientField(formPanel, nutrientFields, "saturatedFat", "- ebből telített zsírsav (g):");
        addNutrientField(formPanel, nutrientFields, "monounsaturatedFat", "- ebből egyszeresen telítetlen (g):");
        addNutrientField(formPanel, nutrientFields, "polyunsaturatedFat", "- ebből többszörösen telítetlen (g):");
        addNutrientField(formPanel, nutrientFields, "transFat", "- ebből transz-zsír (g):");
        addNutrientField(formPanel, nutrientFields, "cholesterol", "Koleszterin (mg):");

        addNutrientField(formPanel, nutrientFields, "carbs", "Szénhidrát (g):");
        addNutrientField(formPanel, nutrientFields, "sugar", "- ebből cukor (g):");
        addNutrientField(formPanel, nutrientFields, "starch", "- ebből keményítő (g):");
        addNutrientField(formPanel, nutrientFields, "fiber", "Rost (g):");

        addNutrientField(formPanel, nutrientFields, "protein", "Fehérje (g):");
        addNutrientField(formPanel, nutrientFields, "salt", "Só (g):");
        addNutrientField(formPanel, nutrientFields, "sodium", "Nátrium (mg):");

        formPanel.add(Box.createVerticalStrut(10));
        JLabel vitaminsLabel = new JLabel("Vitaminok és ásványi anyagok:");
        vitaminsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        formPanel.add(vitaminsLabel);

        // Vitaminok és ásványi anyagok
        addNutrientField(formPanel, nutrientFields, "vitaminA", "A-vitamin (µg):");
        addNutrientField(formPanel, nutrientFields, "vitaminC", "C-vitamin (mg):");
        addNutrientField(formPanel, nutrientFields, "vitaminD", "D-vitamin (µg):");
        addNutrientField(formPanel, nutrientFields, "vitaminE", "E-vitamin (mg):");
        addNutrientField(formPanel, nutrientFields, "calcium", "Kalcium (mg):");
        addNutrientField(formPanel, nutrientFields, "iron", "Vas (mg):");
        addNutrientField(formPanel, nutrientFields, "magnesium", "Magnézium (mg):");
        addNutrientField(formPanel, nutrientFields, "zinc", "Cink (mg):");
        addNutrientField(formPanel, nutrientFields, "potassium", "Kálium (mg):");

        // Görgetősáv hozzáadása
        JScrollPane scrollPane = new JScrollPane(formPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Gomb panel
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Mentés");
        JButton cancelButton = new JButton("Mégse");

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Gombok eseménykezelése
        saveButton.addActionListener(e -> {
            String ingredientName = nameField.getText().trim();

            if (ingredientName.isEmpty()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Az összetevő neve nem lehet üres.",
                        "Hiányzó adat",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (nutritionDb.getNutritionInfo(ingredientName) != null) {
                int result = JOptionPane.showConfirmDialog(
                        dialog,
                        "Ez az összetevő már létezik az adatbázisban. Szeretnéd felülírni?",
                        "Összetevő már létezik",
                        JOptionPane.YES_NO_OPTION);

                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Tápérték objektum létrehozása
            NutritionInfo info = new NutritionInfo();

            // Értékek beolvasása a mezőkből
            for (Map.Entry<String, JTextField> entry : nutrientFields.entrySet()) {
                String nutrient = entry.getKey();
                JTextField field = entry.getValue();

                try {
                    double value = 0.0;
                    if (!field.getText().trim().isEmpty()) {
                        value = Double.parseDouble(field.getText().trim());
                    }
                    info.setValue(nutrient, value);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Érvénytelen szám a következő mezőben: " + nutrient,
                            "Érvénytelen adat",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Összetevő hozzáadása az adatbázishoz
            nutritionDb.addIngredient(ingredientName, info);

            JOptionPane.showMessageDialog(
                    dialog,
                    "Az összetevő sikeresen hozzáadva az adatbázishoz.",
                    "Sikeres hozzáadás",
                    JOptionPane.INFORMATION_MESSAGE);

            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // Dialógus megjelenítése
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addNutrientField(JPanel panel, Map<String, JTextField> fields, String nutrient, String label) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel nutrientLabel = new JLabel(label);
        nutrientLabel.setPreferredSize(new Dimension(170, 25));
        JTextField nutrientField = new JTextField(15);
        nutrientField.setText("0.0");

        rowPanel.add(nutrientLabel);
        rowPanel.add(nutrientField);

        panel.add(rowPanel);
        fields.put(nutrient, nutrientField);
    }

    public static void main(String[] args) {
        try {
            // Rendszer kinézet beállítása
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            NutritionCalculator calculator = new NutritionCalculator();
            calculator.setVisible(true);
        });
    }
}