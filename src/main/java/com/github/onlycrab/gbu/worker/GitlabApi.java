package com.github.onlycrab.gbu.worker;

import com.github.onlycrab.common.StringUtil;
import com.github.onlycrab.gbu.connector.ApiConnector;
import com.github.onlycrab.gbu.connector.RequestMethod;
import com.github.onlycrab.gbu.exception.ApiConnectorException;
import com.github.onlycrab.gbu.exception.JsonConverterException;
import com.github.onlycrab.gbu.model.ErrorResponse;
import com.github.onlycrab.gbu.model.User;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class for interaction with Gitlab API.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
@Getter
public class GitlabApi {
    /**
     * String template for getting a list of users.
     */
    public static final String TEMPLATE_USERS = "%s/api/v4/users?per_page=50&page=%s&private_token=%s";
    /**
     * String template for block a user.
     */
    public static final String TEMPLATE_BLOCK = "%s/api/v4/users/%s/block?private_token=%s";
    /**
     * String template for unblock a user.
     */
    public static final String TEMPLATE_UNBLOCK = "%s/api/v4/users/%s/unblock?private_token=%s";
    /**
     * Gitlab URL.
     */
    private String address;
    /**
     * Gitlab token with permission to modify users.
     */
    private String token;
    /**
     * Object for connect and execute API requests.
     */
    private ApiConnector connector;
    /**
     * Object to convert JSON objects.
     */
    private JsonConverter converter;
    /**
     * Timeout for API response.
     */
    private int timeout = 30000;

    public void setConnector(ApiConnector connector){
        if (connector != null){
            this.connector = connector;
        }
    }

    public void setConverter(JsonConverter converter){
        if (converter != null){
            this.converter = converter;
        }
    }

    /**
     * Set Gitlab root address.
     *
     * @param address Gitlab root address (f.e. https://gitlab.mycompany.com)
     */
    public void setAddress(String address){
        if (address != null){
            this.address = address;
        }
    }

    public void setToken(String token){
        if (token != null){
            this.token = token;
        }
    }

    /**
     * Set timeout for API response.
     *
     * @param timeout value as milliseconds
     */
    public void setTimeout(int timeout){
        if (timeout > 0){
            this.timeout = timeout * 1000;
        }
    }

    /**
     * Initialize.
     *
     * @param address Gitlab root address (f.e. https://gitlab.mycompany.com)
     * @param token token with permission to modify users
     */
    public GitlabApi(String address, String token) {
        this(address, token, new ApiConnector(), new JsonConverter());
    }

    /**
     * Initialize.
     *
     * @param address Gitlab root address (f.e. https://gitlab.mycompany.com)
     * @param token token with permission to modify users
     * @param certificate Gitlab web certificate
     * @throws ApiConnectorException if exception occurs while trusting certificate
     */
    public GitlabApi(String address, String token, byte[] certificate) throws ApiConnectorException {
        this(address, token, new ApiConnector(certificate), new JsonConverter());
    }

    /**
     * Initialize.
     *
     * @param address Gitlab root address (f.e. https://gitlab.mycompany.com)
     * @param token token with permission to modify users
     * @param connector connector to API
     * @param converter JSON objects converter
     */
    @SuppressWarnings("WeakerAccess")
    public GitlabApi(String address, String token, ApiConnector connector, JsonConverter converter){
        if (address != null){
            if (address.endsWith("/")){
                this.address = address.substring(0, address.length() - 2);
            } else {
                this.address = address;
            }
        } else {
            this.address = "";
        }
        if (token != null){
            this.token = token;
        } else {
            this.token = "";
        }
        if (connector != null){
            this.connector = connector;
        } else {
            this.connector = new ApiConnector();
        }
        if (converter != null){
            this.converter = converter;
        } else {
            this.converter = new JsonConverter();
        }
    }

    /**
     * Build URL from template.
     *
     * @param template API call address template
     * @param address Gitlab root address
     * @param number page number
     * @param token access token
     * @return API call URL
     * @throws ApiConnectorException if built URL is incorrect
     *
     * @see GitlabApi#TEMPLATE_USERS
     * @see GitlabApi#TEMPLATE_BLOCK
     * @see GitlabApi#TEMPLATE_UNBLOCK
     */
    protected URL getUrl(String template, String address, long number, String token) throws ApiConnectorException {
        try {
            return new URL(String.format(template, address, number, token));
        } catch (MalformedURLException e) {
            throw new ApiConnectorException(String.format("Bad URL : %s.", e.getMessage()));
        }
    }

    /**
     * Union two arrays.
     *
     * @param arr1 first array
     * @param arr2 second array
     * @return array that contains all elements of {@code arr1} and {@code arr2}
     */
    protected User[] union(User[] arr1, User[] arr2){
        if (arr1 == null && arr2 == null){
            return new User[0];
        } else if (arr2 == null){
            return arr1;
        } else if (arr1 == null){
            return arr2;
        }
        User[] newArr = new User[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, newArr, 0, arr1.length);
        System.arraycopy(arr2, 0, newArr, arr1.length, arr2.length);
        return newArr;
    }

    /**
     * Get all users by API.
     *
     * @return list of all Gitlab users
     * @throws ApiConnectorException if exception occurs during communicating with API
     * @throws JsonConverterException if the API response cannot be converted to an array {@link User}
     */
    public User[] getAllGitUsers() throws ApiConnectorException, JsonConverterException {
        URL url;
        User[] users = new User[0];
        User[] parsed;
        String response;

        int page = 0;
        do {
            page++;
            url = getUrl(TEMPLATE_USERS, address, page, token);
            response = connector.execute(url, RequestMethod.GET, timeout, null);

            try {
                parsed = converter.fromJson(response);
            } catch (RuntimeException e){
                throw new JsonConverterException(String.format("Cant parse Gitlab API response to JSON : %s.", e.getMessage()));
            }
            if (parsed.length == 0){
                break;
            }

            if (users.length == 0){
                users = parsed;
            } else {
                users = union(users, parsed);
            }
        } while (true);

        return users;
    }

    /**
     * Block Gitlab user.
     *
     * @param id user ID
     * @throws ApiConnectorException if exception occurs during communicating with API
     */
    public void blockUser(long id) throws ApiConnectorException {
        changeUserState(id, true);
    }

    /**
     * Unblock Gitlab user.
     *
     * @param id user ID
     * @throws ApiConnectorException if exception occurs during communicating with API
     */
    public void unblockUser(long id) throws ApiConnectorException {
        changeUserState(id, false);
    }

    /**
     * Change Gitlab user state.
     *
     * @param id user ID
     * @param block if {@code true} - user will be blocked, otherwise - user will be unblocked
     * @throws ApiConnectorException if exception occurs during communicating with API
     */
    protected void changeUserState(long id, boolean block) throws ApiConnectorException {
        URL url;

        if (block) {
            url = getUrl(TEMPLATE_BLOCK, address, id, token);
        } else {
            url = getUrl(TEMPLATE_UNBLOCK, address, id, token);
        }

        String response = connector.execute(url, RequestMethod.POST, timeout, null);
        if (!response.equals("true")) {
            if (response.equals("null") || response.equals("false")){
                if (block){
                    throw new ApiConnectorException(String.format("User <%s> already blocked.", id));
                } else {
                    throw new ApiConnectorException(String.format("User <%s> already unblocked.", id));
                }
            }
            try {
                ErrorResponse errorResponse = converter.getError(response);
                if (errorResponse != null) {
                    if (!StringUtil.isEmptyOrNull(errorResponse.getErrorDescription())) {
                        throw new ApiConnectorException(errorResponse.getErrorDescription());
                    } else if (!StringUtil.isEmptyOrNull(errorResponse.getError())) {
                        throw new ApiConnectorException(errorResponse.getError());
                    }
                }
            } catch (RuntimeException e) {
                throw new ApiConnectorException(String.format("Cant parse API response <%s> : %s.", response, e.getMessage()));
            }
        }
    }
}
