package com.nhom15.model.auction;

import java.util.ArrayList;
import java.util.List;

public class AuctionRegistry {

  private static AuctionRegistry instance;
  // Danh sach phien dau gia
  private final List<Auction> autions;

  private AuctionRegistry() {
    autions = new ArrayList<>();
  }

  public static AuctionRegistry getInstance() {
    if (instance == null) {
      instance = new AuctionRegistry();
    }
    return instance;
  }

  // Them mot phien dau gia moi
  public void addAuction(Auction auction) {
    autions.add(auction);
  }

  // Lay danh sach dau gia
  public List<Auction> getAuctions() {
    return autions;
  }
}
