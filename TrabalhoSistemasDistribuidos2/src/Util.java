import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

class Util {

    static synchronized Map<Integer, Process> cloneProcessesMap(Map<Integer, Process> processes) {
        Map<Integer, Process> clone = Collections.synchronizedMap(new HashMap<>());
        Iterator<Integer> keySetIterator = processes.keySet().iterator();
        Integer key;
        while (keySetIterator.hasNext()) {
            key = keySetIterator.next();
            clone.put(key, processes.get(key));
        }
        return clone;
    }

    static Process getHighestPid(List<Process> activeProcesses) {
        AtomicReference<Process> highestPid = new AtomicReference<>(activeProcesses.size() > 0 ? activeProcesses.get(0) : null);
        activeProcesses.forEach(process -> {
            if (highestPid.get().getPid() < process.getPid())
                highestPid.set(process);
        });
        return highestPid.get();
    }

    static void sleep(int seconds) {
        sleep(seconds * 1000L);
    }

    static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    static void sleepRange(long lower, long higher) {
        try {
            Thread.sleep(new Random().nextInt(Integer.parseInt(String.valueOf(higher - lower))) + lower);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    static void sleepRange(int lower, int higher) {
        try {
            Thread.sleep((new Random().nextInt(higher - lower) + lower) * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final List<Integer> generatedPids = new ArrayList<>();
    
    static Integer getRandomPid(Integer index) {
        synchronized (Util.generatedPids) {
            Random r = new Random();
            int newPid;
            do {
                newPid = r.nextInt((index + 1) * 100);
            } while (Util.generatedPids.contains(newPid));
            return newPid;
        }
    }
}
