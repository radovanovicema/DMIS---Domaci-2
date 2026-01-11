package fileindexer;

import java.io.File;
import java.util.Set;
import java.util.concurrent.*;

public class Main {

    private static final int QUEUE_CAPACITY = 100;
    private static final int N_CONSUMERS =
            Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Pokretanje: java Main <rootDir>");
            System.exit(0);
        }

        File root = new File(args[0]);

        // ograničeni red
        BlockingQueue<File> queue =
                new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        // thread-safe indeks 
        ConcurrentHashMap<String, FileMeta> indexMap =
                new ConcurrentHashMap<>();

        // statistika
        Stats stats = new Stats();

       
        Set<String> visited =
                ConcurrentHashMap.newKeySet();

        // poison pill (isti objekat za sve consumer-e)
        final File POISON_PILL = new File("__POISON_PILL__");

        // barijere
        CountDownLatch producersDone = new CountDownLatch(1);
        CountDownLatch consumersDone = new CountDownLatch(N_CONSUMERS);

        // periodična statistika (svakih 1000 ms)
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println(
                    "[STAT] found=" + stats.getFound() +
                    ", indexed=" + stats.getIndexed() +
                    ", queue=" + queue.size() +
                    ", skipped=" + stats.skippedSnapshot()
            );
        }, 0, 1000, TimeUnit.MILLISECONDS);

        Thread producer = new Thread(() -> {
            try {
                new FileCrawler(queue, root, stats, visited).run();
            } finally {
                producersDone.countDown();
            }
        }, "Producer");
        producer.start();

        Thread[] consumers = new Thread[N_CONSUMERS];
        for (int i = 0; i < N_CONSUMERS; i++) {
            consumers[i] = new Thread(
                    new Indexer(
                            queue,
                            indexMap,
                            stats,
                            POISON_PILL,
                            consumersDone
                    ),
                    "Consumer-" + (i + 1)
            );
            consumers[i].start();
        }

        try {
            producersDone.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < N_CONSUMERS; i++) {
            try {
                queue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            consumersDone.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduler.shutdown();
        try {
            scheduler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Završeno indeksiranje. Ukupno fajlova u indeksu: "
                + indexMap.size());

        System.out.println("FINAL STATS: found=" + stats.getFound() +
                ", indexed=" + stats.getIndexed() +
                ", skipped=" + stats.skippedSnapshot());

        System.out.println("Primer unosa iz indeksa:");
        indexMap.values().stream().limit(3).forEach(System.out::println);
    }
}
