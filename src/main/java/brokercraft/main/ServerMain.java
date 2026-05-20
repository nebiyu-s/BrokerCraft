package brokercraft.main;

import brokercraft.rmi.RMIServer;

public class ServerMain {
    public static void main(String[] args) {
        try {
            RMIServer.start();
            System.out.println("BrokerCraft server is running. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Server failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
