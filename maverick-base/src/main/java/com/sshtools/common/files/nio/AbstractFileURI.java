package com.sshtools.common.files.nio;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AbstractFileURI {

	public static final String URI_SCHEME = "abfs";

	private final String pathInsideVault;

	private AbstractFileURI(URI uri) {
		validate(uri);
		pathInsideVault = uri.getPath();
	}

	public static URI create(String... pathComponentsInsideVault) {
		try {
			return new URI(URI_SCHEME, null, "/" + String.join("/", pathComponentsInsideVault), null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Can not create URI from given input", e);
		}
	}

	static AbstractFileURI parse(URI uri) throws IllegalArgumentException {
		return new AbstractFileURI(uri);
	}

	private static void validate(URI uri) {
		if (!URI_SCHEME.equals(uri.getScheme())) {
			throw new IllegalArgumentException("URI must have " + URI_SCHEME + " scheme");
		}
		if (uri.getAuthority() == null) {
			throw new IllegalArgumentException("URI must have an authority");
		}
		if (uri.getPath() == null || uri.getPath().isEmpty()) {
			throw new IllegalArgumentException("URI must have a path");
		}
		if (uri.getQuery() != null) {
			throw new IllegalArgumentException("URI must not have a query part");
		}
		if (uri.getFragment() != null) {
			throw new IllegalArgumentException("URI must not have a fragment part");
		}
	}

	public String pathInsideVault() {
		return pathInsideVault;
	}

}