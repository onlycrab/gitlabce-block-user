package com.github.onlycrab.gbu.worker;

import com.github.onlycrab.gbu.exception.ApiConnectorException;
import com.github.onlycrab.gbu.exception.JsonConverterException;
import com.github.onlycrab.gbu.exception.LdapException;
import com.github.onlycrab.gbu.model.Identity;
import com.github.onlycrab.gbu.model.User;
import com.github.onlycrab.gbu.worker.GitlabApi;
import com.github.onlycrab.gbu.worker.LdapSearcher;
import com.github.onlycrab.gbu.worker.Worker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link Worker} test class.
 */
public class WorkerTest {
    private GitlabApi api;
    private LdapSearcher searcher;
    private Worker worker;

    @Before
    public void setup(){
        api = Mockito.mock(GitlabApi.class);
        searcher = Mockito.mock(LdapSearcher.class);
        worker = Mockito.mock(Worker.class);

        Mockito.doCallRealMethod().when(worker).setGitlabApi(Mockito.any());
        Mockito.doCallRealMethod().when(worker).setLdapSearcher(Mockito.any());
        Mockito.doCallRealMethod().when(worker).setProdMode(Mockito.anyBoolean());
        Mockito.doCallRealMethod().when(worker).setUsernameExclude(Mockito.any(String[].class));
        Mockito.doCallRealMethod().when(worker).setWithIdentities(Mockito.anyBoolean());

        Mockito.doCallRealMethod().when(worker).parseUsernameExclude(Mockito.anyString());
        Mockito.doCallRealMethod().when(worker).removeExclude(Mockito.any(User[].class), Mockito.any(String[].class));
        Mockito.doCallRealMethod().when(worker).applyTemplate(Mockito.any(), Mockito.any());
        try{
            Mockito.doCallRealMethod().when(worker).removeWithoutIdentities(Mockito.any(User[].class));
            Mockito.doCallRealMethod().when(worker).processGitUsers();
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }
        Mockito.doCallRealMethod().when(worker).getUsername(Mockito.any(User[].class));
        Mockito.doCallRealMethod().when(worker).removeNonexistentUsers(Mockito.anyMap());
        Mockito.doCallRealMethod().when(worker).getUsernameFromMap(Mockito.anyMap());

        worker.setLdapSearcher(searcher);
        worker.setGitlabApi(api);
    }

    /**
     * {@link Worker#processGitUsers()}.
     */
    @Test
    public void processGitUsers() {
        User[] usersAllFromGit = new User[] {
                new User(1, "u1", "active", new Identity[0]),
                new User(2, "u2", "active", new Identity[0]),
                new User(3, "u3", "blocked", new Identity[0]),
                new User(4, "u4", "active", new Identity[0]),
                new User(5, "u5", "blocked", new Identity[0]),
                new User(6, "u6", "blocked", new Identity[0]),
        };
        worker.setTemplate("");
        worker.setWithIdentities(false);
        worker.setUsernameExclude(new String[0]);

        try {
            Mockito.when(api.getAllGitUsers()).thenReturn(usersAllFromGit);
        } catch (ApiConnectorException | JsonConverterException e) {
            Assert.fail(e.getMessage());
            return;
        }
        Map<String, Boolean> userMap = new HashMap<>();
        userMap.put("u1", true);
        userMap.put("u2", true);
        userMap.put("u3", true);
        userMap.put("u4", true);
        userMap.put("u5", true);
        userMap.put("u6", true);
        Map<String, Boolean> userLocked = new HashMap<>();
        userLocked.put("u1", true);
        userLocked.put("u2", false);
        userLocked.put("u3", true);
        userLocked.put("u4", false);
        userLocked.put("u5", false);
        userLocked.put("u6", true);
        try {
            Mockito.when(searcher.isUserExist(Mockito.any(String[].class))).thenReturn(userMap);
            Mockito.when(searcher.isUserLocked(Mockito.any(String[].class))).thenReturn(userLocked);
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
            return;
        }

        String[] expected = new String[]{
                "[{\"to_state\":\"BLOCK\",\"result\":\"SUCCESS\",\"user\":{\"id\":1,\"username\":\"u1\",\"state\":\"active\",\"identities\":[]}}," +
                        "{\"to_state\":\"UNBLOCK\",\"result\":\"SUCCESS\",\"user\":{\"id\":5,\"username\":\"u5\",\"state\":\"blocked\",\"identities\":[]}}]",
                "[{\"to_state\":\"BLOCK\",\"result\":\"NONE\",\"user\":{\"id\":1,\"username\":\"u1\",\"state\":\"active\",\"identities\":[]}}," +
                        "{\"to_state\":\"UNBLOCK\",\"result\":\"NONE\",\"user\":{\"id\":5,\"username\":\"u5\",\"state\":\"blocked\",\"identities\":[]}}]"
        };
        String actual;
        try {
            worker.setProdMode(true);
            actual = worker.processGitUsers();
            Assert.assertEquals(expected[0], actual);
            worker.setProdMode(false);
            actual = worker.processGitUsers();
            Assert.assertEquals(expected[1], actual);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**
     * {@link Worker#parseUsernameExclude(String)}.
     */
    @Test
    public void parseUsernameExclude() {
        Assert.assertArrayEquals(new String[0], worker.parseUsernameExclude(""));
        Assert.assertArrayEquals(new String[]{"u1"}, worker.parseUsernameExclude("u1"));
        Assert.assertArrayEquals(new String[]{"u1", "u2", "u3"}, worker.parseUsernameExclude("u1,u2,u3"));
        Assert.assertArrayEquals(new String[]{"u1", "u2"}, worker.parseUsernameExclude(",u1,,u2,"));
        Assert.assertArrayEquals(new String[0], worker.parseUsernameExclude(",,,"));
    }

    /**
     * {@link Worker#removeExclude(User[], String[])}.
     */
    @Test
    public void removeExclude() {
        User[] users = new User[]{
                new User("u1"),
                new User("u2"),
                new User("u3")
        };
        Assert.assertArrayEquals(new User[0], worker.removeExclude(new User[0], new String[]{"u1"}));
        Assert.assertArrayEquals(users, worker.removeExclude(users, new String[0]));
        Assert.assertArrayEquals(new User[]{
                new User("u2"),
                new User("u3")
        }, worker.removeExclude(users, new String[]{"u1"}));
        Assert.assertArrayEquals(
                new User[]{new User("u2")},
                worker.removeExclude(users, new String[]{"u1", "u3"})
        );
        Assert.assertArrayEquals(new User[0], worker.removeExclude(users, new String[]{"u1", "u3", "u2"}));
    }

    /**
     * {@link Worker#applyTemplate(User[], String)}.
     */
    @Test
    public void applyTemplate() {
        User[] users = new User[]{
                new User("user_1"),
                new User("usr02"),
                new User("u3")
        };
        Assert.assertArrayEquals(new User[]{new User("user_1")}, worker.applyTemplate(users, "([A-Za-z]+)_\\d+"));
        Assert.assertArrayEquals(
                new User[]{
                    new User("usr02"),
                    new User("u3")
                }, worker.applyTemplate(users, "([A-Za-z]+)\\d+"));
        Assert.assertArrayEquals(new User[]{}, worker.applyTemplate(users, "([A-Za-z]{6})\\d+"));
    }

    /**
     * {@link Worker#removeWithoutIdentities(User[])}.
     */
    @Test
    public void removeWithoutIdentities() {
        Mockito.when(searcher.getDomain()).thenReturn("dc=example,dc=com");
        Identity identity1 = new Identity("ldapmain", "cn=u1,ou=users,dc=example,dc=com");
        Identity identity2 = new Identity("ldapmain", "cn=u2,ou=users,dc=example2,dc=com");
        Identity identity3 = new Identity("ldapmain", "");
        Identity identity4 = new Identity("ldapmain", "cn=u4,ou=some,ou=users,dc=example,dc=com");
        User user1 = new User("u1");
        User user2 = new User("u2");
        User user3 = new User("u3");
        User user4 = new User("u4");
        User user5 = new User("u5");
        user1.setIdentities(new Identity[]{identity1});
        user2.setIdentities(new Identity[]{identity2});
        user3.setIdentities(new Identity[]{identity3});
        user4.setIdentities(new Identity[]{identity4});
        user5.setIdentities(new Identity[0]);
        User[] users = new User[]{ user1, user2, user3, user4, user5};
        User[] expected = new User[]{ user1, user4 };
        User[] actual;
        try {
            actual = worker.removeWithoutIdentities(users);
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
            return;
        }
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * {@link Worker#getUsername(User[])}.
     */
    @Test
    public void getUsername() {
        User[] users = new User[]{
                new User("u1"),
                new User("u2"),
                new User("u3")
        };
        String[] expected = new String[]{ "u1", "u2", "u3" };
        String[] actual = worker.getUsername(users);
        Assert.assertArrayEquals(expected, actual);
        Assert.assertArrayEquals(new User[0], worker.getUsername(new User[0]));
    }

    /**
     * {@link Worker#removeNonexistentUsers(Map)}.
     */
    @Test
    public void removeNonexistentUsers() {
        Map<String, Boolean> users = new HashMap<>();
        users.put("u1", true);
        users.put("u2", true);
        users.put("u3", false);
        users.put("u4", true);
        users.put("u5", false);
        Map<String, Boolean> expected = new HashMap<>();
        expected.put("u1", true);
        expected.put("u2", true);
        expected.put("u4", true);
        Map<String, Boolean> actual = worker.removeNonexistentUsers(users);
        Assert.assertEquals(expected, actual);
    }

    /**
     * {@link Worker#getUsernameFromMap(Map)}.
     */
    @Test
    public void getUsernameFromMap() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("u1", true);
        map.put("u2", true);
        map.put("u3", false);
        String[] expected = new String[]{ "u1", "u2", "u3" };
        String[] actual = worker.getUsernameFromMap(map);
        Assert.assertArrayEquals(expected, actual);
    }
}