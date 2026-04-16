package com.nhom15.model.auction;
import java.util.*;

public class AuctionManager {
    private static AuctionManager instance;
    // Danh sach phien dau gia
    private List<Auction> autions;
    private AuctionManager(){
        autions= new ArrayList<>();
    }
    public static AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
    // Them mot phien dau gia moi
    public void addAuction(Auction auction){
        autions.add(auction);
    }
    // Lay danh sach dau gia
    public List<Auction> getAuctions(){
        return autions;
    }
}
