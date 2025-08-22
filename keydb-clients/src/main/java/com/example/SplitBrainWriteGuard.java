package com.example;

import redis.clients.jedis.Jedis;

public class SplitBrainWriteGuard {
    public static void main(String[] args) throws InterruptedException {
        final String myHost = "localhost";
        final int myPort = 6379;
        final String peerHost = "localhost";
        final int peerPort = 6380;

        System.out.println("🔐 [Guard] Monitoring for split-brain...");

        while (true) {
            try (Jedis self = new Jedis(myHost, myPort)) {
                boolean peerReachable = false;

                try (Jedis peer = new Jedis(peerHost, peerPort)) {
                    String pong = peer.ping();
                    peerReachable = pong.equalsIgnoreCase("PONG");
                } catch (Exception e) {
                    peerReachable = false;
                }

                if (!peerReachable) {
                    System.out.println("🚫 [Guard] Peer unreachable! Blocking writes on " + myPort);
                    self.configSet("rename-command", "SET \"\"");
                } else {
                    System.out.println("✅ [Guard] Peer reachable. Ensuring SET is enabled.");
                    self.configSet("rename-command", "\"\" SET");
                }
            } catch (Exception e) {
                System.err.println("💥 [Guard] Error: " + e.getMessage());
            }

            Thread.sleep(3000); // Check every 3s
        }
    }
}
