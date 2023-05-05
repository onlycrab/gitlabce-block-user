package com.github.onlycrab.gbu.connector;

import com.github.onlycrab.gbu.connector.ApiConnector;
import com.github.onlycrab.gbu.connector.RequestMethod;
import com.github.onlycrab.gbu.exception.ApiConnectorException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * {@link ApiConnector} test class.
 */
@SuppressWarnings("Duplicates")
public class ApiConnectorTest {
    /**
     * {@link ApiConnector#open(URL)}.
     */
    @Test
    public void open() {
        try {
            ApiConnector connector = new ApiConnector();
            Assert.assertThrows(ApiConnectorException.class, () -> connector.open(null));

            connector.open(new URL("http://localhost/pointNotExist"));
        } catch (MalformedURLException | ApiConnectorException e){
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link ApiConnector#execute(HttpURLConnection, RequestMethod, int, Properties)}.
     */
    @Test
    public void executeConnection() {
        ApiConnector connector = new ApiConnector();

        URL url = Mockito.mock(URL.class);
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);

        try {
            Mockito.when(url.openConnection()).thenReturn(connection);
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("[]".getBytes()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        try {
            connector.execute(connection, RequestMethod.GET, 0, null);
            connector.execute(connection, RequestMethod.POST, 1, null);
            connector.execute(connection, RequestMethod.GET, 1000, new Properties());
            Properties props = new Properties();
            props.put("prop1", "val1");
            props.put("prop2", "val2");
            connector.execute(connection, RequestMethod.GET, 1000, props);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link ApiConnector#execute(URL, RequestMethod, int, Properties)} ;
     */
    @Test
    public void executeUrl(){
        ApiConnector connector = new ApiConnector();

        URL url = Mockito.mock(URL.class);
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        try {
            Mockito.when(connector.open(url)).thenReturn(connection);
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("[]".getBytes()));
        } catch (ApiConnectorException | IOException e) {
            Assert.fail(e.getMessage());
        }

        try {
            connector.execute(url, RequestMethod.GET, 0, null);
            connector.execute(url, RequestMethod.POST, 1, null);
            connector.execute(url, RequestMethod.GET, 1000, new Properties());
            Properties props = new Properties();
            props.put("prop1", "val1");
            props.put("prop2", "val2");
            connector.execute(url, RequestMethod.GET, 1000, props);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
        }
    }
}