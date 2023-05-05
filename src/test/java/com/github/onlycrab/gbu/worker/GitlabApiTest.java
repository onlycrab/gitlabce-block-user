package com.github.onlycrab.gbu.worker;

import com.github.onlycrab.gbu.connector.ApiConnector;
import com.github.onlycrab.gbu.connector.RequestMethod;
import com.github.onlycrab.gbu.exception.ApiConnectorException;
import com.github.onlycrab.gbu.exception.JsonConverterException;
import com.github.onlycrab.gbu.model.Identity;
import com.github.onlycrab.gbu.model.User;
import com.github.onlycrab.gbu.worker.GitlabApi;
import com.github.onlycrab.gbu.worker.JsonConverter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;

/**
 * {@link GitlabApi} test class.
 */
public class GitlabApiTest {
    /**
     * {@link GitlabApi#setTimeout(int)}.
     */
    @Test
    public void setTimeout() {
        GitlabApi api = new GitlabApi(null, null);

        int timeout = 10;
        api.setTimeout(timeout);
        Assert.assertEquals(timeout * 1000, api.getTimeout());

        timeout = api.getTimeout();
        api.setTimeout(-1);
        Assert.assertEquals(timeout, api.getTimeout());
    }

    /**
     * {@link GitlabApi#getUrl(String, String, long, String)}.
     */
    @Test
    public void getUrl() {
        GitlabApi api = new GitlabApi(null, null);
        String address = "address";
        int page = 1;
        String token = "token";

        try {
            api.getUrl(GitlabApi.TEMPLATE_USERS, address, page, token);
            Assert.fail("ApiConnectorException expected, but nothing was thrown");
        } catch (ApiConnectorException ignore){ }

        try {
            api.getUrl(GitlabApi.TEMPLATE_USERS, null, page, token);
            Assert.fail("ApiConnectorException expected, but nothing was thrown");
        } catch (ApiConnectorException ignore){ }

        address = "http://localhost/pointNotExist";
        try {
            Assert.assertEquals(
                    new URL(String.format(GitlabApi.TEMPLATE_USERS, address, page, token)),
                    api.getUrl(GitlabApi.TEMPLATE_USERS, address, page, token)
            );
            Assert.assertEquals(
                    new URL(String.format(GitlabApi.TEMPLATE_BLOCK, address, page, token)),
                    api.getUrl(GitlabApi.TEMPLATE_BLOCK, address, page, token)
            );
            Assert.assertEquals(
                    new URL(String.format(GitlabApi.TEMPLATE_UNBLOCK, address, page, token)),
                    api.getUrl(GitlabApi.TEMPLATE_UNBLOCK, address, page, token)
            );
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }

        try {
            Assert.assertEquals(
                    new URL(String.format(GitlabApi.TEMPLATE_USERS, address, page, null)),
                    api.getUrl(GitlabApi.TEMPLATE_USERS, address, page, null)
            );
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link GitlabApi#union(User[], User[])}.
     */
    @Test
    public void union() {
        GitlabApi api = new GitlabApi(null, null);
        User user1 = new User(1, "1", null, null);
        User user2 = new User(2, "2", null, null);
        User user3 = new User(3, "3", null, null);
        User user4 = new User(4, "4", null, null);
        User user5 = new User(5, "5", null, null);
        User[] arr1 = new User[]{ user1, user2 };
        User[] arr2 = new User[]{ user3, user4, user5 };
        User[] arr3 = new User[]{ user1, user2, user3, user4, user5 };

        Assert.assertArrayEquals(new User[0], api.union(null, null));
        Assert.assertArrayEquals(arr1, api.union(arr1, null));
        Assert.assertArrayEquals(arr1, api.union(new User[0], arr1));
        Assert.assertArrayEquals(arr3, api.union(arr1, arr2));
    }

    /**
     * {@link GitlabApi#getAllGitUsers()}.
     */
    @Test
    public void getAllGitUsers() {
        String address = "http://localhost/pointNotExist";
        String token = "token";
        ApiConnector connector = Mockito.mock(ApiConnector.class);
        URL urlPage1 = Mockito.mock(URL.class);
        URL urlPage2 = Mockito.mock(URL.class);
        URL urlPage3 = Mockito.mock(URL.class);
        String response1 = "[ " +
                "{ \"id\": 1, \"username\": \"user1\", \"name\": \"user1\", \"state\": \"active\", \"identities\": []}, " +
                "{ \"id\": 2, \"username\": \"user2\", \"name\": \"user2\", \"state\": \"blocked_pending_approval\", " +
                "   \"identities\": [{ \"provider\": \"ldapmain\", \"extern_uid\": \"cn=user2,ou=users,ou=mc,dc=my,dc=company\" }]" +
                "} " +
                "]";
        String response2 = "[ " +
                "{ \"id\": 3, \"username\": \"user3\", \"name\": \"user3\", \"state\": \"active\", \"identities\": []} " +
                "]";
        String response3 = "[]";
        GitlabApi api = Mockito.mock(GitlabApi.class);
        JsonConverter converter = new JsonConverter();
        try {
            Mockito.when(api.getTimeout()).thenCallRealMethod();
            Mockito.doCallRealMethod().when(api).setConnector(connector);
            Mockito.doCallRealMethod().when(api).setConverter(converter);
            Mockito.doCallRealMethod().when(api).setAddress(address);
            Mockito.doCallRealMethod().when(api).setToken(token);

            Mockito.when(api.union(Mockito.any(User[].class), Mockito.any(User[].class))).thenCallRealMethod();

            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_USERS, address, 1, token)).thenReturn(urlPage1);
            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_USERS, address, 2, token)).thenReturn(urlPage2);
            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_USERS, address, 3, token)).thenReturn(urlPage3);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
            return;
        }

        try {
            Mockito.when(connector.execute(urlPage1, RequestMethod.GET, api.getTimeout(), null)).thenReturn(response1);
            Mockito.when(connector.execute(urlPage2, RequestMethod.GET, api.getTimeout(), null)).thenReturn(response2);
            Mockito.when(connector.execute(urlPage3, RequestMethod.GET, api.getTimeout(), null)).thenReturn(response3);

            api.setConnector(connector);
            api.setConverter(converter);
            api.setAddress(address);
            api.setToken(token);
            Mockito.when(api.getAllGitUsers()).thenCallRealMethod();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            return;
        }

        User[] expected = new User[]{
                new User(1, "user1", "active", new Identity[]{}),
                new User(2, "user2", "blocked_pending_approval", new Identity[] { new Identity("ldapmain", "cn=user2,ou=users,ou=mc,dc=my,dc=company")}),
                new User(3, "user3", "active", new Identity[]{}),
        };
        User[] actual;
        try {
            actual = api.getAllGitUsers();
        } catch (ApiConnectorException | JsonConverterException e) {
            Assert.fail(e.getMessage());
            return;
        }

        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * {@link GitlabApi#changeUserState(long, boolean)}.
     */
    @Test
    public void changeUserState() {
        String address = "http://localhost/pointNotExist";
        String token1 = "token";
        String token2 = "token2";
        int userId = 1;

        ApiConnector connector = Mockito.mock(ApiConnector.class);
        URL urlPage1 = Mockito.mock(URL.class);
        URL urlPage2 = Mockito.mock(URL.class);
        URL urlPage3 = Mockito.mock(URL.class);
        URL urlPage4 = Mockito.mock(URL.class);

        String response1 = "true";
        String response2 = "null";
        String response3 = "false";
        String response4 = "{" +
                "\"error\":\"insufficient_scope\"," +
                "\"error_description\":\"The request requires higher privileges than provided by the access token.\"" +
                "}";

        GitlabApi api = Mockito.mock(GitlabApi.class);
        JsonConverter converter = new JsonConverter();

        try {
            Mockito.when(api.getTimeout()).thenCallRealMethod();
            Mockito.doCallRealMethod().when(api).changeUserState(userId, true);
            Mockito.doCallRealMethod().when(api).changeUserState(userId, false);
            Mockito.doCallRealMethod().when(api).setConnector(connector);
            Mockito.doCallRealMethod().when(api).setConverter(converter);
            Mockito.doCallRealMethod().when(api).setAddress(address);
            Mockito.doCallRealMethod().when(api).setToken(token1);
            Mockito.doCallRealMethod().when(api).setToken(token2);

            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_BLOCK, address, userId, token1)).thenReturn(urlPage1);
            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_BLOCK, address, userId, token2)).thenReturn(urlPage2);
            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_UNBLOCK, address, userId, token1)).thenReturn(urlPage3);
            Mockito.when(api.getUrl(GitlabApi.TEMPLATE_UNBLOCK, address, userId, token2)).thenReturn(urlPage4);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
            return;
        }

        try {
            Mockito.when(connector.execute(urlPage1, RequestMethod.POST, api.getTimeout(), null)).thenReturn(response1);
            Mockito.when(connector.execute(urlPage2, RequestMethod.POST, api.getTimeout(), null)).thenReturn(response2);
            Mockito.when(connector.execute(urlPage3, RequestMethod.POST, api.getTimeout(), null)).thenReturn(response3);
            Mockito.when(connector.execute(urlPage4, RequestMethod.POST, api.getTimeout(), null)).thenReturn(response4);

            api.setConnector(connector);
            api.setConverter(converter);
            api.setAddress(address);
            Mockito.when(api.getAllGitUsers()).thenCallRealMethod();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            return;
        }


        api.setToken(token1);
        try {
            api.changeUserState(userId, true);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
        }

        api.setToken(token2);
        try {
            api.changeUserState(userId, true);
            Assert.fail("ApiConnectorException expected, but nothing thrown : must be requires higher privileges.");
        } catch (ApiConnectorException e) {
            Assert.assertEquals(String.format("User <%s> already blocked.", userId), e.getMessage());
        }

        api.setToken(token1);
        try {
            api.changeUserState(userId, false);
            Assert.fail("ApiConnectorException expected, but nothing thrown : user already unblocked and cant be unblocked again.");
        } catch (ApiConnectorException e) {
            Assert.assertEquals(String.format("User <%s> already unblocked.", userId), e.getMessage());
        }

        api.setToken(token2);
        try {
            api.changeUserState(userId, false);
            Assert.fail("ApiConnectorException expected, but nothing thrown : user already unblocked and cant be unblocked again.");
        } catch (ApiConnectorException e) {
            Assert.assertEquals("insufficient_scope", e.getMessage());
        }
    }

    /**
     * {@link GitlabApi#blockUser(long)}.
     */
    @Test
    public void blockUser() {
        long id = 1;
        GitlabApi api = Mockito.mock(GitlabApi.class);
        try {
            Mockito.doCallRealMethod().when(api).blockUser(Mockito.anyLong());
            api.blockUser(id);
            Mockito.verify(api, Mockito.times(1)).changeUserState(id, true);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link GitlabApi#unblockUser(long)}.
     */
    @Test
    public void unblockUser() {
        long id = 1;
        GitlabApi api = Mockito.mock(GitlabApi.class);
        try {
            Mockito.doCallRealMethod().when(api).unblockUser(Mockito.anyLong());
            api.unblockUser(id);
            Mockito.verify(api, Mockito.times(1)).changeUserState(id, false);
        } catch (ApiConnectorException e) {
            Assert.fail(e.getMessage());
        }
    }
}