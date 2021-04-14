package net.voldrich.graal.async.script;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ScriptSchedulers {

    private final List<Scheduler> schedulerList;

    private final int numberOfSchedulers;

    private volatile int nextScheduler = 0;

    public ScriptSchedulers() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ScriptSchedulers(int numberOfSchedulers) {
        this.numberOfSchedulers = numberOfSchedulers;
        this.schedulerList = createSchedulers(numberOfSchedulers);
    }

    private List<Scheduler> createSchedulers(int numberOfSchedulers) {
        List<Scheduler> list = new ArrayList<>(numberOfSchedulers);
        for (int i = 0; i < numberOfSchedulers; i++) {
            list.add(Schedulers.fromExecutorService(
                    Executors.newSingleThreadExecutor(
                            new ScriptSchedulerThreadFactory("Script-" + i))));
        }
        return Collections.unmodifiableList(list);
    }

    public Scheduler getNextScheduler() {
        nextScheduler = (nextScheduler + 1) % numberOfSchedulers;
        return schedulerList.get(nextScheduler);
    }

    public void dispose() {
        schedulerList.forEach(Scheduler::dispose);
    }

    private static final class ScriptSchedulerThreadFactory implements ThreadFactory {
        public final String name;

        public ScriptSchedulerThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(name);
            return t;
        }
    }
}
