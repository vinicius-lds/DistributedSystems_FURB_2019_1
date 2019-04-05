import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class Util {

    static Map<Integer, Process> cloneProcessesMap(Map<Integer, Process> processes) {
        Map<Integer, Process> clone = new HashMap<>();
        Iterator<Integer> keySetIterator = processes.keySet().iterator();
        Integer key;
        while (keySetIterator.hasNext()) {
            key = keySetIterator.next();
            clone.put(key, processes.get(key));
        }
        return clone;
    }

    static Process getHighestPid(List<Process> activeProcesses) {
        AtomicReference<Process> highestPid = new AtomicReference<>(activeProcesses.size() > 1 ? activeProcesses.get(0) : null);
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
}
