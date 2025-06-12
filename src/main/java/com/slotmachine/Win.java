package com.slotmachine;

import java.util.List;

public class Win {
	private String name;
    private double rewardMultiplier;
    private String when;
    private Integer count;
    private String group;
    private List<List<String>> coveredAreas;
    
    public Win(String name, double rewardMultiplier, String when, String group, Integer count, List<List<String>> coveredAreas) {
        this.name = name;
        this.rewardMultiplier = rewardMultiplier;
        this.when = when;
        this.group = group;
        this.count = count;
        this.coveredAreas = coveredAreas;
    }

    public String getName() {
        return name;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public String getWhen() {
        return when;
    }

    public String getGroup() {
        return group;
    }

    public Integer getCount() {
        return count;
    }

    public List<List<String>> getCoveredAreas() {
        return coveredAreas;
    }

}
