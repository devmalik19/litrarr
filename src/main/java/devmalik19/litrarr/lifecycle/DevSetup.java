package devmalik19.litrarr.lifecycle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public class DevSetup implements ApplicationListener<ApplicationEnvironmentPreparedEvent>
{
	private static final List<String> SAMPLE_LIBRARY_FILES = List.of(
			"books/Author/Book Title 1/Book 1.epub",
			"books/Author/Book Title 2/Book 2.epub",
			"audiobooks/Author/Book 1/File 1.mp3",
			"audiobooks/Author/Book 1/File 2.mp3",
			"audiobooks/Author/Book 1/File 3.mp3",
			"audiobooks/Author/Book 2/File 1.mp3",
			"audiobooks/Author/Book 2/File 2.mp3",
			"comics/Sample Title/#001/Title.cbz",
			"comics/Sample Title/#002/Title.cbz",
			"comics/Sample Title/#003/Title.cbz",
			"manga/Sample Title/#001/Title.cbz");

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event)
	{
		ConfigurableEnvironment environment = event.getEnvironment();
		if (!environment.acceptsProfiles(Profiles.of("dev")))
			return;

		try
		{
			Path config = Path.of(environment.getRequiredProperty("app.path.config"));
			Path download = Path.of(environment.getRequiredProperty("app.path.download"));
			Path library = Path.of(environment.getRequiredProperty("app.path.library"));

			Files.createDirectories(config);
			Files.createDirectories(download);
			Files.createDirectories(library);

			for (String file : SAMPLE_LIBRARY_FILES)
				createIfMissing(library.resolve(file));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException("Could not create dev data folders", e);
		}
	}

	private void createIfMissing(Path file) throws IOException
	{
		if (Files.exists(file))
			return;
		Files.createDirectories(file.getParent());
		Files.createFile(file);
	}
}
