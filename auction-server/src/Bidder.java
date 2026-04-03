public class Bidder extends User{
    // Constructor
    public Bidder(int id, String username, String password, String email) {
        super(id, username, password, email, UserRole.BIDDER);
    }

    @Override
    public void printInfo() {
        System.out.println("Bidder: " + username + " | Email: " + email);
    }

    // Chức năng của Bidder
    public void placeBid(Auction auction, double amount) {
        auction.addBid(this, amount); // thêm lượt đấu giá
    }

    public void viewBidHistory() {
        System.out.println("Lịch sử đấu giá của " + username);
        // logic hiển thị danh sách các bid đã đặt
    }
}

}
