package com.kcl.osc.imageprocessor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

public class ThreadPool implements Runnable {

    private final int size;
    private Queue<Runnable> waiting;
    private List<Thread> active;

    public ThreadPool(int size) {
        this.size = size;
        this.waiting = new LinkedList<>();
        this.active = new ArrayList<>();
    }

    /**
     * Submits task to waiting queue
     * @param task implements runnable to pass to thread
     */
    public void submit(Runnable task) {
        waiting.add(task);
    }

    /**
     * While tasks waiting (and not max threads reached),
     * pass tasks to Threads
     */
    public void activateThreads() {
        while(active.size() < this.size || waiting.size() > 0) {
            Runnable next = waiting.poll();
            active.add(new Thread(next));
            active.get(active.size() - 1).start();
        }
    }


    /**
     * Start thread pool by creating threads
     */
    public void start() {
        activateThreads();
        this.run();
    }


    /**
     * Clear terminated threads and pass any new
     * tasks to new threads while waiting is not empty
     */
    public void run() {
        // Run till waiting is empty
        while(waiting.size() > 0) {
            clearActive();
            this.activateThreads();
        }
    }

    /**
     * Rid of terminated threads
     */
    public void clearActive() {
        List<Thread> terminatedThreads = new ArrayList<>();

        for(Thread thread : active) {
            if(thread.getState() == Thread.State.TERMINATED) {
                terminatedThreads.add(thread);
            }
        }

        for(Thread thread : terminatedThreads) {
            active.remove(thread);
        }
    }

    /**
     * Make active threads wait till finished
     */
    public void join() {
        while(active.size() > 0) {
            try {
                clearActive();
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Terminate all threads and clear active
     */
    public void quit() {
        for(Thread thread : active) {
            thread.stop();
        }
        active.clear();
    }

}
