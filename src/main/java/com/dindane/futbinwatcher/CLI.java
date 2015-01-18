package com.dindane.futbinwatcher;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;
import com.dindane.futbinwatcher.exceptions.IdParsingException;
import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CLI {
    public static void main(String[] args) throws IOException, InterruptedException, UnsupportedPlatformException, IdParsingException {
        Object[] parsedArguments = parseArguments(args);
        Platform platform = (Platform) parsedArguments[0];
        Integer delay = (Integer) parsedArguments[1];

        checkForUpdates();

        Map<String, Long> players = parsePlayersList();
        FutBINWatcher watcher = new FutBINWatcher();

        while (true) {
            List<Player> prices = watcher.getPrices(platform, players);
            printPrices(prices);
            Thread.sleep(delay * 1000);
        }
    }

    private static void checkForUpdates() {
        String localVersion = null;
        try {
            localVersion = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("com/dindane/futbinwatcher/version"), "UTF-8");
        } catch (IOException e) {
            return;
        }

        try {
            URL versionURL = new URL("https://raw.githubusercontent.com/Dinduks/futbin-watcher/master/src/main/resources/com/dindane/futbinwatcher/version");
            InputStream in = versionURL.openStream();
            String lastVersion = IOUtils.toString(in);

            if (!lastVersion.equals(localVersion)) {
                String message = "\u001B[31m" +
                        "FUTBIN Watcher is not up to date. Please visit " +
                        "https://dinduks.github.com/futbin-watcher/ in order to download the last version." +
                        "\u001B[0m\n";
                System.out.println(message);
            }
        } catch (IOException e) {
            String message = "\u001B[31m" +
                    "An error happened while checking for new versions which " +
                    "might mean there is a new one. Please visit " +
                    "https://dinduks.github.com/futbin-watcher/ and check by yourself.\n" +
                    "Current version: " + localVersion + "." +
                    "\u001B[0m\n";
            System.out.println(message);
        }
    }

    /**
     * Extracts the player's ID from a string
     */
    private static String cleanFUTId(String id) throws IdParsingException {
        Pattern p = Pattern.compile("\\d+.*$");
        Matcher m = p.matcher(id);

        if (m.find()) {
            return m.group(0);
        } else {
            throw new IdParsingException(String.format("Could not extract the player's id from \"%s\".", id));
        }
    }

    private static String colorize(String s) {
        if (s.contains("-")) {
            return "\u001B[31m" + s + "\u001B[0m";
        } else {
            return "\u001B[32m" + s + "\u001B[0m";
        }
    }

    private static String formatNumber(Long n) {
        return new DecimalFormat("#,###").format(n);
    }

    private static String[][] listToString2DArray(List<Player> players) {
        String[][] data = new String[players.size() + 2][4];

        for (int i = 0; i < players.size(); i++) {
            data[i][0] = players.get(i).getName();
            data[i][1] = formatNumber(players.get(i).getTargetPrice());
            data[i][2] = formatNumber(players.get(i).getLowestBIN());
            data[i][3] = colorize(formatNumber(players.get(i).getTargetPrice() - players.get(i).getLowestBIN()));

        }

        for (int i = 0; i < 4; i++) data[players.size()][i] = "";

        data[players.size() + 1][0] = "Total";
        data[players.size() + 1][1] = formatNumber(totalTargetPrice(players));
        data[players.size() + 1][2] = formatNumber(totalLowestBIN(players));
        data[players.size() + 1][3] = colorize(formatNumber(totalTargetPrice(players) - totalLowestBIN(players)));

        return data;
    }

    private static Object[] parseArguments(String[] args) throws UnsupportedPlatformException {
        Object[] result = new Object[2];

        if (args.length < 1) {
            System.err.println("No enough arguments. Please specify the platform, and optionally the refresh rate.");
            System.exit(-1);
        }

        try {
            result[0] = Platform.valueOf(args[0].toUpperCase());
        } catch (Exception e) {
            throw new UnsupportedPlatformException(String.format("Platform \"%s\" not supported.", args[0]));
        }

        result[1] = 120;
        if (args.length >= 2) {
            try {
                result[1] = 60 * Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("The specified refresh delay is not a number.");
                System.exit(-1);
            }

            if ((Integer) result[1] < 120) {
                result[1] = 120;
                System.out.println("The refresh delay cannot be lower than 120 seconds in order to not flood " +
                        "FutBIN's servers. Refresh delay forced to 2 minutes.");
            }
        }

        return result;
    }

    private static Map<String, Long> parsePlayersList() throws IdParsingException {
        Map<String, Long> players = new HashMap<>();

        File file = new File("players_list");
        try {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String line : lines) {
                if (line.length() == 0) continue;
                String link = cleanFUTId(line.split(" ")[0]);
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

    private static void printPrices(List<Player> players) {
        ASCIITableHeader[] header = {
                new ASCIITableHeader("Name", ASCIITable.ALIGN_LEFT),
                new ASCIITableHeader("Target price"),
                new ASCIITableHeader("Lowest BIN"),
                new ASCIITableHeader("Difference")
        };

        String table = ASCIITable.getInstance().getTable(header, listToString2DArray(players));
        table = table.replace("\u001B[3", "         \u001B[3");
        System.out.println(table);
    }

    private static Long totalLowestBIN(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getLowestBIN();

        return total;
    }

    private static Long totalTargetPrice(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getTargetPrice();

        return total;
    }
}