package com.slotmachine;

public class StandardSymbol extends Symbol {
	private double rewardMultiplier;

	public StandardSymbol(String name, double rewardMultiplier) {
		super(name,"Standard");
		this.rewardMultiplier= rewardMultiplier;
	}
	
	public double getRewardMultiplier() {
		return rewardMultiplier;
	}

}
