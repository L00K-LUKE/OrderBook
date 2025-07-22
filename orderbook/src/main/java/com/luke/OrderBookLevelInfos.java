package com.luke;

import java.util.ArrayList;

public class OrderBookLevelInfos {
    
    private ArrayList<LevelInfo> bids;
    private ArrayList<LevelInfo> asks;

    public OrderBookLevelInfos(ArrayList<LevelInfo> bids, ArrayList<LevelInfo> asks) {
        this.bids = bids;
        this.asks = asks;
    }

    public ArrayList<LevelInfo> getBids() {
        return bids;
    }

    public ArrayList<LevelInfo> getAsks() {
        return asks;
    }

    
}
