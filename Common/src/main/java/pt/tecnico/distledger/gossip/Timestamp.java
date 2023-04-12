package pt.tecnico.distledger.gossip;

import java.util.*;

public class Timestamp {
    private Map<String, Integer> times;

    public Timestamp() {
        this(new HashMap<>());
    }

    public Timestamp(Map<String, Integer> times) {
        this.times = times;
    }

    public synchronized void updateTime(String qual, int time) {
        times.put(qual, time);
    }

    public synchronized Map<String, Integer> getTimes() {
        return times;
    }

    public synchronized int getTime(String qual) {
        Integer time = times.get(qual);
        return time == null ? 0 : time;
    }

    public synchronized Collection<String> getNonNullReplicas() {
        return times.keySet();
    }

    public static boolean lessOrEqual(Timestamp t1, Timestamp t2) {
        for(String target: t1.getNonNullReplicas())
            if (t1.getTime(target) > t2.getTime(target)) return false;

        return true;
    }

    public synchronized Timestamp set(String target, int value) {
        times.put(target, value);
        return this;
    }

    public synchronized void merge(Timestamp t) {
        Map<String, Integer> newTimes = new HashMap<>();

        for (String qual: times.keySet()) newTimes.put(qual, (int) Math.max(t.getTime(qual), this.getTime(qual)));
        for (String qual: t.getTimes().keySet()) newTimes.put(qual, (int) Math.max(t.getTime(qual), this.getTime(qual)));

        this.times = newTimes;
    }

    public synchronized Timestamp increaseAndGetCopy(String target) {
        times.putIfAbsent(target, 0);
        times.put(target, times.get(target)+1);
        return getCopy();
    }

    public synchronized Timestamp getCopy() {
        Map<String, Integer> copy = new HashMap<>();
        for (String q: times.keySet()) copy.put(q, times.get(q));

        return new Timestamp(copy);
    }

    public static Timestamp fromGrpc(pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Timestamp t) {
        Map<String, Integer> times = t.getTimestampMap();
        return new Timestamp(times);
    }

    public pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Timestamp toGrpc() {
        pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Timestamp.Builder builder =
                pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Timestamp.newBuilder();

        for (String target: times.keySet()) {
            builder.putTimestamp(target, times.get(target));
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timestamp timestamp = (Timestamp) o;

        for (String target: times.keySet()) {
            Integer t = timestamp.getTimes().get(target);
            if (t == null) return false;
            if (t != times.get(target)) return false;
        }

        for (String target: timestamp.getNonNullReplicas()) {
            Integer t = times.get(target);
            if (t == null) return false;
            if (t != timestamp.getTime(target)) return false;
        }

        return true;
    }
}