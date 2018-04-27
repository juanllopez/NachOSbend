package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {

        //Se inicializan las variables de condición,
        //el lock y la variable wordReady, la cual
        //Indica si hay un speaker hablando.
        communicationLock = new Lock();
        currentSpeaker = new Condition2(communicationLock);
        currentListener = new Condition2(communicationLock);
        wordReady = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {

        //Se adquiere el lock de la clase
        communicationLock.acquire();

        //Se incrementa en uno la cantidad de speakers.
        speaker = speaker + 1;

        //Si no hay quien escuche o haya una word lista
        // (hay un speaker activo)
        //se mandará a dormir al speaker, hasta que una
        //de las dos condiciones se cumpla.
        while (listener < 1 || wordReady){
            currentSpeaker.sleep();
        }

        //Una vez se haya despertado, se procederá
        //a guardar el mensaje
        msg = word;

        //Se marcará que hay una palabra lista para
        //ser escuchada.
        wordReady = true;

        //Se despertará a los listeners para que escuchen
        currentListener.wakeAll();

        //Luego se disminuirá el contador de speakers y
        //se liberará el lock.
        speaker = speaker -1;
        communicationLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
        //En esta variable se guardará la palabra
        // a escuchar.
        int word;

        //Se obtiene el lock del comunicador para poder
        //escuchar
        communicationLock.acquire();

        //Se incrementa el contador de listeners
        listener = listener + 1;

        //Mientras no haya una word lista para ser escuchada,
        //se despertará a un speaker para que hable y se
        //dormirá al listener.
        while(!wordReady){
            currentSpeaker.wake();
            currentListener.sleep();
        }

        //Se guarda la palabra que fue escuchada
        word = msg;

        //Se indica que ya no hay una palabra lista para
        //ser escuchada
        wordReady = false;

        //Se disminuye el contador de listeners
        listener = listener -1;

        //Se libera el lock del comunicador y se retorna
        //La palabra que fue escuchada
        communicationLock.release();
        return word;
    }


    public static void selfTest() {
        Lib.debug(dbgThread, "Enter KThread.selfTest");


        final Communicator com = new Communicator();

        KThread thread1 = new KThread(new Runnable() {
            public void run() {
                System.out.println("Thread 1 -- Start/Speaking");
                com.speak(0);
                System.out.println("Thread 1 -- Finish/Speaking");
            }
        });

        KThread thread2 = new KThread(new Runnable() {
            public void run() {
                System.out.println("Thread 2 -- Start/Listening");
                com.listen();
                System.out.println("Thread 2 -- Finish/Listening");
            }
        });

        thread1.fork();
        thread2.fork();
        thread1.join();
        thread2.join();

    }
    private Lock communicationLock;
    private Condition2 currentSpeaker;
    private Condition2 currentListener;
    private int speaker = 0;
    private int listener =0;
    private int msg;
    private boolean wordReady;
    private static final char dbgThread = 't';
}