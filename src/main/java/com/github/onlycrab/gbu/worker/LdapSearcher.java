package com.github.onlycrab.gbu.worker;

import com.github.onlycrab.gbu.exception.LdapException;
import lombok.Getter;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Class for searching users in Active Directory via LDAP.
 *
 * LDAP Matching Rules (https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-adts/4e638665-f466-4597-93c4-12f2ebfabab5?redirectedfrom=MSDN)
 * LDAP_MATCHING_RULE_BIT_AND           1.2.840.113556.1.4.803
 * LDAP_MATCHING_RULE_BIT_OR            1.2.840.113556.1.4.804
 * LDAP_MATCHING_RULE_TRANSITIVE_EVAL   1.2.840.113556.1.4.1941
 * LDAP_MATCHING_RULE_DN_WITH_DATA      1.2.840.113556.1.4.2253
 *
 * UserAccountControl flags (https://learn.microsoft.com/en-US/troubleshoot/windows-server/identity/useraccountcontrol-manipulate-account-properties)
 * SCRIPT                           0x0001      1
 * ACCOUNTDISABLE                   0x0002      2
 * HOMEDIR_REQUIRED                 0x0008      8
 * LOCKOUT                          0x0010      16
 * PASSWD_NOTREQD                   0x0020      32
 * PASSWD_CANT_CHANGE               0x0040      64
 * ENCRYPTED_TEXT_PWD_ALLOWED       0x0080      128
 * TEMP_DUPLICATE_ACCOUNT           0x0100      256
 * NORMAL_ACCOUNT                   0x0200      512
 * INTERDOMAIN_TRUST_ACCOUNT        0x0800      2048
 * WORKSTATION_TRUST_ACCOUNT        0x1000      4096
 * SERVER_TRUST_ACCOUNT             0x2000      8192
 * DONT_EXPIRE_PASSWORD             0x10000     65536
 * MNS_LOGON_ACCOUNT                0x20000     131072
 * SMARTCARD_REQUIRED               0x40000     262144
 * TRUSTED_FOR_DELEGATION           0x80000     524288
 * NOT_DELEGATED                    0x100000    1048576
 * USE_DES_KEY_ONLY                 0x200000    2097152
 * DONT_REQ_PREAUTH                 0x400000    4194304
 * PASSWORD_EXPIRED                 0x800000    8388608
 * TRUSTED_TO_AUTH_FOR_DELEGATION   0x1000000   16777216
 * PARTIAL_SECRETS_ACCOUNT          0x04000000  67108864
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
@Getter
public class LdapSearcher {
    /**
     * LDAP context.
     */
    private LdapContext ldapContext;
    /**
     * Node within which the search will be performed.
     */
    private String searchPoint;
    /**
     * Root domain.
     */
    private String domain;
    /**
     * Factors that determine scope of search and what gets returned as a result of the search.
     */
    private SearchControls searchControls;

    protected void setLdapContext(LdapContext context){
        if (context != null){
            ldapContext = context;
        }
    }

    protected void setSearchPoint(String point){
        if (point != null){
            searchPoint = point;
        }
    }

    protected void setDomain(String domain){
        if (domain != null){
            this.domain = domain;
        }
    }

    @SuppressWarnings("unused")
    protected void setSearchControls(SearchControls searchControls){
        if (searchControls != null){
            this.searchControls = searchControls;
        }
    }

    /**
     * Initialize LDAP searcher by provider and search node. LDAP auth is anonymous.
     *
     * @param provider provider string like {@code ldap://server:port}
     * @param point search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @throws LdapException if {@code point} is null;
     *                      if LDAP connection exception occurs;
     *                      if an error occurred while extracting a domain from the {@code point}
     */
    public LdapSearcher(String provider, String point) throws LdapException {
        ldapContext = getLdapContext(provider, null, null);
        if (point == null){
            throw new LdapException("Search point can not be <null>.");
        } else {
            this.searchPoint = point;
        }
        this.domain = parseDomain(searchPoint);
        searchControls = buildSearchControls(new String[]{"sAMAccountName"});
    }

    /**
     * Initialize LDAP searcher by provider and search node. LDAP auth by credentials.
     *
     * @param provider provider string like {@code ldap://server:port}
     * @param principal LDAP user
     * @param credentials LDAP password
     * @param point search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @throws LdapException if {@code point} is null;
     *                      if LDAP connection exception occurs;
     *                      if an error occurred while extracting a domain from the {@code point}
     */
    public LdapSearcher(String provider, String principal, String credentials, String point) throws LdapException {
        ldapContext = getLdapContext(provider, principal, credentials);
        if (point == null){
            throw new LdapException("Search point can not be <null>.");
        } else {
            this.searchPoint = point;
        }
        this.domain = parseDomain(searchPoint);
        searchControls = buildSearchControls(new String[]{"sAMAccountName"});
    }

    /**
     * Get LDAP domain from search point.
     *
     * @param point search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @return LDAP domain
     * @throws LdapException if {@code point} is null;
     *                      if an error occurred while extracting a domain from the {@code point}
     */
    public static String parseDomain(String point) throws LdapException {
        if (point == null){
            throw new LdapException("LDAP point is <null>");
        }
        if (point.toLowerCase().startsWith("dc=")){
            return point;
        }
        int i = point.toLowerCase().indexOf(",dc=") + 1;
        if (i < 1){
            throw new LdapException(String.format("LDAP domain missing in <%s>", point));
        } else {
            return point.substring(i);
        }
    }

    /**
     * Build LDAP context.
     *
     * @param provider provider string like {@code ldap://server:port}
     * @param principal LDAP user
     * @param credentials LDAP password
     * @return built LDAP context
     * @throws LdapException if LDAP connection exception occurs
     */
    protected LdapContext getLdapContext(String provider, String principal, String credentials) throws LdapException {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            if (principal != null && credentials != null) {
                env.put(Context.SECURITY_PRINCIPAL, principal);
                env.put(Context.SECURITY_CREDENTIALS, credentials);
            }
            env.put(Context.PROVIDER_URL, provider);
            env.put(Context.REFERRAL, "follow");
            return new InitialLdapContext(env, null);
        } catch (NamingException e) {
            throw new LdapException(String.format("LDAP connection failed : %s.", e.getMessage()));
        }
    }

    /**
     * Build filter for search data by {@code filter} and all {@code users}.
     * Result filter template: (and{@code base_filter}({@code user_search_filter)}.
     * Example: (and{@code (objectClass=user)}(or{@code (sAMAccountName=user1)(sAMAccountName=user2)})).
     *
     * @param filter base filter expression
     * @param users array of users for searching
     * @return LDAP filter expression to use for the search
     */
    protected String buildFilterAll(String filter, String[] users){
        if (filter == null || users == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(&").append(filter).append("(|");
        if (users.length == 0){
            sb.append("(sAMAccountName=*)");
        } else {
            for (String user : users){
                sb.append("(sAMAccountName=").append(user).append(")");
            }
        }
        sb.append("))");
        return sb.toString();
    }

    /**
     * Search users by LDAP.
     * Result map key-value pairs:
     *      key: LDAP user ID
     *      value: {@code true} if user founded, otherwise - {@code false}.
     *
     * @param filter base filter expression to use for the search
     * @param users array of users for searching
     * @param point search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @return map whose keys are user IDs, values ​​are a boolean indication of whether users were found
     * @throws LdapException if error occurs while working with LDAP search
     */
    protected Map<String, Boolean> searchUsers(String filter, String[] users, String point) throws LdapException {
        Map<String, Boolean> result = new HashMap<>();
        for (String user : users){
            result.put(user, false);
        }
        String filterAll = buildFilterAll(filter, users);
        try {
            NamingEnumeration<SearchResult> answer = ldapContext.search(point, filterAll, searchControls);
            while (answer.hasMore()){
                Attributes attrs = answer.nextElement().getAttributes();
                String name = attrs.get("sAMAccountName").get().toString();
                result.put(name, true);
            }
        } catch (Exception e) {
            throw new LdapException(String.format("LDAP search failed : %s.", e.getMessage()));
        }
        return result;
    }

    private SearchControls buildSearchControls(String[] attrs) {
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(attrs);
        return sc;
    }

    /**
     * Determine if users exist.
     * Result map key-value pairs:
     *      key: LDAP user ID
     *      value: {@code true} if user exist, otherwise - {@code false}.
     *
     * @param users users for searching
     * @return map whose keys are user IDs, values ​​are a boolean indication of whether users were exist
     * @throws LdapException if error occurs while working with LDAP search
     */
    public Map<String, Boolean> isUserExist(String[] users) throws LdapException {
        return isUserExist(users, searchPoint);
    }

    /**
     * Determine if users exist.
     * Result map key-value pairs:
     *      key: LDAP user ID
     *      value: {@code true} if user exist, otherwise - {@code false}.
     *
     * @param users users for searching
     * @param point search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @return map whose keys are user IDs, values ​​are a boolean indication of whether users were exist
     * @throws LdapException if error occurs while working with LDAP search
     */
    public Map<String, Boolean> isUserExist(String[] users, String point) throws LdapException {
        return searchUsers(
                "(objectCategory=person)(objectClass=user)",
                users,
                point
        );
    }

    /**
     * Determine if users locked.
     * Result map key-value pairs:
     *      key: LDAP user ID
     *      value: {@code true} if user locked, otherwise - {@code false}.
     *
     * @param users users for searching
     * @return map whose keys are user IDs, values ​​are a boolean indication of whether users were locked
     * @throws LdapException if error occurs while working with LDAP search
     */
    public Map<String, Boolean> isUserLocked(String[] users) throws LdapException {
        return isUserLocked(users, searchPoint);
    }

    /**
     * Determine if users locked.
     * Result map key-value pairs:
     *      key: LDAP user ID
     *      value: {@code true} if user locked, otherwise - {@code false}.
     *
     * @param users users for searching
     * @param point search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @return map whose keys are user IDs, values ​​are a boolean indication of whether users were locked
     * @throws LdapException if error occurs while working with LDAP search
     */
    public Map<String, Boolean> isUserLocked(String[] users, String point) throws LdapException {
        return searchUsers(
                "(objectCategory=person)(objectClass=user)(userAccountControl:1.2.840.113556.1.4.803:=2)",
                users,
                point
        );
    }
}
