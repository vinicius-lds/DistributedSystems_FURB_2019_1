
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Process implements Runnable {

    private Map<Integer, Process> processes;
    private Boolean isActive;
    private Integer pid;
    private Queue<Process> queue;

    private Boolean orderedToCreateProcess;
    private Boolean electionInProgress;
    private Boolean accessGranted;
    private Boolean lock;
    private Process coordinator;

    Process(Integer pid, Map<Integer, Process> processes) {
        this.processes = Util.cloneProcessesMap(processes);
        this.isActive = true;
        this.pid = pid;
        this.orderedToCreateProcess = false;
        this.electionInProgress = false;
        this.accessGranted = false;
        this.lock = false;
        this.putSelf();
        Main.updateProcesses(this.processes);
        this.findCoordinator();
        new Thread(this).start();
    }
    
    @Override
    public void run() {
        System.out.println("PID " + this.pid + " created!");
        while (this.isActive) {
            
            if (this.coordinator == this) {
                
                if (this.lock)
                    Util.sleep(1);
                else
                    this.queue.poll().accessGranted = true;
                
            } else {
                if (this.orderedToCreateProcess) {
                    this.orderedToCreateProcess = false;
                    try {
                        this.createProcess();
                    } catch (IllegalArgumentException e) {
                    }
                }

                try {
                    System.out.println("PID " + this.pid + " made a request to the coordinator " + this.coordinator.pid);
                    this.coordinator.request(this);
                    while (this.accessGranted) { Util.sleep(1); }
                    this.coordinator.consume();
                    this.accessGranted = false;
                } catch (TimeoutException e) {
                    System.out.println("The coordinator " + this.coordinator.pid + " didn't respond to the request and the PID " + this.pid + " started a new election!");
                    this.initializeElection();
                }
                Util.sleep(new Random().nextInt(15) + 10);
            }

        }
    }
    
    private void consume() {
        this.lock = true;
        Util.sleep(new Random().nextInt(10) + 5);
        this.lock = false;
    }

    private void findCoordinator() {
        Process nextProcess = this.getNext();
        while (true) {
            if (nextProcess == this) {
                this.setNewCoordinator(this);
                return;
            }

            try {
                this.coordinator = nextProcess.requestCoordinator();
                return;
            } catch (TimeoutException e) {
                nextProcess = nextProcess.getNextFrom(nextProcess.pid);
            }

        }
    }

    private Process requestCoordinator() throws TimeoutException {
        timeoutVerifier();
        return this.coordinator;
    }

    private void setNewCoordinator(Process process) {
        System.out.println("A new coordinator was elected " + process.pid);
        Iterator<Integer> keySetIterator = this.processes.keySet().iterator();
        Process current;
        while (keySetIterator.hasNext()) {
            current = this.processes.get(keySetIterator.next());
            if (current.isActive) {
                current.coordinator = process;
            }
        }
        process.queue = new ArrayDeque<>();
    }

    private Process getNextFrom(Integer pid) {
        Integer newPid = pid + 1;
        if (newPid >= this.getTotalProcesses()) {
            return this.processes.get(0);
        } else {
            return this.processes.get(newPid);
        }
    }

    private Integer getTotalProcesses() {
        return this.processes.keySet().size();
    }

    private Process getNext() {
        return this.getNextFrom(this.pid);
    }

    private void putSelf() {
        this.processes.forEach((key, process) -> process.putProcess(this));
        this.processes.put(this.pid, this);
    }

    private void putProcess(Process process) {
        if (this.processes.get(process.pid) == null) {
            this.processes.put(process.pid, process);
        } else {
            throw new IllegalArgumentException("PID " + process.pid + " has already been created!");
        }
    }

    void orderToCreateProcess() {
        this.orderedToCreateProcess = true;
    }

    void createProcess() {
        new Process(Main.processesCreated, this.processes);
        Main.processesCreated++;
    }

    private void timeoutVerifier() throws TimeoutException {
        if (!this.isActive) {
            throw new TimeoutException("PID " + this.pid + " is dead!");
        }
    }

    private void request(Process origin) throws TimeoutException {
        this.timeoutVerifier();
        System.out.println("PID " + origin.pid + " made a request to the PID " + this.pid);
        if (this.coordinator == this) {
            this.queue.add(origin);
            System.out.println("PID " + origin.pid + " was added do the queue of the coordinator " + this.pid);
        }
    }

    private void initializeElection() {
        this.electionInProgress = true;
        this.election(new ArrayList<>());
    }

    private void election(List<Process> activeProcesses) {

        // Deu a volta e retornou no cara que chamou a eleição
        if (activeProcesses.size() >= 1 && this == activeProcesses.get(0)) {
            this.setNewCoordinator(Util.getHighestPid(activeProcesses));
            this.electionInProgress = false;
            return;
        }

        activeProcesses.add(this);
        Process process = this.getNext();

        // Existe somente um processo ativo, então ele vira o coordenador
        if (process == this) {
            this.setNewCoordinator(this);
            this.electionInProgress = false;
            return;
        }

        while (true) {
            // Caso ele achar um processo que não é o que iniciou essa eleição
            // e que tem um pid maior do que o que iniciou a eleição
            // é cancelada essa eleição
            if (process.electionInProgress && this.pid > activeProcesses.get(0).pid) {
                activeProcesses.get(0).electionInProgress = false;
                return;
            }

            // Tenta-se fazer um requisição para o proximo processo
            // Caso de timeout, e chamado o proximo processo
            // Caso não de timeout, a eleição é passada em diante
            try {
                process.request(this);
                process.election(activeProcesses);
                return;
            } catch (TimeoutException e) {
                process = process.getNextFrom(process.pid);
            }
        }
    }

    void kill() {
        System.out.println("PID " + this.pid + " is dead");
        this.isActive = false;
    }

    boolean isActive() {
        return this.isActive;
    }

    Process getCoordinator() {
        return this.coordinator;
    }

    boolean isCoordinator() {
        return this.coordinator == this;
    }

    Integer getPid() {
        return this.pid;
    }
}
