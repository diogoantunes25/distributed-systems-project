package pt.tecnico.distledger.gossip;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Collection;
import java.util.Comparator;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;;

public class Timestamp {
    private Map<String, Integer> times;

    public Timestamp() {
        this(new HashMap<>());
    }

    public Timestamp(Map<String, Integer> times) {
        this.times = times;
    }

    // Getters
    public synchronized Map<String, Integer> getTimes() {
        return times;
    }

    public synchronized int getTime(String target) {
        Integer time = times.get(target);
        return time == null ? 0 : time;
    }

    public synchronized Collection<String> getNonNullReplicas() {
        return times.keySet();
    }

    // Setters
    public synchronized Timestamp set(String target, int value) {
        times.put(target, value);
        return this;
    }

    public synchronized void merge(Timestamp t) {
        Map<String, Integer> newTimes = new HashMap<>();

        for (String target: times.keySet()) 
            newTimes.put(target, (int) Math.max(t.getTime(target), this.getTime(target)));
        for (String target: t.getTimes().keySet()) 
            newTimes.put(target, (int) Math.max(t.getTime(target), this.getTime(target)));

        this.times = newTimes;
    }

    // Copy
    public synchronized Timestamp getCopy() {
        Map<String, Integer> copy = new HashMap<>();
        for (String target: times.keySet()) copy.put(target, this.getTime(target));

        return new Timestamp(copy);
    }

    public synchronized void increase(String target) {
        this.set(target, this.getTime(target)+1);
    }

    public synchronized Timestamp increaseAndGetCopy(String target) {
        return this.set(target, this.getTime(target)+1).getCopy();
    }

    // Serialization
    public static Timestamp fromGrpc(DistLedgerCommonDefinitions.Timestamp t) {
        Map<String, Integer> times = t.getTimestampMap();
        return new Timestamp(times);
    }

    public DistLedgerCommonDefinitions.Timestamp toGrpc() {
            DistLedgerCommonDefinitions.Timestamp.Builder builder =
                    DistLedgerCommonDefinitions.Timestamp.newBuilder();

        for (String target: times.keySet()) {
            builder.putTimestamp(target, times.get(target));
        }

        return builder.build();
    }

    // Comparators
    public static boolean lessOrEqual(Timestamp t1, Timestamp t2) {
        for(String target: t1.getNonNullReplicas())
            if (t1.getTime(target) > t2.getTime(target)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timestamp timestamp = (Timestamp) o;

        for (String target: this.getNonNullReplicas())
            if (this.getTime(target) != timestamp.getTime(target)) return false;
        for (String target: timestamp.getNonNullReplicas())
            if (this.getTime(target) != timestamp.getTime(target)) return false;

        return true;
    }

    /*
     * A total order comparator is necessary to unambiguously order timestamps.
     * Note that ordering timestamps might be necessary in order to guarantee
     * that updates received in a gossip are applied in the correct order.
     */
    public static Comparator<Timestamp> getTotalOrderPrevComparator() {
        return (o1, o2) -> {
            Map<String, Integer> t1 = o1.getTimes();
            Map<String, Integer> t2 = o2.getTimes();

            if (o1.equals(o2)) return 0;
            if (lessOrEqual(o1, o2)) return -1;
            if (lessOrEqual(o2, o1)) return 1;

            // Extra comparison to establish total order
            Set<String> targets = new HashSet<>();
            targets.addAll(t1.keySet());
            targets.addAll(t2.keySet());
            targets = targets.stream().sorted().collect(Collectors.toSet());

            for (String target: targets) {
                if (!t1.containsKey(target)) return -1;
                if (!t2.containsKey(target)) return 1;
                if (t1.get(target) > t2.get(target)) return 1;
                if (t2.get(target) > t1.get(target)) return -1;
            }

            return 0;
        };
    }

    @Override
    public String toString() {
        String ans = new String();
        for (Map.Entry<String, Integer> entry : times.entrySet()) {
            ans += entry.getKey() + ":" + entry.getValue().toString();
        }
        return ans;
    }
}