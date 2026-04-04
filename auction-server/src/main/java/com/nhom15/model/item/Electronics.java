package com.nhom15.model.item;

public class Electronics extends Item{
    private String brand ;
    public Electronics(String id , String name , double startingPrice , String brand){
        super(id , name , startingPrice);
        this.brand = brand;
    }

    @Override
    public void displayItemInfo() {
        System.out.println("Đồ điện tử: " +getName() + " -Hãng " + brand);
    }
}
