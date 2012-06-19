package filesystem;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

public class FileScannerWorker<E extends Path> implements FileVisitor<E> {

	private final List<E> list = new LinkedList<>();

	@Override
	public FileVisitResult preVisitDirectory(E dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(E file, BasicFileAttributes attrs) throws IOException {
		if (file.toString().endsWith(".mp3")) {
			list.add(file);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(E file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(E dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public List<E> getList() {
		return list;
	}
}
