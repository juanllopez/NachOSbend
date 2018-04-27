package nachos.threads;
import java.util.PriorityQueue;
import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run()
            {
                timerInterrupt();
            }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt()
    {
        //Se deshabilitan los interrupts.
        boolean intStatus = Machine.interrupt().disable();

        //Si la cola waitQueue no está vacía y el primer thread posee un wakeTime menor
        //o igual que la hora de la máquina, este thread sale de la cola y se coloca en ready().
        while (!waitQueue.isEmpty() && waitQueue.peek().wakeTime <= Machine.timer().getTime())
        {
            waitQueue.poll().thread.ready();
        }
        KThread.yield();

        //Se rehabilitan losinterrupts.
        Machine.interrupt().restore(intStatus);

    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // for now, cheat just to get something working
        // (busy waiting is bad)
        long wakeTime = Machine.timer().getTime() + x;
        KThread thread = KThread.currentThread();
        WaitThread waitThread = new WaitThread(thread,wakeTime);
        boolean intStatus = Machine.interrupt().disable();
        //Se agrega el thread a la cola y se coloca en sleep.
        waitQueue.add (waitThread);
        KThread.sleep();
        Machine.interrupt().restore(intStatus);
        //while (wakeTime > Machine.timer().getTime())
        //    KThread.yield();
    }

    /**
     * En esta clase se guarda el thread junto con el wakeTime definido
     * en waitUntil().
     * Se implementa la interfaz de Comparable para ordenar así una
     * PriorityQueue con base en el wakeTime.
     * El constructor de la clase guarda los atributos enviados.
     * La función compareTo compara los wakeTime de los threads, devuelve
     * un valor negativo si es menor, 0 si es igual y 1 si es mayor.
     */
    private class WaitThread implements Comparable<WaitThread>
    {
        //En los objetos de esta clase se guardarán el thread
        //deseado y la hora en que se desea despertarlo.
        KThread thread;
        long wakeTime;

        //Constructor de la clase.
        //Asigna los valores enviados como parámetros
        public WaitThread(KThread thread, long wakeTime)
        {
            this.thread = thread;
            this.wakeTime = wakeTime;
        }

        //Método de la interfaz Comparable
        //Se utilizará para ordenar una cola formada
        //por objetos de esta misma clase.
        //Permite ordenar la cola de manera automática
        public int compareTo(WaitThread thread)
        {
            if(this.wakeTime < thread.wakeTime)
                return -1;
            else
            if(this.wakeTime > thread.wakeTime)
                return 1;
            else
                return 0;
        }

    }
    public static void selfTest() {
        Lib.debug(dbgThread, "Enter KThread.selfTest");

        /*
         *  Picks a certain amount of ticks between 0 and 1 million and calls
         *  Alarm.waitUntil with this amount of ticks. Does this several times
         *  just to show that it works properly.
         */
        long ticks;
        Alarm test = new Alarm();
        for (int i =0;i<5;i++)
        {
            ticks=(long)(Math.random()*1000000);
            System.out.println("I'm about to wait for " + ticks + " ticks.");
            test.waitUntil(ticks);
            System.out.println(ticks + " ticks later, I'm done waiting!");
        }


    }
    private static final char dbgThread = 't';

    //Cola de prioridad que se utilizará para alacenar los threads
    //que se encuentran esperando de manera ordenada.
    private PriorityQueue<WaitThread> waitQueue = new PriorityQueue<WaitThread>();
}