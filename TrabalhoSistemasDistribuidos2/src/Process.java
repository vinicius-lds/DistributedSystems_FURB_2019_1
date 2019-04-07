import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Process implements Runnable {

    private Map<Integer, Process> processes;
    private Boolean isActive;
    private Integer index;
    private Integer pid;
    // It's used a LinkedList as a queue because it doesn't have a limit of size
    private LinkedList<Process> queue;

    private Boolean orderedToCreateProcess;
    private Boolean startedElection;
    private Boolean electionInProgress;
    private Boolean accessGranted;
    private Boolean lock;
    private Process coordinator;

    Process(Integer index, Map<Integer, Process> processes) {
        this.processes = Util.cloneProcessesMap(processes);
        this.isActive = true;
        this.index = index;
        this.pid = Util.getRandomPid(index);
        this.orderedToCreateProcess = false;
        this.startedElection = false;
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
        while (this.isActive && this.coordinator != this) {
            
            try {
                this.coordinator.requestAndEnterQueue(this);
                while (!this.accessGranted && this.coordinator != this) {
                    Util.sleep(0L);
                    this.coordinator.request(this, true);
                }

                if (this.coordinator != this) 
                    this.coordinator.consume(this);

            } catch (TimeoutException e) {
                System.out.println("The coordinator " + this.coordinator.pid + " didn't respond to the request and the PID " + this.pid + " started a new election!");
                this.initializeElection();
            }

            if (this.orderedToCreateProcess) {
                this.orderedToCreateProcess = false;
                try {
                    this.createProcess();
                } catch (IllegalArgumentException e) {
                }
            }
            
            // os processos tentam consumir o(s) recurso(s) num intervalo de 10 à 25 segundos
            Util.sleepRange(Constants.WAIT_CONSUME_COORDINATOR_TIME_LOWER, Constants.WAIT_CONSUME_COORDINATOR_TIME_HIGHER);
        }
        
        while (this.isActive && this.coordinator == this) {
            
            synchronized (this) {
                if (!this.lock && this.queue.size() > 0) {
                    this.lock = true;
                    Process first = this.queue.getFirst();
                    this.queue.remove(first);
                    System.out.println("The coordinator " + this.pid + " granted access to the PID " + first.pid);
                    first.accessGranted = true;
                }
            }
            
            if (this.orderedToCreateProcess) {
                this.orderedToCreateProcess = false;
                try {
                    this.createProcess();
                } catch (IllegalArgumentException e) {
                }
            }
            
            Util.sleep(0L);
        }
        
    }
    
    /**
     * Consumes the coordinator
     * @param origin process that is consuming
     */
    private synchronized void consume(Process origin) {
        this.lock = true;
        System.out.println("PID " + origin.pid + " started consuming the coordinator PID " + this.pid);
        //o tempo de processamento de um recurso é de 5 à 15 segundos
        Util.sleepRange(Constants.CONSUME_COORDINATOR_TIME_LOWER, Constants.CONSUME_COORDINATOR_TIME_HIGHER);
        if (this.isActive) {
            System.out.println("PID " + origin.pid + " stopped consuming the coordinator PID " + this.pid);            
            origin.kill();
        } else {
            System.out.println("PID " + origin.pid + " didn't end consuming the coordinator PID " + this.pid + " because he died half way throught");
        }
        this.lock = false;
    }

    /**
     * Finds the first active process mapped, gets he's coordinator and set's it
     * for himself. If there is no active processes, sets itself as coordintor
     */
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
                nextProcess = nextProcess.getNextFrom(nextProcess.index);
            }

        }
    }

    /**
     * Request the process current coordinator 
     * @return the process coordintor
     * @throws TimeoutException if this process is not active
     */
    private Process requestCoordinator() throws TimeoutException {
        timeoutVerifier();
        return this.coordinator;
    }

    /**
     * Sets a new coordinator to every process created
     * @param process new coordinator
     */
    private synchronized void setNewCoordinator(Process process) {
        System.out.println("A new coordinator was elected " + process.pid);
        Iterator<Integer> keySetIterator = this.processes.keySet().iterator();
        Process current;
        while (keySetIterator.hasNext()) {
            current = this.processes.get(keySetIterator.next());
            if (current.isActive) {
                current.coordinator = process;
                current.electionInProgress = false;
            }
        }
        process.queue = new LinkedList();
        process.coordinator = process;
    }

    /**
     * Finds the next process after the one passed
     * @param index process reference to get next
     * @return the next process after the one passed
     */
    private synchronized Process getNextFrom(Integer index) {
        Integer newIndex = index + 1;
        if (newIndex >= this.getProccesCount()) {
            return this.processes.get(0);
        } else {
            return this.processes.get(newIndex);
        }
    }

    /**
     * @return total count of created processes
     */
    private Integer getProccesCount() {
        return this.processes.keySet().size();
    }

    /**
     * @return the next process mapped
     */
    private Process getNext() {
        return this.getNextFrom(this.index);
    }

    /**
     * The process puts himself in the map of processes of all active processes
     */
    private void putSelf() {
        this.processes.forEach((key, process) -> process.putProcess(this));
        this.processes.put(this.index, this);
    }

    /**
     * Puts a new process in map of processes 
     * @param process process to put in the map
     * @throws IllegalArgumentException if the Process ID already exists
     */
    private synchronized void putProcess(Process process) {
        if (this.processes.get(process.index) == null) {
            this.processes.put(process.index, process);
        } else {
            throw new IllegalArgumentException("PID " + process.pid + " has already been created!");
        }
    }

    /**
     * When called, the next iteration of this process will criate a new one
     */
    void orderToCreateProcess() {
        this.orderedToCreateProcess = true;
    }

    /**
     * Creates a new processes to be inserted into the conext of the application
     */
    synchronized void createProcess() {
        new Process(Main.processesCreated, this.processes);
        Main.processesCreated++;
    }

    /**
     * Checks if the process is currently active
     * @throws TimeoutException if the process is currently not active
     */
    private void timeoutVerifier() throws TimeoutException {
        if (!this.isActive) {
            throw new TimeoutException("PID " + this.pid + " is dead!");
        }
    }

    /**
     * Simulates a request to this process
     * @param origin process that made the request
     * @param supressLog boolean to supress log of request
     * @throws TimeoutException if the process is currently not active
     */
    private void request(Process origin, boolean supressLog) throws TimeoutException {
        this.timeoutVerifier();
        if (!supressLog) 
            System.out.println("PID " + origin.pid + " made a request to the PID " + this.pid);
    }
    
    /**
     * Simulates a request to this process
     * @param origin process that made the request
     * @throws TimeoutException if the process is currently not active
     */
    private void request(Process origin) throws TimeoutException {
        this.request(origin, false);
    }
    
    /**
     * Simulates a request to this process, and adds the process that made the
     * request to the queue of processes to consume the coordinator
     * @param origin process that made the request
     * @throws TimeoutException if the process is currently not active
     */
    private synchronized void requestAndEnterQueue(Process origin) throws TimeoutException {
        this.queue.addLast(origin);
        System.out.println("PID " + origin.pid + " was added do the queue of the coordinator " + this.pid);
    }

    /**
     * Initializes the election of a new coordinator
     */
    private void initializeElection() {
        if (this.electionInProgress) 
            return;
        this.startedElection = true;
        this.electionInProgress = true;
        this.election(new ArrayList<>());
    }

    /**
     * Recusive mathod that makes the election
     * @param activeProcesses list of active processes that are part of the election
     */
    private void election(List<Process> activeProcesses) {

        // Deu a volta e retornou no cara que chamou a eleição
        if (activeProcesses.size() > 0 && this == activeProcesses.get(0)) {
            this.setNewCoordinator(Util.getHighestPid(activeProcesses));
            this.startedElection = false;
            return;
        }
        
        if (this.startedElection && activeProcesses.size() > 0 && this.pid > activeProcesses.get(0).pid) {
            activeProcesses.get(0).startedElection = false;
            return;
        } else 
            this.electionInProgress = true;
            

        activeProcesses.add(this);
        Process process = this.getNext();

        while (true) {
            
            // Existe somente um processo ativo, então ele vira o coordenador
            if (process == this) {
                this.setNewCoordinator(this);
                this.startedElection = false;
                return;
            }
            
            // Caso ele achar um processo que não é o que iniciou essa eleição
            // e que tem um pid maior do que o que iniciou a eleição
            // é cancelada essa eleição
            if (process.startedElection && this.pid > activeProcesses.get(0).pid) {
                activeProcesses.get(0).startedElection = false;
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
                process = process.getNextFrom(process.index);
            }
        }
    }

    /**
     * Kills the process
     */
    void kill() {
        this.isActive = false;
        if (this.coordinator == this) {
            System.out.println("The coordinator PID " + this.pid + " and he's queue died");
        } else {
            System.out.println("PID " + this.pid + " is dead");
        }
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
