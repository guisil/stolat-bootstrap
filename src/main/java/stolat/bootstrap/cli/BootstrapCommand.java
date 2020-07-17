package stolat.bootstrap.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Component
@Slf4j
@Command(
        name = "stolat-bootstrap",
        description = "Bootstraps the DB for StoLat",
        mixinStandardHelpOptions = true,
        version = "0.1"
)
public class BootstrapCommand implements Callable<Integer> {

    @Option(names = {"-b", "--album-birthday"}, description = "populate the album birthday database structures")
    public boolean albumBirthday;

    @Option(names = {"-c", "--album-collection"}, description = "populate the album collection database structures")
    public boolean albumCollection;

    @Option(names = {"-t", "--truncate"}, description = "truncate table(s) before starting")
    public boolean truncate;

    @Option(names = {"-f", "--force"}, description = "forces an update when an album/track entry already exists")
    public boolean force;

    @Option(names = {"-p", "--path"}, description = "overrides the path where the album collection is to be fetched")
    public Path path;

    @Option(names = "--spring.config.location", hidden = true)
    private String springConfigLocation;

//    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
//    private boolean help;

    @Autowired
    private AlbumBirthdayCommand albumBirthdayCommand;

    @Autowired
    private AlbumCollectionCommand albumCollectionCommand;

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();

//    private ExecutorService albumBirthdayExecutor = Executors.newSingleThreadExecutor();
//    private ExecutorService albumCollectionExecutor = Executors.newSingleThreadExecutor();

    @Override
    public Integer call() {

        final AtomicInteger exitCode = new AtomicInteger(0);

        if (albumBirthday) {
            triggerAlbumBirthdayUpdate();
        }
        if (albumCollection) {
            triggerAlbumCollectionUpdate();
        }
        if (!albumBirthday && !albumCollection) {
            triggerAlbumBirthdayUpdate();
            triggerAlbumCollectionUpdate();
        }

        CompletableFuture
                .allOf(futures.stream().toArray(CompletableFuture[]::new))
                .exceptionallyAsync(ex -> {
                    log.error("Error during thread execution", ex);
                    exitCode.set(1);
                    return null;
                });

        return exitCode.get();
    }

    private void triggerAlbumBirthdayUpdate() {
        log.debug("Triggered option to update album birthdays.");
        final Runnable updateAlbumBirthdayDatabase = albumBirthdayCommand::updateAlbumBirthdayDatabase;
        futures.add(CompletableFuture.runAsync(updateAlbumBirthdayDatabase));
//        futures.add(albumBirthdayExecutor.submit(albumBirthdayCommand::updateAlbumBirthdayDatabase));
    }

    private void triggerAlbumCollectionUpdate() {
        final String truncateAnd = truncate ? "truncate and " : "";
        final String forceUpdate = force ? "force update" : "update";
        final Runnable updateAlbumCollectionDatabase;
        if (path != null) {
            log.debug("Triggered option to {}{} album collection from path {}.", truncateAnd, forceUpdate, path);
            updateAlbumCollectionDatabase = () -> albumCollectionCommand.updateAlbumCollectionDatabase(truncate, path, force);
//            futures.add(albumCollectionExecutor.submit(() -> albumCollectionCommand.updateAlbumCollectionDatabase(truncate, path, force)));
        } else {
            log.debug("Triggered option to {}{} album collection from root path.", truncateAnd, forceUpdate);
            updateAlbumCollectionDatabase = () -> albumCollectionCommand.updateAlbumCollectionDatabase(truncate, force);
//            futures.add(albumCollectionExecutor.submit(() -> albumCollectionCommand.updateAlbumCollectionDatabase(truncate, force)));
        }
        futures.add(CompletableFuture.runAsync(updateAlbumCollectionDatabase));
    }
}
