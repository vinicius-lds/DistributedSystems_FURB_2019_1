import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {

    static Integer processesCreated = 0;
    private static Map<Integer, Process> processes;

    private static final int NEW_PROCESS_TIME = 3; //30;
    private static final int REQUEST_TIME = 2; //25;
    private static final int KILL_COORDINATOR = 10; //100;
    private static final int KILL_PROCESS = 8; //80;

    public static void main(String[] args) {
        Process p = new Process(processesCreated, new HashMap<>());
        Main.processesCreated++;

        // a cada 30 segundos um novo processo deve ser criado
        new Thread(() -> {
            while (true) {
                // System.out.println("new process");
                Process randomProcess = getRandom();
                randomProcess.orderToCreateProcess();
                Util.sleep(NEW_PROCESS_TIME);
            }
        }).start();

        // a cada 25 segundos um processo fazer uma requisição para o coordenador
        new Thread(() -> {
            while (true) {
                // System.out.println("new request");
                getRandom(false).orderToMakeRequest();
                Util.sleep(REQUEST_TIME);
            }
        }).start();

        // a cada 100 segundos o coordenador fica inativo
        new Thread(() -> {
            while (true) {
                // System.out.println("mata coordenador");
                Util.sleep(KILL_COORDINATOR);
                getRandom().getCoordinator().kill();
            }
        }).start();

        // a cada 80 segundos um processo da lista de processos fica inativo
        new Thread(() -> {
            while (true) {
                // System.out.println("mata processo");
                Util.sleep(KILL_PROCESS);
                getRandom(false).kill();
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
