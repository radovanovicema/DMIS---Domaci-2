package fileindexer;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Indexer implements Runnable {

    private final BlockingQueue<File> queue;
    private final ConcurrentHashMap<String, FileMeta> indexMap;
    private final Stats stats;
    private final File poisonPill;
    private final CountDownLatch consumersDone;

    private int indexedCount = 0;

    public Indexer(BlockingQueue<File> queue,
                   ConcurrentHashMap<String, FileMeta> indexMap,
                   Stats stats,
                   File poisonPill,
                   CountDownLatch consumersDone) {
        this.queue = queue;
        this.indexMap = indexMap;
        this.stats = stats;
        this.poisonPill = poisonPill;
        this.consumersDone = consumersDone;
    }

    @Override
    public void run() {
        try {
            while (true) {
                File file = queue.take();

                if (file == poisonPill) {
                    return;
                }

                FileMeta meta = new FileMeta(
                        file.getAbsolutePath(),
                        file.length(),
                        file.lastModified(),
                        getExtension(file)
                );

                indexMap.put(meta.getAbsolutePath(), meta);
                indexedCount++;
                stats.incIndexed();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println(Thread.currentThread().getName() +
                    " indeksirao " + indexedCount + " fajlova");
            if (consumersDone != null) consumersDone.countDown();
        }
    }

    private String getExtension(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot).toLowerCase();
    }
}
