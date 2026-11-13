package devmalik19.litrarr.service;

import devmalik19.litrarr.helper.SortingHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AudiobookConverterService
{
	private static final Logger logger = LoggerFactory.getLogger(AudiobookConverterService.class);

	private static final Set<String> CONVERTIBLE_EXTENSIONS = Set.of("mp3", "flac", "ogg");
	private static final long PROBE_TIMEOUT_SECONDS = 60;
	private static final long MERGE_TIMEOUT_HOURS = 6;

	private final String configuredFfmpeg;
	private final boolean convertDownloads;
	private String ffmpegCommand;
	private String ffprobeCommand;
	private boolean ffmpegChecked;

	public AudiobookConverterService(@Value("${app.tools.ffmpeg:}") String configuredFfmpeg,
									 @Value("${app.convert.audiobooks:true}") boolean convertDownloads)
	{
		this.configuredFfmpeg = configuredFfmpeg;
		this.convertDownloads = convertDownloads;
	}

	public boolean isEnabledForDownloads()
	{
		return convertDownloads && isAvailable();
	}

	public boolean isAvailable()
	{
		return findFfmpeg() != null;
	}

	public static boolean isConvertible(Path file)
	{
		String extension = StringUtils.getFilenameExtension(file.getFileName().toString());
		return extension != null && CONVERTIBLE_EXTENSIONS.contains(extension.toLowerCase());
	}

	public static List<Path> naturalOrder(List<Path> files)
	{
		List<Path> sorted = new ArrayList<>(files);
		sorted.sort(Comparator.comparing(file -> file.getFileName().toString(), SortingHelper.naturalOrder()));
		return sorted;
	}

	public boolean mergeToM4b(List<Path> files, Path target, String title)
	{
		String ffmpeg = findFfmpeg();
		if (ffmpeg == null || files.size() < 2)
			return false;

		Path directory = target.toAbsolutePath().getParent();
		Path temp = directory.resolve(target.getFileName() + ".tmp");
		Path listFile = null;
		Path metadataFile = null;
		try
		{
			List<Path> ordered = naturalOrder(files);
			List<Long> durations = new ArrayList<>();
			for (Path file : ordered)
				durations.add(probeDurationMillis(file));

			listFile = Files.createTempFile(directory, "litrarr-concat-", ".txt");
			Files.writeString(listFile, buildConcatList(ordered), StandardCharsets.UTF_8);
			metadataFile = Files.createTempFile(directory, "litrarr-chapters-", ".txt");
			Files.writeString(metadataFile, buildChapterMetadata(ordered, durations, title), StandardCharsets.UTF_8);

			run(List.of(ffmpeg, "-y", "-nostdin", "-loglevel", "error",
				"-f", "concat", "-safe", "0", "-i", listFile.toString(),
				"-i", metadataFile.toString(),
				"-map", "0:a", "-map_metadata", "1", "-map_chapters", "1",
				"-vn", "-c:a", "aac", "-b:a", "64k",
				"-movflags", "+faststart", "-f", "ipod", temp.toString()),
				MERGE_TIMEOUT_HOURS * 60, TimeUnit.MINUTES);

			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Merged {} files into '{}'", ordered.size(), target.getFileName());
			return true;
		}
		catch (IOException | RuntimeException e)
		{
			logger.warn("Could not merge files into '{}': {}", target.getFileName(), e.getMessage());
			return false;
		}
		finally
		{
			deleteQuietly(temp);
			deleteQuietly(listFile);
			deleteQuietly(metadataFile);
		}
	}

	private String buildConcatList(List<Path> files)
	{
		StringBuilder builder = new StringBuilder();
		for (Path file : files)
		{
			String path = file.toAbsolutePath().toString().replace('\\', '/').replace("'", "'\\''");
			builder.append("file '").append(path).append("'\n");
		}
		return builder.toString();
	}

	private String buildChapterMetadata(List<Path> files, List<Long> durations, String title)
	{
		StringBuilder builder = new StringBuilder(";FFMETADATA1\n");
		if (StringUtils.hasText(title))
			builder.append("title=").append(escapeMetadata(title)).append('\n');

		long start = 0;
		for (int i = 0; i < files.size(); i++)
		{
			long end = start + durations.get(i);
			String chapter = StringUtils.stripFilenameExtension(files.get(i).getFileName().toString());
			builder.append("[CHAPTER]\nTIMEBASE=1/1000\nSTART=").append(start)
				.append("\nEND=").append(end)
				.append("\ntitle=").append(escapeMetadata(chapter)).append('\n');
			start = end;
		}
		return builder.toString();
	}

	private String escapeMetadata(String value)
	{
		return value.replace("\\", "\\\\").replace("=", "\\=").replace(";", "\\;").replace("#", "\\#").replace("\n", " ");
	}

	private long probeDurationMillis(Path file) throws IOException
	{
		String output = run(List.of(ffprobeCommand, "-v", "error", "-show_entries", "format=duration",
			"-of", "default=noprint_wrappers=1:nokey=1", file.toAbsolutePath().toString()),
			PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		try
		{
			return Math.round(Double.parseDouble(output.trim()) * 1000);
		}
		catch (NumberFormatException e)
		{
			throw new IOException("could not read the duration of " + file.getFileName());
		}
	}

	private String run(List<String> command, long timeout, TimeUnit unit) throws IOException
	{
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		try
		{
			process.getOutputStream().close();
			String output;
			try (InputStream in = process.getInputStream())
			{
				output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
			}
			if (!process.waitFor(timeout, unit))
			{
				process.destroyForcibly();
				throw new IOException(command.get(0) + " timed out");
			}
			if (process.exitValue() != 0)
				throw new IOException(command.get(0) + " failed with exit code " + process.exitValue() + ": " + output);
			return output;
		}
		catch (InterruptedException e)
		{
			process.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while running " + command.get(0), e);
		}
	}

	private synchronized String findFfmpeg()
	{
		if (ffmpegChecked)
			return ffmpegCommand;

		ffmpegChecked = true;
		String candidate = StringUtils.hasText(configuredFfmpeg) ? configuredFfmpeg.trim() : "ffmpeg";
		String probe = siblingProbe(candidate);
		if (works(candidate) && works(probe))
		{
			ffmpegCommand = candidate;
			ffprobeCommand = probe;
			logger.info("ffmpeg found ({}), multi-file audiobooks can be merged into M4B", candidate);
			return ffmpegCommand;
		}
		logger.info("ffmpeg not found, multi-file audiobooks will not be merged into M4B");
		return null;
	}

	private String siblingProbe(String ffmpeg)
	{
		Path path = Path.of(ffmpeg);
		String name = path.getFileName().toString().replaceFirst("(?i)ffmpeg", "ffprobe");
		Path parent = path.getParent();
		return parent == null ? name : parent.resolve(name).toString();
	}

	private boolean works(String command)
	{
		try
		{
			Process process = new ProcessBuilder(command, "-version").redirectErrorStream(true).start();
			process.getOutputStream().close();
			process.getInputStream().readAllBytes();
			if (!process.waitFor(30, TimeUnit.SECONDS))
			{
				process.destroyForcibly();
				return false;
			}
			return process.exitValue() == 0;
		}
		catch (IOException e)
		{
			return false;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void deleteQuietly(Path file)
	{
		if (file == null)
			return;
		try
		{
			Files.deleteIfExists(file);
		}
		catch (IOException e)
		{
			logger.debug("Could not delete {}: {}", file, e.getMessage());
		}
	}
}
