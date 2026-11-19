package com.tiketin.util;

import com.tiketin.model.Event;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Shared event status classification and 3-tier deterministic sorting helpers.
 * Priority order:
 * 0 = Aktif (not expired and slots > 0)
 * 1 = Habis (not expired and slots <= 0)
 * 2 = Berakhir (expired based on date + time)
 */
public final class EventSortUtil {

    public enum EventStatus {
        AKTIF("Aktif", 0),
        HABIS("Habis", 1),
        BERAKHIR("Berakhir", 2);

        private final String label;
        private final int tier;

        EventStatus(String label, int tier) {
            this.label = label;
            this.tier = tier;
        }

        public String getLabel() {
            return label;
        }

        public int getTier() {
            return tier;
        }
    }

    private EventSortUtil() {}

    public static boolean isExpired(Event event, LocalDateTime referenceNow) {
        return event != null && DateUtil.isEventPassed(event.getDate(), event.getTime(), referenceNow);
    }

    public static boolean isExpired(Event event) {
        return isExpired(event, LocalDateTime.now());
    }

    public static boolean isAvailable(Event event, LocalDateTime referenceNow) {
        return event != null && !isExpired(event, referenceNow) && event.getSlots() > 0;
    }

    public static EventStatus getStatus(Event event, LocalDateTime referenceNow) {
        if (event == null || isExpired(event, referenceNow)) {
            return EventStatus.BERAKHIR;
        }
        if (event.getSlots() <= 0) {
            return EventStatus.HABIS;
        }
        return EventStatus.AKTIF;
    }

    public static EventStatus getStatus(Event event) {
        return getStatus(event, LocalDateTime.now());
    }

    public static int tierForLabel(String statusLabel) {
        if (statusLabel == null) {
            return 3;
        }
        String normalized = statusLabel.trim();
        if ("Aktif".equalsIgnoreCase(normalized)) {
            return EventStatus.AKTIF.getTier();
        }
        if ("Habis".equalsIgnoreCase(normalized)) {
            return EventStatus.HABIS.getTier();
        }
        if ("Berakhir".equalsIgnoreCase(normalized)) {
            return EventStatus.BERAKHIR.getTier();
        }
        return 3;
    }

    public static Comparator<Event> tierComparator(LocalDateTime referenceNow) {
        final LocalDateTime now = referenceNow != null ? referenceNow : LocalDateTime.now();
        return Comparator.nullsLast(
                Comparator.comparingInt(event -> getStatus(event, now).getTier())
        );
    }

    public static Comparator<Event> defaultWithinTierComparator(LocalDateTime referenceNow) {
        final LocalDateTime now = referenceNow != null ? referenceNow : LocalDateTime.now();
        return Comparator.nullsLast((a, b) -> {
            LocalDateTime dtA = DateUtil.parseEventDateTime(a.getDate(), a.getTime());
            LocalDateTime dtB = DateUtil.parseEventDateTime(b.getDate(), b.getTime());
            if (dtA == null && dtB == null) {
                return Integer.compare(b.getId(), a.getId());
            }
            if (dtA == null) {
                return 1;
            }
            if (dtB == null) {
                return -1;
            }
            int cmp = (isExpired(a, now) && isExpired(b, now))
                    ? dtB.compareTo(dtA)
                    : dtA.compareTo(dtB);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(b.getId(), a.getId());
        });
    }

    public static Comparator<Event> defaultComparator(LocalDateTime referenceNow) {
        final LocalDateTime now = referenceNow != null ? referenceNow : LocalDateTime.now();
        return Comparator.nullsLast(
                tierComparator(now).thenComparing(defaultWithinTierComparator(now))
        );
    }

    public static Comparator<Event> homeComparator(String sortOption, LocalDateTime referenceNow) {
        final LocalDateTime now = referenceNow != null ? referenceNow : LocalDateTime.now();
        Comparator<Event> tierComp = tierComparator(now);
        Comparator<Event> withinDefault = defaultWithinTierComparator(now);

        Comparator<Event> combined;
        if ("Harga terendah".equals(sortOption)) {
            combined = tierComp
                    .thenComparingInt(Event::getPrice)
                    .thenComparing(withinDefault);
        } else if ("Harga tertinggi".equals(sortOption)) {
            combined = tierComp
                    .thenComparing((a, b) -> Integer.compare(b.getPrice(), a.getPrice()))
                    .thenComparing(withinDefault);
        } else if ("Kuota terbanyak".equals(sortOption)) {
            combined = tierComp
                    .thenComparing((a, b) -> Integer.compare(b.getSlots(), a.getSlots()))
                    .thenComparing(withinDefault);
        } else {
            combined = tierComp.thenComparing(withinDefault);
        }
        return Comparator.nullsLast(combined);
    }

    public static void sortDefault(List<Event> events) {
        if (events != null && events.size() > 1) {
            events.sort(defaultComparator(LocalDateTime.now()));
        }
    }
}
