package com.nhom15.model.item;

public class Art extends Item {
    private String artist;

    public Art(String id, String name, double startingPrice, String artist) {
        super(id, name, startingPrice);
        this.artist = artist;
    }

    @Override
    public void displayItemInfo() {
        System.out.println("Nghệ thuật: " + getName() + " - Tác giả: " + artist);
    }
}