package stolat.bootstrap;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import stolat.bootstrap.cli.BootstrapCommand;

import java.util.List;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.fieldIn;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(args = "")
@AutoConfigureEmbeddedDatabase
@TestPropertySource("classpath:test-application.properties")
public class StolatBootstrapApplicationNoArgsTest {

    @Autowired
    private BootstrapCommand command;

    @Autowired
    private StoLatBootstrapApplication application;

    @Test
    void shouldUpdateAlbumBirthdayDatabaseWhenBirthdayOptionSelected() throws Exception {
        waitForExecutorsToFinish();
        assertFalse(command.albumBirthday);
        assertFalse(command.albumCollection);
        assertFalse(command.truncate);
        assertNull(command.path);

    }

    private void waitForExecutorsToFinish() {
        await().until(() -> {
            final List<Future> futures = fieldIn(command).ofType(List.class).andWithName("futures").call();
            return futures.stream().allMatch(Future::isDone);
        });
    }
}
