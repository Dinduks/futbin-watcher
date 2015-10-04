package com.dindane.futbinwatcher;

import com.dindane.futbinwatcher.exceptions.ParsedLine;
import com.dindane.futbinwatcher.exceptions.UnsupportedPlatformException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import us.codecraft.xsoup.Xsoup;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dindane.futbinwatcher.Platform.*;

public class FutBINWatcher {
    private static final String urlPattern = "http://www.%s.com/16/player/%s";

    public List<Player> getPrices(Platform platform, List<ParsedLine> players) throws ConnectException, UnsupportedPlatformException {
        List<Player> playersList = new ArrayList<>();

        String website = (platform == PC) ? "futpc" : "futbin";

        for (ParsedLine line : players) {
            String url = String.format(urlPattern, website, line.getPlayerId());

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

            switch(platform) {
                case PC:
                    playerName = Xsoup.compile("//td/text()").evaluate(doc).list().get(4);
                    lowestBin  = parseLong(Xsoup.compile("//div[contains(@class, \"lowestBin\")]//span/text()").evaluate(doc).list().get(0));
                    lowestBin2 = parseLong(Xsoup.compile("//td/text()").evaluate(doc).list().get(0));
                    lowestBin3 = parseLong(Xsoup.compile("//td/text()").evaluate(doc).list().get(2));
                    break;
                case XBOX:
                    playerName = Xsoup.compile("//table[contains(@id, \"info\")]//tbody//tr").evaluate(doc).getElements().get(0).child(0).childNode(0).toString();
                    lowestBin  = parseLong(Xsoup.compile("//span[contains(@id, \"xboxlbin\")]/text()").evaluate(doc).list().get(0));
                    lowestBin2 = -1L;
                    lowestBin3 = -1L;
                    break;
                case PS:
                    playerName = Xsoup.compile("//table[contains(@id, \"info\")]//tbody//tr").evaluate(doc).getElements().get(0).child(0).childNode(0).toString();
                    lowestBin  = parseLong(Xsoup.compile("//span[contains(@id, \"pslbin\")]/text()").evaluate(doc).list().get(0));
                    lowestBin2 = -1L;
                    lowestBin3 = -1L;
                    break;
                default:
                    throw new UnsupportedPlatformException("");
            }

            playersList.add(new Player(playerName, url, line.getTargetPrice(),
                    lowestBin, lowestBin2, lowestBin3, line.getAction()));
        }

        return playersList;
    }

    private Long parseLong(String s) {
        return Long.parseLong(s.replace(" ", "").replace(",", ""));
    }
}
