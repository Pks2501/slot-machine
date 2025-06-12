package com.slotmachine;

import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class SlotMachineTest {
	private SlotMachine game;

	@Before
	public void setUp() throws Exception {
		// Load config.json from resources
		InputStream configStream = getClass().getClassLoader().getResourceAsStream("config.json");
		assertNotNull("config.json should exist in src/main/resources", configStream);

	    System.out.println("-----> Loading config.json for new SlotMachine instance...");
		game = new SlotMachine(configStream);
		
		  System.out.println("------> SlotMachine initialized successfully.");
	}

	@Test
	public void testGridIs3x3AndFilled() {
		// Test that the grid is 3x3 (9 slots) and all slots are filled
		Map<String, Object> result = game.spin(100.0);
		List<List<String>> matrix = (List<List<String>>) result.get("matrix");
		assertEquals("Grid should have 3 rows", 3, matrix.size());
		for (List<String> row : matrix) {
			assertEquals("Each row should have 3 columns", 3, row.size());
			for (String symbol : row) {
				assertNotNull("No slot should be empty", symbol);
				assertTrue("Symbol should be valid", isValidSymbol(symbol));
			}
		}
	}

	@Test
	public void testProbabilitiesFavorF() {
		// Test that F appears more often due to higher probability (6/21 ≈ 28.6%)
		int totalSpins = 1000;
		int fCount = 0;
		for (int i = 0; i < totalSpins; i++) {
			Map<String, Object> result = game.spin(100.0);
			List<List<String>> matrix = (List<List<String>>) result.get("matrix");
			for (List<String> row : matrix) {
				for (String symbol : row) {
					if ("F".equals(symbol)) {
						fCount++;
					}
				}
			}
		}
		double fPercentage = (double) fCount / (totalSpins * 9) * 100;
		// F should appear ~28.6% of the time (standard: 90% * 6/21)
		assertTrue("F should appear around 25-32%", fPercentage >= 25.0 && fPercentage <= 32.0);
	}


	@Test
	public void testOutputStructure() {
		// Test that the JSON output has all required fields
		Map<String, Object> result = game.spin(100.0);
		assertTrue("Result should have matrix", result.containsKey("matrix"));
		assertTrue("Result should have reward", result.containsKey("reward"));
		assertTrue("Result should have applied_winning_combinations",
				result.containsKey("applied_winning_combinations"));
		assertTrue("Result should have applied_bonus_symbol", result.containsKey("applied_bonus_symbol"));
		List<List<String>> matrix = (List<List<String>>) result.get("matrix");
		assertEquals("Matrix should be 3x3", 3, matrix.size());
		assertEquals("Matrix rows should have 3 columns", 3, matrix.get(0).size());
	}

	private boolean isValidSymbol(String symbol) {
		// List of valid symbols from config.json
		String[] validSymbols = { "A", "B", "C", "D", "E", "F", "10x", "5x", "+1000", "+500", "MISS" };
		for (String valid : validSymbols) {
			if (valid.equals(symbol)) {
				return true;
			}
		}
		return false;
	}


}
