import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {

    static Integer processesCreated = 0;
    private static Map<Integer, Process> processes;
    
    public static void main(String[] args) {
        Process p = new Process(processesCreated, new HashMap<>());
        Main.processesCreated++;

        // a cada 30 segundos um novo processo deve ser criado
        new Thread(() -> {
            while (true) {
                // System.out.println("new process");
                Process randomProcess = getRandom();
                randomProcess.orderToCreateProcess();
                Util.sleep(Constants.NEW_PROCESS_TIME);
            }
        }).start();

        // a cada 100 segundos o coordenador fica inativo
        new Thread(() -> {
            while (true) {
                // quando o coordenador morre, a fila também morre
                Util.sleep(Constants.KILL_COORDINATOR);
                getRandom().getCoordinator().kill();
            }
        }).start();

        
    }

    static void updateProcesses(Map<Integer, Process> processes) {
        Main.processes = Util.cloneProcessesMap(processes);
    }

    private static Process getRandom(boolean coordinator) {
        Random r = new Random();
        int randomNumber;
        Process randomProcess;
        do {
            randomNumber = r.nextInt(processes.keySet().size());
            randomProcess = processes.get(randomNumber);
        } while (randomProcess == null || !randomProcess.isActive() || (!coordinator && randomProcess.isCoordinator()));
        return randomProcess;
    }

    private static Process getRandom() {
        return getRandom(true);
    }

}
