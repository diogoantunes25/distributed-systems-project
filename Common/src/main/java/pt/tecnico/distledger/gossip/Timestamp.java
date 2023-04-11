package pt.tecnico.distledger.gossip;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public synchronized List<String> getNonNullReplicas() {
        return (List<String>) times.keySet();
    }

    public static boolean lessOrEqual(Timestamp t1, Timestamp t2) {
        for(String qual: t1.getNonNullReplicas())
            if (t1.getTime(qual) > t2.getTime(qual)) return false;

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

    public synchronized Timestamp increaseAndGetCopy(String qual) {
        times.put(qual, times.get(qual)+1);
        return getCopy();
    }

    public synchronized Timestamp getCopy() {
        Map<String, Integer> copy = new HashMap<>();
        for (String q: times.keySet()) copy.put(q, times.get(q));

        return new Timestamp(copy);
    }

    public static Timestamp fromGrpc(pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Timestamp t) {
        Map<String, Integer> times = t.getTimestamp();
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
}