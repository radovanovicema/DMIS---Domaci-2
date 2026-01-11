package fileindexer;

import java.io.File;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class FileCrawler implements Runnable {

    private static final long MAX_SIZE = 5 * 1024 * 1024; 
    private static final Set<String> ALLOWED_EXT = Set.of(".txt", ".java", ".md");

    private final BlockingQueue<File> fileQueue;
    private final File root;
    private final Stats stats;
    private final Set<String> visited; 

    public FileCrawler(BlockingQueue<File> fileQueue, File root, Stats stats, Set<String> visited) {
        this.fileQueue = fileQueue;
        this.root = root;
        this.stats = stats;
        this.visited = visited;
    }

    @Override
    public void run() {
        try {
            crawl(root);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void crawl(File dir) throws InterruptedException {
        if (dir == null) return;

        // preskoci hidden direktorijume
        if (dir.isHidden()) {
            stats.incSkipped(SkipReason.HIDDEN);
            return;
        }

        if (!dir.isDirectory()) {
            return;
        }

        File[] entries = dir.listFiles();
        if (entries == null) {
            stats.incSkipped(SkipReason.IO_ERROR);
            return;
        }

        for (File entry : entries) {

            if (entry.isDirectory()) {
                crawl(entry);
                continue;
            }

            if (!entry.isFile()) {
                stats.incSkipped(SkipReason.NOT_A_FILE);
                continue;
            }

            // kandidat fajl
            stats.incFound();

            // hidden fajl
            if (entry.isHidden()) {
                stats.incSkipped(SkipReason.HIDDEN);
                continue;
            }

            // ekstenzija
            String ext = getExtension(entry);
            if (!ALLOWED_EXT.contains(ext)) {
                stats.incSkipped(SkipReason.EXT_NOT_ALLOWED);
                continue;
            }

            // velicina
            if (entry.length() > MAX_SIZE) {
                stats.incSkipped(SkipReason.TOO_LARGE);
                continue;
            }

            String absPath = entry.getAbsolutePath();

            if (!visited.add(absPath)) {
                stats.incSkipped(SkipReason.DUPLICATE);
                continue;
            }

            // ubaci u red
            fileQueue.put(entry);
        }
    }

    private String getExtension(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot).toLowerCase();
    }
}
