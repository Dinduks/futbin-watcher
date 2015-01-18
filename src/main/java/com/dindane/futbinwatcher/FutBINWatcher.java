package com.dindane.futbinwatcher;

import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import us.codecraft.xsoup.Xsoup;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FutBINWatcher {
    private static final String urlPattern = "http://%s.com/player/%s";

    public List<Player> getPrices(String platform, Map<String, Long> players) throws ConnectException, UnsupportedPlatformException {
        List<Player> playersList = new ArrayList<>();

        String website = (platform.equals("pc")) ? "futpc" : "futbin";

        for (Map.Entry<String, Long> idAndPrice : players.entrySet()) {
            String url = String.format(urlPattern, website, idAndPrice.getKey());

            Document doc;

            try {
                doc = Jsoup.connect(url).get();
            } catch (IOException e) {
                throw new ConnectException(String.format("Could not connect to \"%s\".", url));
            }

            String playerName;
            Long lowestBin;
            Long lowestBin2;
            Long lowestBin3;

            if (platform.equals("pc")) {
                playerName = Xsoup.compile("//td/text()").evaluate(doc).list().get(4);
                lowestBin  = parseLong(Xsoup.compile("//div[contains(@class, \"lowestBin\")]//span/text()").evaluate(doc).list().get(0));
                lowestBin2 = parseLong(Xsoup.compile("//td/text()").evaluate(doc).list().get(0));
                lowestBin3 = parseLong(Xsoup.compile("//td/text()").evaluate(doc).list().get(2));
            } else if (platform.equals("xbox")) {
                playerName = Xsoup.compile("//td/text()").evaluate(doc).list().get(8);
                lowestBin  = parseLong(Xsoup.compile("//div[contains(@id, \"xboxlowest\")]/text()").evaluate(doc).list().get(0));
                lowestBin2 = parseLong(Xsoup.compile("//div[contains(@id, \"xboxlowest2\")]/text()").evaluate(doc).list().get(0));
                lowestBin3 = parseLong(Xsoup.compile("//div[contains(@id, \"xboxlowest3\")]/text()").evaluate(doc).list().get(0));
            } else if (platform.equals("ps")) {
                playerName = Xsoup.compile("//td/text()").evaluate(doc).list().get(8);
                lowestBin  = parseLong(Xsoup.compile("//div[contains(@id, \"pslowest\")]/text()").evaluate(doc).list().get(0));
                lowestBin2 = parseLong(Xsoup.compile("//div[contains(@id, \"pslowest2\")]/text()").evaluate(doc).list().get(0));
                lowestBin3 = parseLong(Xsoup.compile("//div[contains(@id, \"pslowest3\")]/text()").evaluate(doc).list().get(0));
            } else {
                throw new UnsupportedPlatformException(String.format("Platform \"%s\" not supported.", platform));
            }

            playersList.add(new Player(playerName, idAndPrice.getKey(), idAndPrice.getValue(),
                    lowestBin, lowestBin2, lowestBin3));
        }

        return playersList;
    }

    private Long parseLong(String s) {
        return Long.parseLong(s.replace(" ", "").replace(",", ""));
    }
}
