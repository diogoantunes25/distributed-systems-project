package pt.tecnico.distledger.gossip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timestamp {
    private Map<Integer, Long> times;

    public Timestamp() {
        times = new HashMap<>();
    }

    public void updateTime(int replicaId, long time) {
        times.put(replicaId, time);
    }

    public long getTime(int replicaId) {
        Long time = times.get(replicaId);
        return time == null ? 0 : time;
    }

    public List<Integer> getNonNullReplicas() {
        return (List<Integer>) times.keySet();
    }

    public static boolean lessOrEqual(Timestamp t1, Timestamp t2) {
        for(int i: t1.getNonNullReplicas())
            if (t1.getTime(i) > t2.getTime(i)) return false;

        return true;
    }
}