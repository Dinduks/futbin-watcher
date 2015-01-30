package com.dindane.futbinwatcher;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;
import com.dindane.futbinwatcher.exceptions.Action;
import com.dindane.futbinwatcher.exceptions.IdParsingException;
import com.dindane.futbinwatcher.exceptions.ParsedLine;
import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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
    @Option(name = "--platform",
            required = true, usage = "The market's platform.")
    private Platform platform;

    @Option(name = "--players-list",
            required = true, usage = "The monitored players' list file.")
    private String playersListFileName;

    @Option(name = "--refresh-delay",
            required = false, usage = "Refresh delay. Must be higher than 2.")
    private Integer refreshDelay = 2 * 60;

    @Option(name = "--help", aliases = {"-h"}, help = true)
    private Boolean showHelp = false;

    @Option(name = "--lowest-bin-2", required = false, usage = "Display the second lowest BIN column.")
    private Boolean lowestBin2 = false;

    @Option(name = "--lowest-bin-3", required = false, usage = "Display the third lowest BIN column.")
    private Boolean lowestBin3 = false;

    private Integer headerSize = 5;

    private static String COLOR_RED = null;
    private static String COLOR_GREEN = null;
    private static String COLOR_RESET = null;

    public static void main(String[] args) throws IOException, InterruptedException, UnsupportedPlatformException, IdParsingException {
        new CLI().doMain(args);
    }

    private void doMain(String[] args) throws IOException, InterruptedException, UnsupportedPlatformException, IdParsingException {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            if (showHelp) {
                parser.printUsage(System.err);
                return;
            }

            if (refreshDelay < 2) {
                System.out.println("The refresh delay cannot be lower than 2 minutes in order to not flood " +
                        "FutBIN's servers. Refresh delay forced to 2 minutes.");
                refreshDelay = 2;
            }
            refreshDelay = refreshDelay * 60;

            if (lowestBin2) headerSize++;
            if (lowestBin3) headerSize++;
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            return;
        }

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

            Thread.sleep(refreshDelay * 1000);
        }
    }

    private void checkForUpdates() {
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
    private String cleanFUTId(String id) throws IdParsingException {
        Pattern p = Pattern.compile("\\d+.*$");
        Matcher m = p.matcher(id);

        if (m.find()) {
            return m.group(0);
        } else {
            throw new IdParsingException(String.format("Could not extract the player's id from \"%s\".", id));
        }
    }

    private String colorize(String s) {
        if (s.contains("-")) {
            return COLOR_RED + s + COLOR_RESET;
        } else {
            return COLOR_GREEN + s + COLOR_RESET;
        }
    }

    private String formatNumber(Number n) {
        return new DecimalFormat("#,###").format(n);
    }

    private void initColors() {
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

    private String[][] listToString2DArray(List<Player> players, Action action) {
        String[][] data = new String[players.size() + 2][headerSize];
        Double mult = (action.equals(Action.BUY)) ? -1 : 0.95;

        for (Integer i = 0; i < players.size(); i++) {
            Integer j = 0;
            data[i][j++] = (action.equals(Action.BUY)) ? "B" : "S";
            data[i][j++] = players.get(i).getName();
            data[i][j++] = formatNumber(players.get(i).getTargetPrice());
            data[i][j++] = formatNumber(players.get(i).getLowestBIN());
            if (lowestBin2) data[i][j++] = formatNumber(players.get(i).getLowestBIN2());
            if (lowestBin3) data[i][j++] = formatNumber(players.get(i).getLowestBIN3());
            data[i][j] = colorize(formatNumber(mult * (players.get(i).getTargetPrice() - players.get(i).getLowestBIN())));
        }

        for (Integer i = 0; i < headerSize; i++) data[players.size()][i] = "";

        Integer j = 0;
        data[players.size() + 1][j++] = "";
        data[players.size() + 1][j++] = "  Total";
        data[players.size() + 1][j++] = formatNumber(totalTargetPrice(players));
        data[players.size() + 1][j++] = formatNumber(totalLowestBIN(players));
        if (lowestBin2) data[players.size() + 1][j++] = formatNumber(totalLowestBIN2(players));
        if (lowestBin3) data[players.size() + 1][j++] = formatNumber(totalLowestBIN3(players));
        data[players.size() + 1][j] = colorize(formatNumber(mult * (totalTargetPrice(players) - totalLowestBIN(players))));

        return data;
    }

    private List<ParsedLine> readPlayersList() throws IdParsingException {
        List<ParsedLine> players = new ArrayList<>();

        File file = new File(playersListFileName);
        try {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String line : lines) {
                if (line.length() == 0) continue;
                if (line.startsWith("#")) continue;
                players.add(parseLine(line));
            }
        } catch (Exception e) {
            System.err.println("Error while reading the players list.");
            e.printStackTrace();
            System.exit(-1);
        }

        return players;
    }

    private ParsedLine parseLine(String line) throws IdParsingException {
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

    private void printPrices(List<Player> players, Action action) {
        Integer i = 0;
        ASCIITableHeader[] header = new ASCIITableHeader[headerSize];
        header[i++] = new ASCIITableHeader(" ");
        header[i++] = new ASCIITableHeader("Name", ASCIITable.ALIGN_LEFT);
        header[i++] = new ASCIITableHeader("Target price");
        header[i++] = new ASCIITableHeader("Lowest BIN");
        if (lowestBin2) header[i++] = new ASCIITableHeader("Lowest BIN 2");
        if (lowestBin3) header[i++] = new ASCIITableHeader("Lowest BIN 3");
        header[i] = new ASCIITableHeader("Difference - 5%");

        String table = ASCIITable.getInstance().getTable(header, listToString2DArray(players, action));
        table = table.replace("\u001B[31", "         \u001B[31").replace("\u001B[32", "         \u001B[32");
        System.out.println(table);
    }

    private Long totalLowestBIN(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getLowestBIN();

        return total;
    }

    private Long totalLowestBIN2(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getLowestBIN2();

        return total;
    }

    private Long totalLowestBIN3(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getLowestBIN3();

        return total;
    }

    private Long totalTargetPrice(List<Player> players) {
        Long total = 0L;
        for (Player player : players) total += player.getTargetPrice();

        return total;
    }
}