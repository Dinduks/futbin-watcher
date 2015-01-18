package com.dindane.futbinwatcher;

import com.bethecoder.ascii_table.ASCIITable;
import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CLI {
    private final static String[] platforms = {"pc", "xbox", "ps"};

    public static void main(String[] args) throws ConnectException, InterruptedException, UnsupportedPlatformException {
        if (args.length < 1) {
            System.err.println("No enough arguments. Please specify the platform, and optionally the refresh rate.");
            System.exit(-1);
        }

        String platform = args[0];
        if (!Arrays.asList(platforms).contains(platform)) {
            System.err.println(String.format("Platform \"%s\" not supported.", platform));
            System.exit(-1);
        }

        Integer delay = null;
        if (args.length >= 2) {
            try {
                delay = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("The delay is not a number.");
                System.exit(-1);
            }

            if (delay < 120) {
                delay = 120;
                System.out.println("The delay cannot be lower than 120 seconds in order to not flood " +
                        "FutBIN's servers. It was forced to 120 seconds.");
            }
        }

        Map<String, Long> players = parsePlayersList();

        FutBINWatcher watcher = new FutBINWatcher();
        List<Player> prices = prices = watcher.getPrices(platform, players);

        if (delay != null) {
            while (true) {
                printPrices(prices);
                Thread.sleep(delay * 1000);
            }
        } else {
            printPrices(prices);
        }
    }

    private static void printPrices(List<Player> players) {
        String[] header = {"Name", "Target price", "Lowest BIN", "Difference"};
        ASCIITable.getInstance().printTable(header, listToString2DArray(players));
    }

    private static String[][] listToString2DArray(List<Player> players) {
        String[][] data = new String[players.size() + 2][4];

        for (int i = 0; i < players.size(); i++) {
            data[i][0] = players.get(i).getName();
            data[i][1] = players.get(i).getTargetPrice().toString();
            data[i][2] = players.get(i).getLowestBIN().toString();
            data[i][3] = Long.toString(players.get(i).getTargetPrice() - players.get(i).getLowestBIN());
        }

        for (int i = 0; i < 4; i++) data[players.size()][i] = "";

        data[players.size() + 1][0] = "Total";
        data[players.size() + 1][1] = Long.toString(totalTargetPrice(players));
        data[players.size() + 1][2] = Long.toString(totalLowestBIN(players));
        data[players.size() + 1][3] = Long.toString(totalTargetPrice(players) - totalLowestBIN(players));

        return data;
    }

    private static Long totalTargetPrice(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getTargetPrice();
        return total;
    }

    private static Long totalLowestBIN(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getLowestBIN();
        return total;
    }

    private static Map<String, Long> parsePlayersList() {
        Map<String, Long> players = new HashMap<>();

        File file = new File("players_list");
        try {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String line : lines) {
                String link = line.split(" ")[0];
                Long price = Long.parseLong(line.split(" ")[1]);
                players.put(link, price);
            }
        } catch (Exception e) {
            System.err.println("Error while reading the players list.");
            e.printStackTrace();
            System.exit(-1);
        }

        return players;
    }
}