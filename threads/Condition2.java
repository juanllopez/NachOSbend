package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;

        //Se inicializa la cola de espera.
        waitQueue=ThreadedKernel.scheduler.newThreadQueue(false);

    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep()
    {
        //Revisa si el lock es poseído por el thread actual.
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        //Se deshabilitan las interrupciones.
        boolean inStatus=Machine.interrupt().disable();

        //Se libera el lock.
        conditionLock.release();

        //El thread actual se marca como esperando a accesar
        // el recurso y se duerme.
        waitQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();

        //Se obtiene el lock.
        conditionLock.acquire();

        //Se restauran las interrupciones.
        Machine.interrupt().restore(inStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake()
    {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        //Se deshabilitan las interrupciones.
        boolean inStatus=Machine.interrupt().disable();

        //Se obtiene el siguiente thread que está en la
        // cola de espera.
        KThread thread=waitQueue.nextThread();

        //Si el thread existe (waitQueue.nextThread() devuelve
        // null si no hay siguiente thread en la cola) se
        // coloca en ready.
        if(thread!=null)
        {
            thread.ready();
        }

        //Se restauran los interrupts.
        Machine.interrupt().restore(inStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll()
    {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        //Se deshabilitan los interrupts.
        boolean inStatus=Machine.interrupt().disable();

        //Se coloca en ready cada elemento de la cola.
        //Si no hay más elementos, nextThread() devuelve
        // null y se sale del ciclo
        while(true)
        {
            KThread thread=waitQueue.nextThread();
            if(thread != null)
            {
                thread.ready();
            }
            else {
                break;
            }
        }

        //Se restauran los interrupts.
        Machine.interrupt().restore(inStatus);
    }
    public static void selfTest() {
        Lib.debug(dbgThread, "Enter KThread.selfTest");

        /*
         * Tests condition2 by spawning a thread that goes to sleep and
         * is waken up by the main thread
         */
        System.out.println("Condition TEST #1: Start");
        final Lock lock = new Lock();
        final Condition2 condition = new Condition2(lock);
        KThread thread1 = new KThread(new Runnable(){
            public void run(){
                lock.acquire();
                System.out.println("Thread is going to sleep");
                condition.sleep();
                System.out.println("Thread has been woken up");
                lock.release();
            }
        });
        KThread thread2 = new KThread(new Runnable(){
            public void run(){
                lock.acquire();
                System.out.println("Thread is going to sleep");
                condition.sleep();
                System.out.println("Thread has been woken up");
                lock.release();
            }
        });
        KThread thread3 = new KThread(new Runnable(){
            public void run(){
                lock.acquire();
                System.out.println("Thread is going to sleep");
                condition.sleep();
                System.out.println("Thread has been woken up");
                lock.release();
            }
        });
        thread1.fork();
        thread2.fork();
        thread3.fork();
        System.out.println("Main: yielding to run the other thread");
        KThread.yield();
        System.out.println("Main: sending the wake signal then yeilding");
        lock.acquire();
        condition.wakeAll();
        lock.release();
        KThread.yield();
        System.out.println("Condition TEST #1: End");

    }
    private static final char dbgThread = 't';


    private Lock conditionLock;
    private ThreadQueue waitQueue;
}