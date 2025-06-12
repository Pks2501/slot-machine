package com.slotmachine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SlotMachine {
	private int rows;
	private int columns;
	private Map<String, Symbol> symbols;
	private List<Map<String, Object>> standardProbabilities;
	private Map<String, Integer> bonusProbabilities;
	private List<Win> winCombinations;

	public static void main(String[] args) throws IOException {
		System.out.println("Slot Machine Game Started!");
		try {
			InputStream configStream;
			double bet = getBettingAmount(args);
			System.out.println("Betting amount: " + bet);
			configStream = SlotMachine.class.getClassLoader().getResourceAsStream("config.json");
			if (configStream == null) {
				System.out.println("Config file not found!");
				return;
			}
			SlotMachine game = new SlotMachine(configStream);
			System.out.println("Config loaded!");
			Map<String, Object> result = game.spin(bet);
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(result));
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private static double getBettingAmount(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--betting-amount") && i + 1 < args.length) {
				try {
					return Double.parseDouble(args[i + 1]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid betting amount!");
				}
			}
		}
		throw new IllegalArgumentException("No betting amount provided!");
	}

	public SlotMachine(InputStream configStream) throws IOException {
		System.out.println("-----> SlotMachine constructor called");
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> config = mapper.readValue(configStream, Map.class);
		this.rows = (Integer) config.get("rows");
		this.columns = (Integer) config.get("columns");
		// Load symbols
		this.symbols = new HashMap<>();
		Map<String, Map<String, Object>> symbolsConfig = (Map<String, Map<String, Object>>) config.get("symbols");
		for (Map.Entry<String, Map<String, Object>> entry : symbolsConfig.entrySet()) {
			String name = entry.getKey();
			Map<String, Object> symbolData = entry.getValue();
			String type = (String) symbolData.get("type");
			if ("standard".equals(type)) {
				double rewardMultiplier = ((Number) symbolData.get("reward_multiplier")).doubleValue();
				symbols.put(name, new StandardSymbol(name, rewardMultiplier));
			} else if ("bonus".equals(type)) {
				Double rewardMultiplier = symbolData.containsKey("reward_multiplier")
						? ((Number) symbolData.get("reward_multiplier")).doubleValue()
						: null;
				Integer extra = symbolData.containsKey("extra") ? ((Number) symbolData.get("extra")).intValue() : null;
				String impact = (String) symbolData.get("impact");
				symbols.put(name, new BonusSymbol(name, rewardMultiplier, extra, impact));
			}
		}

		// Load probabilities
		Map<String, Object> probabilitiesConfig = (Map<String, Object>) config.get("probabilities");
		this.standardProbabilities = (List<Map<String, Object>>) probabilitiesConfig.get("standard_symbols");
		this.bonusProbabilities = (Map<String, Integer>) ((Map<String, Object>) probabilitiesConfig
				.get("bonus_symbols")).get("symbols");

		// Load win combinations
		this.winCombinations = new ArrayList<>();
		Map<String, Map<String, Object>> winConfig = (Map<String, Map<String, Object>>) config.get("win_combinations");
		for (Map.Entry<String, Map<String, Object>> entry : winConfig.entrySet()) {
			String name = entry.getKey();
			Map<String, Object> winData = entry.getValue();
			double rewardMultiplier = ((Number) winData.get("reward_multiplier")).doubleValue();
			String when = (String) winData.get("when");
			String group = (String) winData.get("group");
			Integer count = winData.containsKey("count") ? (Integer) winData.get("count") : null;
			List<List<String>> coveredAreas = winData.containsKey("covered_areas")
					? (List<List<String>>) winData.get("covered_areas")
					: null;
			winCombinations.add(new Win(name, rewardMultiplier, when, group, count, coveredAreas));
		}
	}	

	public Map<String, Object> spin(double bet) {
		System.out.println("-----> Starting spin with bet: " + bet);
		// Generate 3x3 matrix
		Symbol[][] matrix = generateMatrix();
		System.out.println("----> Generated matrix:");

		// Calculate rewards
		double totalReward = 0;
		Map<String, List<String>> appliedWins = new HashMap<>();
		List<BonusSymbol> appliedBonuses = new ArrayList<>();
		//Map<String, Double> maxRewardPerSymbol = new HashMap<>();


		// Check for wins
		Map<String, Integer> symbolCounts = new HashMap<>();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				Symbol symbol = matrix[i][j];
				if (symbol instanceof StandardSymbol) {
					symbolCounts.put(symbol.getName(), symbolCounts.getOrDefault(symbol.getName(), 0) + 1);
				} else if (symbol instanceof BonusSymbol) {
					appliedBonuses.add((BonusSymbol) symbol);
				}
			}
		}
	    System.out.println("----> Symbol counts: " + symbolCounts);

		// Apply same symbol wins
		for (Win win : winCombinations) {
			if ("same_symbols".equalsIgnoreCase(win.getWhen())) {
				for (Map.Entry<String, Integer> entry : symbolCounts.entrySet()) {
					String symbolName = entry.getKey();
					int count = entry.getValue();
					  if (win.getCount() != null && count >= win.getCount()) {
			                double rewardMultiplier = ((StandardSymbol) symbols.get(symbolName)).getRewardMultiplier() * win.getRewardMultiplier();
			                //maxRewardPerSymbol.merge(symbolName, rewardMultiplier, Double::max);
			                totalReward += rewardMultiplier;
			                appliedWins.computeIfAbsent(symbolName, k -> new ArrayList<>()).add(win.getName());
			            }
				}
			}
		}

		// Apply linear wins
		for (Win win : winCombinations) {
			if ("linear_symbols".equalsIgnoreCase(win.getWhen()) && win.getCoveredAreas() != null) {
				for (List<String> area : win.getCoveredAreas()) {
					String firstSymbolName = null;
					boolean match = true;
					for (String pos : area) {
						String[] parts = pos.split(":");
						int row = Integer.parseInt(parts[0]);
						int col = Integer.parseInt(parts[1]);
						Symbol symbol = matrix[row][col];
						if (!(symbol instanceof StandardSymbol)) {
							match = false;
							break;
						}
						String currentName = symbol.getName();
						if (firstSymbolName == null) {
							firstSymbolName = currentName;
						} else if (!firstSymbolName.equals(currentName)) {
							match = false;
							break;
						}
					}
					if (match && firstSymbolName != null) {
			             double rewardMultiplier = ((StandardSymbol) symbols.get(firstSymbolName)).getRewardMultiplier() * win.getRewardMultiplier();
			               // maxRewardPerSymbol.merge(firstSymbolName, rewardMultiplier, Double::max);
			             totalReward += rewardMultiplier;
			                appliedWins.computeIfAbsent(firstSymbolName, k -> new ArrayList<>()).add(win.getName());
					}
				}
			}
		}
		
		//totalReward = 0;
		//for (Map.Entry<String, Double> entry : maxRewardPerSymbol.entrySet()) {
		   // totalReward += bet * entry.getValue();
		//}
		// Apply bonuses
		String lastBonusApplied = null;
		for (BonusSymbol bonus : appliedBonuses) {
			String impact = bonus.getImpact();
			if ("multiply_reward".equals(impact) && totalReward > 0 && bonus.getRewardMultiplier() != null) {
				totalReward *= bonus.getRewardMultiplier();
				lastBonusApplied = bonus.getName();
			} else if ("extra_bonus".equals(impact) && totalReward > 0 && bonus.getExtra() != null) {
				totalReward += bonus.getExtra();
				lastBonusApplied = bonus.getName();
			}
		}

		// Prepare result
		Map<String, Object> result = new HashMap<>();
		List<List<String>> matrixList = new ArrayList<>();
		for (Symbol[] row : matrix) {
			List<String> rowList = Arrays.stream(row).map(Symbol::getName).collect(Collectors.toList());
			matrixList.add(rowList);
		}
		System.out.println("Grid:");
		for (List<String> row : matrixList) {
		    System.out.println(row.stream().collect(Collectors.joining(" | ")));
		}
		result.put("matrix", matrixList);
		result.put("reward", totalReward);
		result.put("applied_winning_combinations", appliedWins);
		result.put("applied_bonus_symbol", lastBonusApplied);
		 // After reward calculation
	    System.out.println("----> Total reward: " + totalReward);
		return result;
	}

	private Symbol[][] generateMatrix() {
		Symbol[][] matrix = new Symbol[rows][columns];
		Random random = new Random();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				// 90% standard, 10% bonus (spinner)
				if (random.nextDouble() < 0.9) {
					matrix[i][j] = getRandomStandardSymbol(i, j, random);
				} else {
					matrix[i][j] = getRandomBonusSymbol(random);
				}
			}
		}
		return matrix;
	}

	private Symbol getRandomStandardSymbol(int row, int col, Random random) {
		// Find probabilities for this slot
		Map<String, Integer> probabilities = null;
		for (Map<String, Object> prob : standardProbabilities) {
			if (((Number) prob.get("row")).intValue() == row && ((Number) prob.get("column")).intValue() == col) {
				probabilities = (Map<String, Integer>) prob.get("symbols");
				break;
			}
		}
		if (probabilities == null) {
			// Default to (0,0) if not found
			probabilities = (Map<String, Integer>) standardProbabilities.get(0).get("symbols");
		}

		// Raffle
		int totalWeight = probabilities.values().stream().mapToInt(Integer::intValue).sum();
		int rand = random.nextInt(totalWeight);
		int sum = 0;
		for (Map.Entry<String, Integer> entry : probabilities.entrySet()) {
			sum += entry.getValue();
			if (rand < sum) {
				return symbols.get(entry.getKey());
			}
		}
		return symbols.get("F"); // Fallback
	}

	private Symbol getRandomBonusSymbol(Random random) {
		int totalWeight = bonusProbabilities.values().stream().mapToInt(Integer::intValue).sum();
		int rand = random.nextInt(totalWeight);
		int sum = 0;
		for (Map.Entry<String, Integer> entry : bonusProbabilities.entrySet()) {
			sum += entry.getValue();
			if (rand < sum) {
				return symbols.get(entry.getKey());
			}
		}
		return symbols.get("MISS"); // Fallback
	}

}
