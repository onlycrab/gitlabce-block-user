package com.github.onlycrab.gbu.worker;

import com.github.onlycrab.gbu.exception.LdapException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link LdapSearcher} test class.
 */
@SuppressWarnings({"unchecked", "TryWithIdenticalCatches"})
public class LdapSearcherTest {
    @SuppressWarnings("FieldCanBeLocal")
    private String provider;
    private final String point = "dc=my,dc=com";
    private LdapContext context;
    private LdapSearcher searcher;
    private final String[] users = new String[]{"u1", "u2", "u3"};

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setup(){
        //common
        provider = "ldap://localhost:389";
        context = Mockito.mock(LdapContext.class);
        searcher = Mockito.mock(LdapSearcher.class);
        try {
            Mockito.when(searcher.getLdapContext(provider, null, null)).thenReturn(context);
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }

        Mockito.doCallRealMethod().when(searcher).buildFilterAll(Mockito.anyString(), Mockito.any());
        Mockito.doCallRealMethod().when(searcher).getSearchControls();
        Mockito.doCallRealMethod().when(searcher).setLdapContext(Mockito.any());
        Mockito.doCallRealMethod().when(searcher).setSearchPoint(Mockito.any());
        Mockito.doCallRealMethod().when(searcher).setDomain(Mockito.any());
        Mockito.doCallRealMethod().when(searcher).getSearchPoint();

        searcher.setLdapContext(context);
        searcher.setSearchPoint(point);
        searcher.setDomain(point);

        //search
        try {
            Mockito.when(context.search(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(buildAnswer());
            Mockito.doCallRealMethod().when(searcher).searchUsers(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        } catch (NamingException e) {
            Assert.fail(e.getMessage());
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        }
    }

    private TestNamingEnumeration buildAnswer(){
        Attributes attrsU1 = new BasicAttributes();
        attrsU1.put("sAMAccountName", "u1");
        Attributes attrsU2 = new BasicAttributes();
        attrsU2.put("sAMAccountName", "uu2");
        Attributes attrsU3 = new BasicAttributes();
        attrsU3.put("sAMAccountName", "u3");

        TestNamingEnumeration<SearchResult> answer = new TestNamingEnumeration<>();
        SearchResult sr1 = new SearchResult("", null, attrsU1);
        SearchResult sr2 = new SearchResult("", null, attrsU2);
        SearchResult sr3 = new SearchResult("", null, attrsU3);
        answer.add(sr1);
        answer.add(sr2);
        answer.add(sr3);

        return answer;
    }

    /**
     * {@link LdapSearcher#parseDomain(String)}.
     */
    @Test
    public void parseDomain() {
        try {
            Assert.assertEquals("dc=mycompany,dc=com", LdapSearcher.parseDomain("cn=admin,ou=Users,ou=MC,dc=mycompany,dc=com"));
            Assert.assertEquals("dc=my,dc=com", LdapSearcher.parseDomain("rs,ou=MC,dc=my,dc=com"));
            Assert.assertEquals("dc=some,dc=my,dc=com", LdapSearcher.parseDomain("dc=some,dc=my,dc=com"));
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        }

        String str = "cn=admin,ou=Users,ou=MC";
        try {
            LdapSearcher.parseDomain(str);
            Assert.fail("LdapException expected, but nothing thrown : domain missing : " + str);
        } catch (LdapException e){
            Assert.assertEquals(String.format("LDAP domain missing in <%s>", str), e.getMessage());
        }

        try {
            LdapSearcher.parseDomain(null);
            Assert.fail("LdapException expected, but nothing thrown : domain is <null>");
        } catch (LdapException ignore){ }
    }

    /**
     * {@link LdapSearcher#buildFilterAll(String, String[])}.
     */
    @Test
    public void buildFilterAll() {
        Assert.assertEquals(
                "(&(objectClass=user)(|(sAMAccountName=u1)(sAMAccountName=u2)))",
                searcher.buildFilterAll("(objectClass=user)", new String[]{"u1", "u2"})
        );
        Assert.assertEquals(
                "(&(objectCategory=person)(objectClass=user)(|(sAMAccountName=u1)))",
                searcher.buildFilterAll("(objectCategory=person)(objectClass=user)", new String[]{"u1"})
        );
        Assert.assertEquals(
                "(&(objectClass=user)(|(sAMAccountName=*)))",
                searcher.buildFilterAll("(objectClass=user)", new String[]{})
        );
    }

    /**
     * {@link LdapSearcher#searchUsers(String, String[], String)}.
     */
    @Test
    public void searchUsers() {
        Map<String, Boolean> expected = new HashMap<>();
        expected.put("u1", true);
        expected.put("u2", false);
        expected.put("u3", true);

        try {
            Map<String, Boolean> actual = searcher.searchUsers("", users, point);
            for (Map.Entry<String, Boolean> entryExpected : expected.entrySet()){
                if (!actual.containsKey(entryExpected.getKey())){
                    Assert.fail(String.format("Actual entry not contains key <%s>.", entryExpected.getKey()));
                } else {
                    if (entryExpected.getValue() != actual.get(entryExpected.getKey()).booleanValue()){
                        Assert.fail(String.format("Actual value <%s> not equals to expected <%s>, key : <%s>.",
                                actual.get(entryExpected.getKey()),
                                entryExpected.getValue(),
                                entryExpected.getKey()
                        ));
                    }
                }
            }
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link LdapSearcher#isUserExist(String[])}.
     */
    @Test
    public void isUserExist_StringArr() {
        try {
            Mockito.doCallRealMethod().when(searcher).isUserExist(Mockito.any(String[].class));
            String[] users = new String[]{"u1", "u2"};
            searcher.isUserExist(users);
            Mockito.verify(searcher, Mockito.times(1)).isUserExist(users, point);
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link LdapSearcher#isUserExist(String[], String)}.
     */
    @Test
    public void isUserExist_StringArr_String() {
        try {
            Mockito.doCallRealMethod().when(searcher).isUserExist(Mockito.any(String[].class), Mockito.anyString());

            Map<String, Boolean> actual = searcher.isUserExist(new String[]{"u1"}, point);
            assertContains("u1", true, actual);

            Mockito.when(context.search(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(buildAnswer());
            actual = searcher.isUserExist(new String[]{"u1", "u2", "uu2", "u3"}, point);
            assertContains("u1", true, actual);
            assertContains("u2", false, actual);
            assertContains("uu2", true, actual);
            assertContains("u3", true, actual);

        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        } catch (NamingException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void assertContains(String expectedStr, Boolean expectedBool, Map<String, Boolean> actual){
        for (Map.Entry<String, Boolean> entry : actual.entrySet()){
            if (entry.getKey().equals(expectedStr)){
                if (expectedBool != entry.getValue()){
                    Assert.fail(String.format("Values not equals for key <%s> : expected <%s> -> actual <%s>",
                            expectedStr, expectedBool, entry.getValue()));
                } else {
                    return;
                }
            }
        }
        Assert.fail(String.format("Map not contains key <%s>.", expectedStr));
    }

    /**
     * {@link LdapSearcher#isUserLocked(String[])}.
     */
    @Test
    public void isUserLocked_StringArr() {
        try {
            Mockito.doCallRealMethod().when(searcher).isUserLocked(Mockito.any(String[].class));
            String[] users = new String[]{"u1", "u2"};
            searcher.isUserLocked(users);
            Mockito.verify(searcher, Mockito.times(1)).isUserLocked(users, point);
        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * {@link LdapSearcher#isUserLocked(String[], String)}.
     */
    @Test
    public void isUserLocked_StringArr_String() {
        try {
            Mockito.doCallRealMethod().when(searcher).isUserLocked(Mockito.any(String[].class), Mockito.anyString());

            Map<String, Boolean> actual = searcher.isUserLocked(new String[]{"u1"}, point);
            assertContains("u1", true, actual);

            Mockito.when(context.search(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(buildAnswer());
            actual = searcher.isUserLocked(new String[]{"u2", "uu2", "u3", "u4"}, point);
            assertContains("u2", false, actual);
            assertContains("uu2", true, actual);
            assertContains("u3", true, actual);
            assertContains("u4", false, actual);

        } catch (LdapException e) {
            Assert.fail(e.getMessage());
        } catch (NamingException e) {
            Assert.fail(e.getMessage());
        }
    }
}