package com.github.onlycrab.gbu.connector;

import com.github.onlycrab.gbu.exception.ApiConnectorException;
import com.github.onlycrab.common.ISUtil;
import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Connector to a Gitlab API.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
public class ApiConnector {
    /**
     * Array of trusted certificates.
     */
    private final TrustManager[] trustMrgs;

    /**
     * API response encoding.
     */
    @Setter
    @Getter
    private String encoding = "UTF-8";

    /**
     * Create connector instance witch trust any certificate.
     */
    public ApiConnector(){
        trustMrgs = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
    }

    /**
     * Create connector instance witch trust {@code certificate}.
     *
     * @param certificate Gitlab web certificate
     * @throws ApiConnectorException if exception occurs while trusting certificate
     */
    public ApiConnector(byte[] certificate) throws ApiConnectorException {
        try {
            InputStream is = ISUtil.getInputStream(certificate);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null);
            ks.setCertificateEntry("caCert", caCert);

            factory.init(ks);
            trustMrgs = factory.getTrustManagers();
        } catch (Exception e){
            throw new ApiConnectorException(String.format("Cant trust to certificate : %s.", e.getMessage()));
        }
    }

    /**
     * Open connection to API.
     *
     * @param url target URL
     * @return connection object
     * @throws ApiConnectorException if {@code url} is null;
     *                              if there was a context initiation exception;
     *                              if there was an open connection exception.
     */
    public HttpURLConnection open(URL url) throws ApiConnectorException {
        if (url == null){
            throw new ApiConnectorException("Error at opening connection : URL is <null>.");
        }
        if (url.toString().toLowerCase().startsWith("https")){
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustMrgs, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                throw new ApiConnectorException(String.format("Cant init SSL context : %s.", e.getMessage()));
            }
        }

        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e){
            throw new ApiConnectorException(String.format("Error at opening connection : %s.", e.getMessage()));
        }
        return connection;
    }

    /**
     * Execute API call.
     *
     * @param connection connection to API
     * @param method request method
     * @param timeout timeout in millisecond
     * @param properties request properties
     * @return API response as string
     * @throws ApiConnectorException if {@code connection} is null;
     *                              if {@code method} is invalid;
     *                              if there was {@code java.io.IOException} while reading API response
     */
    public String execute(HttpURLConnection connection, RequestMethod method, int timeout, Properties properties) throws ApiConnectorException {
        if (connection == null){
            throw new ApiConnectorException("Connection is <null>.");
        }

        try {
            connection.setRequestMethod(method.getCode());
        } catch (ProtocolException e) {
            throw new ApiConnectorException(String.format("Error at set request method : %s.", e.getMessage()));
        }
        timeout = timeout * 1000;
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        if (properties != null) {
            for (Object key : properties.keySet()) {
                connection.setRequestProperty(key.toString(), properties.get(key).toString());
            }
        }

        try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding))) {
            String input;
            final StringBuilder content = new StringBuilder();
            while ((input = in.readLine()) != null) {
                content.append(input);
            }
            return content.toString();
        } catch (IOException e){
            throw new ApiConnectorException(String.format("Error at reading response : %s.", e.getMessage()));
        }
    }

    /**
     * Open connection and execute API call.
     *
     * @param url target URL
     * @param method request method
     * @param timeout timeout in millisecond
     * @param properties request properties
     * @return API response as string
     * @throws ApiConnectorException if there was {@code ApiConnectorException} while opening connection to API;
     *                              if {@code connection} is null;
     *                              if {@code method} is invalid;
     *                              if there was {@link java.io.IOException} while reading API response
     */
    public String execute(URL url, RequestMethod method, int timeout, Properties properties) throws ApiConnectorException {
        return execute(open(url), method, timeout, properties);
    }
}
