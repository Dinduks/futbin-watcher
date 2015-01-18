package com.dindane.futbinwatcher;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;
import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.ConnectException;
import java.text.DecimalFormat;
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

        if (delay != null) {
            while (true) {
                List<Player> prices = watcher.getPrices(platform, players);
                clearConsole();
                printPrices(prices);
                Thread.sleep(delay * 1000);
            }
        } else {
            List<Player> prices = watcher.getPrices(platform, players);
            printPrices(prices);
        }
    }

    private static void printPrices(List<Player> players) {
        ASCIITableHeader[] header = {
                new ASCIITableHeader("Name", ASCIITable.ALIGN_LEFT),
                new ASCIITableHeader("Target price"),
                new ASCIITableHeader("Lowest BIN"),
                new ASCIITableHeader("Difference")
        };

        ASCIITable.getInstance().printTable(header, listToString2DArray(players));
    }

    private static String[][] listToString2DArray(List<Player> players) {
        String[][] data = new String[players.size() + 2][4];

        for (int i = 0; i < players.size(); i++) {
            data[i][0] = players.get(i).getName();
            data[i][1] = formatNumber(players.get(i).getTargetPrice());
            data[i][2] = formatNumber(players.get(i).getLowestBIN());
            data[i][3] = formatNumber(players.get(i).getTargetPrice() - players.get(i).getLowestBIN());
        }

        for (int i = 0; i < 4; i++) data[players.size()][i] = "";

        data[players.size() + 1][0] = "Total";
        data[players.size() + 1][1] = formatNumber(totalTargetPrice(players));
        data[players.size() + 1][2] = formatNumber(totalLowestBIN(players));
        data[players.size() + 1][3] = formatNumber(totalTargetPrice(players) - totalLowestBIN(players));

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

    private static String formatNumber(Long n) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(n);
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

    public final static void clearConsole()
    {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                Runtime.getRuntime().exec("cls");
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (final Exception e) {}
    }
}