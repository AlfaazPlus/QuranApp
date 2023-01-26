package com.quranapp.android.utils.univ;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class URLBuilder {
    public static final String CONNECTION_TYPE_HTTP = "http";
    public static final String CONNECTION_TYPE_HTTPS = "https";
    private final StringBuilder paths;
    private final StringBuilder params;
    private String connType, host;

    public URLBuilder() {
        paths = new StringBuilder();
        params = new StringBuilder();
    }

    public URLBuilder(@NonNull String host) {
        this();
        this.host = host;
    }

    @NonNull
    public URLBuilder setConnectionType(@NonNull String conn) {
        connType = conn;
        return this;
    }

    @NonNull
    public URLBuilder addPath(@NonNull String path) {
        if (paths.toString().endsWith("/") && path.startsWith("/")) path = path.substring(1);
        if (!paths.toString().endsWith("/") && !path.startsWith("/")) paths.append("/");
        paths.append(path);
        return this;
    }

    /**
     * @param parameter Query parameter
     * @param value     Query value
     * @return {@link URLBuilder}
     */
    @NonNull
    public URLBuilder addParam(@NonNull String parameter, @NonNull String value) {
        if (params.toString().length() > 0) {
            params.append("&");
        }
        params.append(parameter);
        params.append("=");
        params.append(value);
        return this;
    }

    /**
     * @return {@link String} url
     * @throws URISyntaxException    URISyntaxException
     * @throws MalformedURLException MalformedURLException
     */
    @NonNull
    public String getURLString() throws URISyntaxException, MalformedURLException, IllegalArgumentException {
        URI uri = new URI(connType, host, paths.toString(), params.toString(), null);
        return uri.toURL().toString();
    }

    @NonNull
    public String getRelativeURLString() throws URISyntaxException {
        URI uri = new URI(null, null, paths.toString(), params.toString(), null);
        return uri.toString();
    }
}