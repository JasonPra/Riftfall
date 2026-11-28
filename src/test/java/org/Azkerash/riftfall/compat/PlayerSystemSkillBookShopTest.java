package org.Azkerash.riftfall.compat;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;

import dev.kubas.playersystem.rank.Rank;
import dev.kubas.playersystem.shop.ShopRules;

import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

class PlayerSystemSkillBookShopTest {
    private ShopRules.Candidate candidate(String id, JsonObject data) {
        String rank = data.has("min_rank") ? data.get("min_rank").getAsString() : "";
        return new ShopRules.Candidate(
                id,
                data.has("weight") ? data.get("weight").getAsInt() : 10,
                rank.isEmpty() ? 0 : Rank.parse(rank).orElseThrow().index(),
                data.has("permanent") && data.get("permanent").getAsBoolean());
    }

    @Test
    void booksAppearOccasionallyAlongsideOriginalStockAndRespectRanks() throws Exception {
        var pool = new ArrayList<ShopRules.Candidate>();
        var books = new HashSet<String>();
        var folder = Path.of("src/main/resources/data/riftfall/playersystem/shop");
        try (var files = Files.list(folder)) {
            for (var file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var data = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                String id = "riftfall:" + file.getFileName().toString().replace(".json", "");
                pool.add(candidate(id, data));
                books.add(id);
            }
        }
        try (var jar =
                new JarFile(
                        Path.of(
                                        ShopRules.class
                                                .getProtectionDomain()
                                                .getCodeSource()
                                                .getLocation()
                                                .toURI())
                                .toFile())) {
            for (var entry :
                    jar.stream()
                            .filter(
                                    e ->
                                            e.getName()
                                                            .startsWith(
                                                                    "data/player_system/playersystem/shop/")
                                                    && e.getName().endsWith(".json"))
                            .toList()) {
                try (var stream = jar.getInputStream(entry)) {
                    pool.add(
                            candidate(
                                    entry.getName(),
                                    JsonParser.parseString(
                                                    new String(
                                                            stream.readAllBytes(),
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8))
                                            .getAsJsonObject()));
                }
            }
        }
        assertEquals(12, books.size());
        var byId = new HashMap<String, ShopRules.Candidate>();
        pool.forEach(c -> byId.put(c.id(), c));
        for (String rank : List.of("F-", "SSS")) {
            int index = Rank.parse(rank).orElseThrow().index();
            int withBooks = 0, withoutBooks = 0, ordinaryOffers = 0;
            var seen = new HashSet<String>();
            for (long day = 0; day < 2048; day++) {
                var offers = ShopRules.draw(pool, index, 4, day);
                boolean book = false;
                for (String id : offers) {
                    assertTrue(byId.get(id).minRank() <= index);
                    if (books.contains(id)) {
                        book = true;
                        seen.add(id);
                    } else {
                        ordinaryOffers++;
                    }
                }
                if (book) {
                    withBooks++;
                } else {
                    withoutBooks++;
                }
            }
            assertTrue(
                    withBooks > 0 && withoutBooks > withBooks,
                    rank + ": books should be occasional");
            assertTrue(ordinaryOffers > 0, rank + ": original stock remains available");
            var eligible =
                    books.stream()
                            .filter(id -> byId.get(id).minRank() <= index)
                            .collect(java.util.stream.Collectors.toSet());
            assertEquals(eligible, seen, rank + ": every eligible book can appear");
        }
    }
}
