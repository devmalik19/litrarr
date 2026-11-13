package devmalik19.litrarr.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ComicConverterService
{
	private static final Logger logger = LoggerFactory.getLogger(ComicConverterService.class);

	private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
	private static final byte[] RAR_SIGNATURE = {'R', 'a', 'r', '!', 0x1A, 0x07};
	private static final byte[] SEVEN_ZIP_SIGNATURE = {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
	private static final int SIGNATURE_LENGTH = 6;
	private static final List<String> SEVEN_ZIP_COMMANDS = List.of("7zz", "7z", "7za");
	private static final long SEVEN_ZIP_TIMEOUT_MINUTES = 10;

	private final String configuredSevenZip;
	private String sevenZipCommand;
	private boolean sevenZipChecked;

	public ComicConverterService(@Value("${app.tools.sevenzip:}") String configuredSevenZip)
	{
		this.configuredSevenZip = configuredSevenZip;
	}

	public Path toCbz(Path file)
	{
		String name = file.getFileName().toString();
		String extension = StringUtils.getFilenameExtension(name);
		if (!"cbr".equalsIgnoreCase(extension))
			return file;

		Path target = file.resolveSibling(name.substring(0, name.length() - extension.length()) + "cbz");
		Path temp = file.resolveSibling(target.getFileName() + ".tmp");
		try
		{
			byte[] header = readHeader(file, SIGNATURE_LENGTH);
			if (startsWith(header, ZIP_SIGNATURE))
			{
				Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
				logger.info("Renamed '{}' to '{}' (it is a ZIP archive)", name, target.getFileName());
				return target;
			}

			if (!startsWith(header, RAR_SIGNATURE) && !startsWith(header, SEVEN_ZIP_SIGNATURE))
			{
				logger.warn("Not converting '{}': it is not a RAR, 7z or ZIP archive", name);
				return file;
			}

			String command = findSevenZip();
			if (command == null)
			{
				logger.warn("Not converting '{}': 7-Zip is not installed", name);
				return file;
			}

			sevenZipToZip(command, file, temp);
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			Files.delete(file);
			logger.info("Converted '{}' to '{}'", name, target.getFileName());
			return target;
		}
		catch (IOException | RuntimeException e)
		{
			logger.warn("Could not convert '{}' to CBZ, keeping the original: {}", name, e.getMessage());
			deleteQuietly(temp);
			return file;
		}
	}

	private void sevenZipToZip(String command, Path archive, Path zip) throws IOException
	{
		Path extractDir = Files.createTempDirectory(archive.toAbsolutePath().getParent(), "litrarr-extract-");
		try
		{
			runSevenZip(command, archive, extractDir);
			zipDirectory(extractDir, zip);
		}
		finally
		{
			deleteRecursively(extractDir);
		}
	}

	private void runSevenZip(String command, Path archive, Path extractDir) throws IOException
	{

		ProcessBuilder builder = new ProcessBuilder(command, "x", "-y", "-bd", "-bso0",
			"-o" + extractDir.toAbsolutePath(), "--", archive.toAbsolutePath().toString());
		builder.redirectErrorStream(true);
		Process process = builder.start();
		try
		{

			process.getOutputStream().close();
			String output;
			try (InputStream in = process.getInputStream())
			{
				output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
			}
			if (!process.waitFor(SEVEN_ZIP_TIMEOUT_MINUTES, TimeUnit.MINUTES))
			{
				process.destroyForcibly();
				throw new IOException("7-Zip timed out");
			}
			if (process.exitValue() != 0)
				throw new IOException("7-Zip failed with exit code " + process.exitValue() + ": " + output);
		}
		catch (InterruptedException e)
		{
			process.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while running 7-Zip", e);
		}
	}

	private void zipDirectory(Path directory, Path zip) throws IOException
	{
		int added = 0;
		List<Path> files;
		try (Stream<Path> walk = Files.walk(directory))
		{
			files = walk.filter(Files::isRegularFile).sorted().toList();
		}

		try (OutputStream out = Files.newOutputStream(zip);
			 ZipOutputStream zipOut = new ZipOutputStream(out))
		{
			for (Path file : files)
			{
				String entryName = safeEntryName(directory.relativize(file).toString());
				if (entryName == null)
					continue;
				zipOut.putNextEntry(new ZipEntry(entryName));
				Files.copy(file, zipOut);
				zipOut.closeEntry();
				added++;
			}
		}

		if (added == 0)
			throw new IOException("no files could be extracted from the archive");
	}

	private synchronized String findSevenZip()
	{
		if (sevenZipChecked)
			return sevenZipCommand;

		sevenZipChecked = true;
		List<String> candidates = StringUtils.hasText(configuredSevenZip)
			? List.of(configuredSevenZip.trim())
			: SEVEN_ZIP_COMMANDS;
		for (String candidate : candidates)
		{
			if (works(candidate))
			{
				sevenZipCommand = candidate;
				logger.info("7-Zip found ({}), RAR and 7z comics can be converted to CBZ", candidate);
				return sevenZipCommand;
			}
		}
		logger.info("7-Zip not found, RAR and 7z comics will not be converted to CBZ");
		return null;
	}

	private boolean works(String command)
	{
		try
		{
			Process process = new ProcessBuilder(command, "i").redirectErrorStream(true).start();
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

	private String safeEntryName(String name)
	{
		if (name == null)
			return null;
		String normalized = name.replace('\\', '/');
		if (normalized.isBlank() || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*"))
			return null;
		for (String part : normalized.split("/"))
		{
			if (part.equals(".."))
				return null;
		}
		return normalized;
	}

	private byte[] readHeader(Path file, int length) throws IOException
	{
		try (InputStream in = Files.newInputStream(file))
		{
			return in.readNBytes(length);
		}
	}

	private boolean startsWith(byte[] data, byte[] signature)
	{
		if (data.length < signature.length)
			return false;
		for (int i = 0; i < signature.length; i++)
		{
			if (data[i] != signature[i])
				return false;
		}
		return true;
	}

	private void deleteQuietly(Path file)
	{
		try
		{
			Files.deleteIfExists(file);
		}
		catch (IOException e)
		{
			logger.debug("Could not delete {}: {}", file, e.getMessage());
		}
	}

	private void deleteRecursively(Path directory)
	{
		try (Stream<Path> walk = Files.walk(directory))
		{
			walk.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
		}
		catch (IOException e)
		{
			logger.debug("Could not clean up {}: {}", directory, e.getMessage());
		}
	}
}
