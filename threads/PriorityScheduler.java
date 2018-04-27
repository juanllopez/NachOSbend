package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 * <p/>
 * <p/>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 * <p/>
 * <p/>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 * <p/>
 * <p/>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks,` and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority <tt>true</tt> if this queue should
	 *                         transfer priority from waiting threads
	 *                         to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	/**
	 * Obtener la prioridad del thread
	 * Verifica si las interrupciones están
	 * deshabilitadas antes de devolver
	 * la prioridad
	 *
	 * @param thread
	 * @return Prioridad del thread
	 */
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	/**
	 * Obtiene la prioridad efectiva del thread.
	 *
	 * @param thread
	 * @return
	 */
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	/**
	 * Asigna la prioridad al thread
	 *
	 * @param thread
	 * @param priority
	 */
	public void setPriority(KThread thread, int priority) {
		//Revisa si las interrupciones están deshabilitadas.
		Lib.assertTrue(Machine.interrupt().disabled());

		//Revisa si la prioridad que quiere asignarse está
		//dentro del rango permitido.
		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		//Asigna la prioridad al thread.
		getThreadState(thread).setPriority(priority);
	}

	/**
	 * Incrementa la prioridad del thread actual
	 *
	 * @return
	 */
	public boolean increasePriority() {

		//Deshabilita interrupciones para asegurar atomicidad
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		//Obtiene la prioridad del thread actual
		int priority = getPriority(thread);

		//Retorna si la prioridad ya es la máxima permitida
		if (priority == priorityMaximum)
			return false;

		//Asigna la nueva prioridad (una unidad mayor)
		setPriority(thread, priority + 1);

		//Restaura interrupciones y retorna true, indicando
		//que la operación fue un éxito.
		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * Reduce la prioridad del thread actual.
	 *
	 * @return
	 */
	public boolean decreasePriority() {

		//Se deshabilitan las interrupciones para segurar
		//atomicidad
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		//Se obtiene la prioridad del thread actual y se
		//compara con la prioridad mínima permitida.
		//Retorna falso si es igual a esta prioridad.
		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		//Asigna la nueva prioridad
		setPriority(thread, priority - 1);

		//Restaura interrupciones y retorna true, indicando
		//que la operación fue un éxito.
		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}


	//clase Priority queue, escencial para este ejercicio
	protected class PriorityQueue extends ThreadQueue {

		//para permitir prestamos de prioridad
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.threadsWaiting = new LinkedList<ThreadState>();
		}

		/*
		 Guarda el estado del thread enviado como parámetro
		 y lo ingresa en la lista de espera
		 */
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			final ThreadState ts = getThreadState(thread);
			this.threadsWaiting.add(ts);
			ts.waitForAccess(this);
		}

		/*
		 Si hay alguien que posee el recurso, se libera
		 y se coloca al thread enviado como parámetro
		 como el nuevo poseedor del recurso.
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			final ThreadState ts = getThreadState(thread);
			if (this.resourceHolder != null) {
				this.resourceHolder.release(this);
			}
			this.resourceHolder = ts;
			ts.acquire(this);
		}


		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			// Pick the next thread
			final ThreadState nextThread = this.pickNextThread();

			if (nextThread == null) return null;

			// Remove the next thread from the queue
			this.threadsWaiting.remove(nextThread);

			// Give nextThread the resource
			this.acquire(nextThread.getThread());

			return nextThread.getThread();
		}


		//retorna el siguiente thread a ejecutar, dependiendo de la prioridad y del age del thread, siendo esta el momento donde inserto a la lista de threads waiting
		protected ThreadState pickNextThread() {
			int nextPriority = priorityMinimum;
			//ThreadState next = null;
			ThreadState next = threadsWaiting.peek();
			for (final ThreadState currThread : this.threadsWaiting) {
				int currPriority = currThread.getEffectivePriority();
				if (next == null || (currPriority > nextPriority)) {
					next = currThread;
					nextPriority = currPriority;
				} else if (currPriority == nextPriority) {
					if (currThread.age <= next.age) {
						//System.out.println(currThread.getThread().getName());
						//System.out.println(currThread.age);
						//System.out.println(next.getThread().getName());
						//System.out.println(next.age);

						//th = currThread;
						nextPriority = currPriority;
						next = currThread;
					}
				}

			}
			return next;
		}


		//devuelve la prioridad effectiva para este queue, donde revisa si el flag que ocurrio un cambio esta dirty
		public int getEffectivePriority() {
			if (!this.transferPriority) {
				return priorityMinimum;
			} else if (this.dirty) {
				// Recalculate effective priorities
				this.effectivePriority = priorityMinimum;
				for (final ThreadState curr : this.threadsWaiting) {
					this.effectivePriority = Math.max(this.effectivePriority, curr.getEffectivePriority());
				}
				this.dirty = false;
			}
			return effectivePriority;
		}


		//imprime ka prioridad efectiva de los threads en espera.

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			for (final ThreadState ts : this.threadsWaiting) {
				System.out.println(ts.getEffectivePriority());
			}
		}

		//invalida la prioridad del priority queue y del holder de esta
		private void makeDirty() {
			if (!this.transferPriority) return;

			this.dirty = true;

			if (this.resourceHolder != null) {
				resourceHolder.makeDirty();
			}
		}


		protected final LinkedList<ThreadState> threadsWaiting;

		//el thread que es el holder de la priority queue
		protected ThreadState resourceHolder = null;

		//prioridad que se toma inmediatamente cuando el queue no esta dirty
		protected int effectivePriority = priorityMinimum;


		//flag de dirty, para saber si el effective priority no esta actualizado
		protected boolean dirty = false;

		//saber si el queue permite transferir prioridad
		public boolean transferPriority;
	}


	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			this.myQueues = new LinkedList<PriorityQueue>();
			this.QueuesWanted = new LinkedList<PriorityQueue>();

			setPriority(priorityDefault);

		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		//retorna la effective priority, y entra al loop dependiendo si esta dirty o no
		public int getEffectivePriority() {

			if (this.myQueues.isEmpty()) {
				return this.getPriority();
			} else if (this.dirty) {
				this.effectivePriority = this.getPriority();
				for (final PriorityQueue pq : this.myQueues) {
					this.effectivePriority = Math.max(this.effectivePriority, pq.getEffectivePriority());
				}
				this.dirty = false;
			}
			return this.effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority the new priority.
		 */
		//se le asigna nueva prioridad al thread y se marca dirty las que quiere
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			this.priority = priority;
			// force priority invalidation
			for (final PriorityQueue pq : QueuesWanted) {
				pq.makeDirty();
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param waitQueue the queue that the associated thread is
		 *                  now waiting on.
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		//se le agrega el age, para que tenga en cuenta el tiempo donde se agregan a la cola
		public void waitForAccess(PriorityQueue waitQueue) {
			this.age = Machine.timer().getTime();
			//System.out.println(this.getThread().getName());
			//System.out.println(this.age);
			this.QueuesWanted.add(waitQueue);
			this.myQueues.remove(waitQueue);
			waitQueue.makeDirty();

		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		//se le asigana un queue al thread y se marca dirty
		public void acquire(PriorityQueue waitQueue) {
			this.myQueues.add(waitQueue);
			this.QueuesWanted.remove(waitQueue);
			this.makeDirty();
		}

		/**
		 * Called when the associated thread has relinquished access to whatever
		 * is guarded by waitQueue.
		 *
		 * @param waitQueue The waitQueue corresponding to the relinquished resource.
		 */
		//libera al queue del thread y se marca dirty
		public void release(PriorityQueue waitQueue) {
			this.myQueues.remove(waitQueue);
			this.makeDirty();
		}

		public KThread getThread() {
			return thread;
		}

		//se marca dirty el thread
		private void makeDirty() {
			if (this.dirty) return;
			this.dirty = true;
			for (final PriorityQueue pq : this.QueuesWanted) {
				pq.makeDirty();
			}
		}


		/**
		 * The thread with which this object is associated.
		 */
		protected KThread thread;
		/**
		 * The priority of the associated thread.
		 */
		protected int priority;

		/**
		 * True if effective priority has been invalidated for this ThreadState.
		 */
		protected boolean dirty = false;
		/**
		 * Holds the effective priority of this Thread State.
		 */
		protected int effectivePriority = priorityMinimum;
		/**
		 * A list of the queues for which I am the current resource holder.
		 */
		protected final List<PriorityQueue> myQueues;

		//
		protected final List<PriorityQueue> QueuesWanted;

		protected long age;

	}


	public static void selfTest() {
		/*
		 * Creates 3 threads with different priorities and runs them
		 */
		System.out.println("Priority TEST #2: START");
		KThread thread1 = new KThread(new Runnable() {
			public void run() {

				//KThread.yield();

				System.out.println("Im first to run");
			}
		}).setName("Thread1");
		KThread thread2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Im Second to run");
			}
		}).setName("Thread2");
		KThread thread3 = new KThread(new Runnable() {

			public void run() {

				//KThread.yield();
				System.out.println("Im Third to run");
			}
		}).setName("Thread3");
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(3);
		ThreadedKernel.scheduler.setPriority(thread1, 7);
		ThreadedKernel.scheduler.setPriority(thread2, 6);
		ThreadedKernel.scheduler.setPriority(thread3, 7);
		Machine.interrupt().enable();
		thread1.fork();
		thread2.fork();
		thread3.fork();

		KThread.yield();
		System.out.println("Priority TEST #2: END");
	}

	public static void selfTest2() {
		System.out.println("Priority TEST #1: Start");
		final Lock theBestLock = new Lock();
		theBestLock.acquire();
		KThread thread1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Important thread wants the lock");
				theBestLock.acquire();
				System.out.println("Important thread got what it wanted");
				theBestLock.release();
			}
		});
		KThread thread3 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Important thread wants the lock");
				theBestLock.acquire();
				System.out.println("Important thread got what it wanted");
				theBestLock.release();
			}
		});

		ThreadHogger th = new ThreadHogger();
		KThread thread2 = new KThread(th);
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(3);
		ThreadedKernel.scheduler.setPriority(thread1, 7);
		ThreadedKernel.scheduler.setPriority(thread2, 7);
		ThreadedKernel.scheduler.setPriority(thread3, 4);
		Machine.interrupt().enable();

		thread1.fork();
		thread2.fork();
		thread3.fork();
		//cant get back without donation
		KThread.yield();
		System.out.println("Main thread letting go of the lock");
		theBestLock.release();
		th.d = 1;
		KThread.yield();
		System.out.println("Priority TEST #1: END");
	}

	public static void selfTest3() {
		System.out.println("---------PriorityScheduler test---------------------");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);

		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		KThread thread5 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");
		thread5.setName("thread5");


		boolean intStatus = Machine.interrupt().disable();

		s.getThreadState(thread1).setPriority(7);
		queue.acquire(thread1);
		s.getThreadState(thread2).setPriority(1);
		s.getThreadState(thread3).setPriority(4);
		s.getThreadState(thread4).setPriority(4);
		s.getThreadState(thread5).setPriority(4);
		queue.waitForAccess(thread2);
		queue.waitForAccess(thread3);
		queue.waitForAccess(thread4);
		queue.waitForAccess(thread5);
		System.out.println("thread1 EP=" + s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP=" + s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread3 EP=" + s.getThreadState(thread3).getEffectivePriority());
		System.out.println("thread4 EP=" + s.getThreadState(thread4).getEffectivePriority());
		System.out.println("thread5 EP=" + s.getThreadState(thread5).getEffectivePriority());

		//System.out.println(s.getThreadState(thread4).timeWaitQueue);
		//System.out.println(s.getThreadState(thread5).timeWaitQueue);

		System.out.println("Next Thread to be picked = " + queue.nextThread());

		Machine.interrupt().restore(intStatus);
		System.out.println("--------End PriorityScheduler test------------------");
	}

	private static class ThreadHogger implements Runnable {
		public int d = 0;

		public void run() {
			while (d == 0) {
				KThread.yield();
			}

		}
	}

	public static void selfTestRun(KThread t1, int t1p, KThread t2, int t2p) {

		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(t1, t1p);
		ThreadedKernel.scheduler.setPriority(t2, t2p);
		Machine.interrupt().restore(int_state);

		t1.setName("a").fork();
		t2.setName("b").fork();
		t1.join();
		t2.join();

	}

	public static void selfTestRun2(KThread t1, int t1p, KThread t2, int t2p, KThread t3, int t3p) {

		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(t1, t1p);
		ThreadedKernel.scheduler.setPriority(t2, t2p);
		ThreadedKernel.scheduler.setPriority(t3, t3p);
		Machine.interrupt().restore(int_state);

		t1.setName("a").fork();
		t2.setName("b").fork();
		t3.setName("c").fork();
		t1.join();
		t2.join();
		t3.join();

	}

	public static void selfTest4() {

		KThread t1, t2, t3;
		final Lock lock;
		final Condition2 condition;

		/*
		 * Case 1: Tests priority scheduler without donation
		 *
		 * This runs t1 with priority 7, and t2 with priority 4.
		 *
		 */

		System.out.println("Case 1:");

		t1 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName() + " started working");
				for (int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName() + " finished working");
			}
		});

		t2 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName() + " started working");
				for (int i = 0; i < 10; ++i) {
					System.out.println(KThread.currentThread().getName() + " working " + i);
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName() + " finished working");
			}

		});
		selfTestRun(t1, 7, t2, 4);

		System.out.println("Case 2:");

		t1 = new KThread(new Runnable()
		{
			public void run()
			{
				System.out.println(KThread.currentThread().getName() + " started working");
				for (int i = 0; i < 10; ++i)
				{
					System.out.println(KThread.currentThread().getName() + " working " + i);
					KThread.yield();
					if (i == 4)
					{
						System.out.println(KThread.currentThread().getName() + " reached 1/2 way, changing priority");
						boolean int_state = Machine.interrupt().disable();
						ThreadedKernel.scheduler.setPriority(2);
						Machine.interrupt().restore(int_state);
					}
				}
				System.out.println(KThread.currentThread().getName() + " finished working");
			}
		});

		t2 = new KThread(new Runnable()
		{
			public void run()
			{
				System.out.println(KThread.currentThread().getName() + " started working");
				for (int i = 0; i < 10; ++i)
				{
					System.out.println(KThread.currentThread().getName() + " working " + i);
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName() + " finished working");
			}

		});

		selfTestRun(t1, 7, t2, 4);

		System.out.println("Case 3:");

		lock = new Lock();
		condition = new Condition2(lock);

		t1 = new KThread(new Runnable()
		{
			public void run()
			{
				lock.acquire();
				System.out.println(KThread.currentThread().getName() + " active");
				lock.release();
			}
		});

		t2 = new KThread(new Runnable()
		{
			public void run()
			{
				System.out.println(KThread.currentThread().getName() + " started working");
				for (int i = 0; i < 3; ++i)
				{
					System.out.println(KThread.currentThread().getName() + " working " + i);
					KThread.yield();
				}
				System.out.println(KThread.currentThread().getName() + " finished working");
			}

		});

		t3 = new KThread(new Runnable()
		{
			public void run()
			{
				lock.acquire();

				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(2);
				Machine.interrupt().restore(int_state);

				KThread.yield();

// t1.acquire() will now have to realise that t3 owns the lock it wants to obtain
// so program execution will continue here.

				System.out.println(KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)");
				lock.release();
				KThread.yield();
				lock.acquire();
				System.out.println(KThread.currentThread().getName() + " active-again (should be after 'a' and 'b' done)");
				lock.release();

			}
		});

		selfTestRun2(t1, 6, t2, 4, t3, 7);


	}
}