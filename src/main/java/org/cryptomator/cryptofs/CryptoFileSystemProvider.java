/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cryptofs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class CryptoFileSystemProvider extends FileSystemProvider {

	/**
	 * example: cryptomator:///Path/to/vault#/path/inside/vault
	 */
	private static final String URI_SCHEME = "cryptomator";

	final ConcurrentHashMap<Path, CryptoFileSystem> fileSystems = new ConcurrentHashMap<>();

	@Override
	public String getScheme() {
		return URI_SCHEME;
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		Path pathToVault = FileSystems.getDefault().getPath(uri.getPath());
		return fileSystems.compute(pathToVault, (key, value) -> {
			if (value == null) {
				return new CryptoFileSystem(this, key);
			} else {
				throw new FileSystemAlreadyExistsException();
			}
		});
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		Path pathToVault = FileSystems.getDefault().getPath(uri.getPath());
		return fileSystems.computeIfAbsent(pathToVault, key -> {
			throw new FileSystemNotFoundException();
		});
	}

	@Override
	public Path getPath(URI uri) {
		Path pathToVault = FileSystems.getDefault().getPath(uri.getPath());
		CryptoFileSystem fs = fileSystems.computeIfAbsent(pathToVault, key -> {
			return new CryptoFileSystem(this, key);
		});
		return fs.getPath(uri.getFragment());
	}

	@Override
	public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
		return new AsyncDelegatingFileChannel(newFileChannel(path, options, attrs), executor);
	}

	@Override
	public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		return newFileChannel(path, options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Path path) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		return path.equals(path2);
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		FileStore store = getFileStore(path);
		if (store.supportsFileAttributeView(DosFileAttributeView.class)) {
			DosFileAttributeView view = this.getFileAttributeView(path, DosFileAttributeView.class);
			return view.readAttributes().isHidden();
		} else {
			return false;
		}
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return BasicPath.cast(path).getFileSystem().getFileStore();
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		// TODO check if file exists first and throw NoSuchFileException, if it doesn't.
		FileStore store = getFileStore(path);
		if (store.supportsFileAttributeView(PosixFileAttributeView.class)) {
			PosixFileAttributeView view = this.getFileAttributeView(path, PosixFileAttributeView.class);
			Set<PosixFilePermission> permissions = view.readAttributes().permissions();
			boolean accessGranted = true;
			for (AccessMode accessMode : modes) {
				switch (accessMode) {
				case READ:
					accessGranted &= permissions.contains(PosixFilePermission.OWNER_READ);
					break;
				case WRITE:
					accessGranted &= permissions.contains(PosixFilePermission.OWNER_WRITE);
					break;
				case EXECUTE:
					accessGranted &= permissions.contains(PosixFilePermission.OWNER_EXECUTE);
					break;
				default:
					throw new UnsupportedOperationException("AccessMode " + accessMode + " not supported.");
				}
			}
			if (!accessGranted) {
				throw new AccessDeniedException(path.toString());
			}
		}
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		// TODO Auto-generated method stub

	}

}