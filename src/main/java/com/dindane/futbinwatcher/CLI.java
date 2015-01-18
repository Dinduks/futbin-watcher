package com.dindane.futbinwatcher;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;
import com.dindane.futbinwatcher.exceptions.Action;
import com.dindane.futbinwatcher.exceptions.IdParsingException;
import com.dindane.futbinwatcher.exceptions.ParsedLine;
import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CLI {
    private static String COLOR_RED = null;
    private static String COLOR_GREEN = null;
    private static String COLOR_RESET = null;

    public static void main(String[] args) throws IOException, InterruptedException, UnsupportedPlatformException, IdParsingException {
        Object[] parsedArguments = parseArguments(args);
        Platform platform = (Platform) parsedArguments[0];
        Integer delay = (Integer) parsedArguments[1];

        initColors();
        checkForUpdates();

        List<ParsedLine> players = readPlayersList();
        FutBINWatcher watcher = new FutBINWatcher();

        while (true) {
            List<Player> prices = watcher.getPrices(platform, players);
            List<Player> buyPrices = new ArrayList<>();
            List<Player> sellPrices = new ArrayList<>();

            for (Player player : prices) {
                if (player.getAction().equals(Action.SELL)) sellPrices.add(player);
                else buyPrices.add(player);
            }

            if (buyPrices.size() > 0) printPrices(buyPrices, Action.BUY);
            if (sellPrices.size() > 0) printPrices(sellPrices, Action.SELL);

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
                String message = COLOR_RED +
                        "FUTBIN Watcher is not up to date. Please visit " +
                        "https://dinduks.github.com/futbin-watcher/ in order to download the last version." +
                        COLOR_RESET + "\n";
                System.out.println(message);
            }
        } catch (IOException e) {
            String message = COLOR_RED +
                    "An error happened while checking for new versions which " +
                    "might mean there is a new one. Please visit " +
                    "https://dinduks.github.com/futbin-watcher/ and check by yourself.\n" +
                    "Current version: " + localVersion + "." +
                    COLOR_RESET + "\n";
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
            return COLOR_RED + s + COLOR_RESET;
        } else {
            return COLOR_GREEN + s + COLOR_RESET;
        }
    }

    private static String formatNumber(Long n) {
        return new DecimalFormat("#,###").format(n);
    }

    private static void initColors() {
        if (System.getProperty("os.name").contains("Windows")) {
            COLOR_RED = "";
            COLOR_GREEN = "";
            COLOR_RESET = "";
        } else {
            COLOR_RED = "\u001B[31m";
            COLOR_GREEN = "\u001B[32m";
            COLOR_RESET = "\u001B[0m";
        }
    }

    private static String[][] listToString2DArray(List<Player> players, Action action) {
        String[][] data = new String[players.size() + 2][5];
        Integer mult = (action.equals(Action.BUY)) ? 1 : -1;

        for (int i = 0; i < players.size(); i++) {
            data[i][0] = (action.equals(Action.BUY)) ? "B" : "S";
            data[i][1] = players.get(i).getName();
            data[i][2] = formatNumber(players.get(i).getTargetPrice());
            data[i][3] = formatNumber(players.get(i).getLowestBIN());
            data[i][4] = colorize(formatNumber(mult * (players.get(i).getTargetPrice() - players.get(i).getLowestBIN())));
        }

        for (int i = 0; i < 5; i++) data[players.size()][i] = "";

        data[players.size() + 1][0] = "";
        data[players.size() + 1][1] = "  Total";
        data[players.size() + 1][2] = formatNumber(totalTargetPrice(players));
        data[players.size() + 1][3] = formatNumber(totalLowestBIN(players));
        data[players.size() + 1][4] = colorize(formatNumber(mult * (totalTargetPrice(players) - totalLowestBIN(players))));

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

    private static List<ParsedLine> readPlayersList() throws IdParsingException {
        List<ParsedLine> players = new ArrayList<>();

        File file = new File("players_list.txt");
        try {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String line : lines) {
                if (line.length() == 0) continue;
                players.add(parseLine(line));
            }
        } catch (Exception e) {
            System.err.println("Error while reading the players list.");
            e.printStackTrace();
            System.exit(-1);
        }

        return players;
    }

    private static ParsedLine parseLine(String line) throws IdParsingException {
        try {
            String[] parts = line.split(" +");
            if (parts.length == 2) {
                System.out.println("\"link price\" notation is deprecated. Use \"buy link for price\" or \"sell link for price\".");
                System.out.println("Check the manual for more information: http://dinduks.github.io/futbin-watcher/");

                return new ParsedLine(cleanFUTId(parts[0]), Action.BUY, Long.parseLong(parts[1]));
            } else if (parts.length == 3) {
                try {
                    Action action = Action.valueOf(parts[0].toUpperCase());
                    return new ParsedLine(cleanFUTId(parts[1]), action, Long.parseLong(parts[2]));
                } catch (Exception e) {
                    System.err.println("Error while reading the players list.");
                    System.out.println("The action parameter in \"" + line + "\" is not valid.");
                    System.out.println("It should be either \"buy\" or \"sell\".");
                    System.exit(-1);
                    return null;
                }
            } else if (parts.length == 4) {
                try {
                    Action action = Action.valueOf(parts[0].toUpperCase());
                    return new ParsedLine(cleanFUTId(parts[1]), action, Long.parseLong(parts[3]));
                } catch (Exception e) {
                    System.err.println("Error while reading the players list.");
                    System.out.println("The action parameter in \"" + line + "\" is not valid.");
                    System.out.println("It should be either \"buy\" or \"sell\".");
                    System.exit(-1);
                    return null;
                }
            } else {
                System.err.println("Error while reading line \"" + line + "\".");
                System.exit(-1);
                return null;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error while reading the players list.");
            System.out.println("The price parameter in \"" + line + "\" is not a number.");
            System.exit(-1);
            return null;
        }
    }

    private static void printPrices(List<Player> players, Action action) {
        ASCIITableHeader[] header = {
                new ASCIITableHeader(" "),
                new ASCIITableHeader("Name", ASCIITable.ALIGN_LEFT),
                new ASCIITableHeader("Target price"),
                new ASCIITableHeader("Lowest BIN"),
                new ASCIITableHeader("Difference")
        };

        String table = ASCIITable.getInstance().getTable(header, listToString2DArray(players, action));
        table = table.replace("\u001B[31", "         \u001B[31").replace("\u001B[32", "         \u001B[32");
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