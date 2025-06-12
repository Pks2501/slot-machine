package com.slotmachine;

public class BonusSymbol extends Symbol {
	private Double rewardMultiplier;
    private Integer extra;
    private String impact;

    public BonusSymbol(String name, Double rewardMultiplier, Integer extra, String impact) {
        super(name, "bonus");
        this.rewardMultiplier = rewardMultiplier;
        this.extra = extra;
        this.impact = impact;
    }

    public Double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public Integer getExtra() {
        return extra;
    }

    public String getImpact() {
        return impact;
    }
}
