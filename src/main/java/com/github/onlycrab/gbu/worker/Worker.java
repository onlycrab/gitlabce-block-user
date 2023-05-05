package com.github.onlycrab.gbu.worker;

import com.github.onlycrab.common.ISUtil;
import com.github.onlycrab.common.StringUtil;
import com.github.onlycrab.gbu.exception.ApiConnectorException;
import com.github.onlycrab.gbu.exception.JsonConverterException;
import com.github.onlycrab.gbu.exception.LdapException;
import com.github.onlycrab.gbu.model. *;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The central class that does all the work. All arguments received from the console must be passed here. This is
 * where API and LDAP communication objects are instantiated. The method that does all the work -
 * {@link Worker#processGitUsers()}.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
public class Worker {
    private static final Logger LOGGER = LogManager.getLogger(Worker.class);
    /**
     * Possible user states to be processed.
     */
    private static final String USER_ACTIVE = "active";
    private static final String USER_BLOCKED = "blocked";
    /**
     * Object for interaction with Gitlab API.
     */
    private GitlabApi gitlabApi;
    /**
     * Object for searching users in Active Directory via LDAP.
     */
    private LdapSearcher ldapSearcher;
    /**
     * Gitlab username exclusions. These names will not be processed.
     */
    private String[] usernameExclude;
    /**
     * Only users whose names match the pattern will be processed.
     */
    private String template;
    /**
     * A sign of whether it is necessary to process Gitlab users by their ID, even if they do not have an AD binding.
     */
    private boolean withIdentities;
    /**
     * Sign of production mode. If {@code true} - Gitlab users will be locked or unlocked depending on their status in
     * AD. If {@code false} - the state of Gitlab users will not be changed, only a record will be created in the log
     * that the user state in Gitlab and AD is different.
     */
    private boolean prodMode;

    protected void setGitlabApi(GitlabApi gitlabApi){
        this.gitlabApi = gitlabApi;
    }

    protected void setLdapSearcher(LdapSearcher ldapSearcher){
        this.ldapSearcher = ldapSearcher;
    }

    protected void setUsernameExclude(String[] usernameExclude) {
        this.usernameExclude = usernameExclude;
    }

    @SuppressWarnings("SameParameterValue")
    protected void setTemplate(String template) { this.template = template; }

    protected void setWithIdentities(boolean withIdentities) {
        this.withIdentities = withIdentities;
    }

    protected void setProdMode(boolean prodMode) {
        this.prodMode = prodMode;
    }

    /**
     * Create new instance.
     *
     * @param git Gitlab root address (f.e. https://gitlab.mycompany.com)
     * @param token token with permission to modify users
     * @param certificatePath path to Gitlab web certificate
     * @param exclude list of users that be excluded from processing
     * @param template regex template for processing usernames
     * @param withIdentities whether to process Gitlab users without binding (identity)
     * @param timeout timeout for API response (milliseconds)
     * @param provider AD provider string like {@code ldap://server:port}
     * @param principal LDAP user
     * @param credentials LDAP password
     * @param searchPoint search node, f.e. {@code ou=Users,ou=MC,dc=mycompany,dc=com}
     * @param prodMode if {@code true} - Gitlab users will be locked or unlocked depending on their status in AD.
     *                 If {@code false} - the state of Gitlab users will not be changed, only a record will be created
     *                 in the log that the user state in Gitlab and AD is different
     * @throws IOException if an I/O error occurs while reading certificate file
     * @throws IllegalArgumentException if timeout value is not an integer
     * @throws LdapException if error occurs while working with LDAP
     * @throws ApiConnectorException if exception occurs while trusting certificate
     */
    public Worker(String git, String token, @Nullable String certificatePath, String exclude,
                  String template, boolean withIdentities, String timeout, String provider,
                  String principal, String credentials, String searchPoint, boolean prodMode)
            throws IOException, IllegalArgumentException, LdapException, ApiConnectorException {
        //Read certificate
        byte[] cert = null;
        if (certificatePath != null){
            try {
                cert = ISUtil.readBytes(new FileInputStream(new File(certificatePath)));
            } catch (IOException e) {
                throw new IOException(String.format("Cant read certificate file <%s> : %s.", certificatePath, e.getMessage()));
            }
        }

        if (cert != null){
            gitlabApi = new GitlabApi(git, token, cert);
        } else {
            gitlabApi = new GitlabApi(git, token);
        }

        if (!StringUtil.isEmptyOrNull(timeout)){
            try {
                int to = Integer.parseInt(timeout);
                gitlabApi.setTimeout(to);
            } catch (NumberFormatException e){
                throw new IllegalArgumentException(String.format("Timeout value <%s> is not an integer.", timeout));
            }
        }
        if (StringUtil.isEmptyOrNullAtLeastOne(principal, credentials)){
            ldapSearcher = new LdapSearcher(provider, searchPoint);
        } else {
            ldapSearcher = new LdapSearcher(provider, principal, credentials, searchPoint);
        }
        usernameExclude = parseUsernameExclude(exclude);
        this.template = template;
        this.withIdentities = withIdentities;
        this.prodMode = prodMode;
    }

    /**
     * The method performs a mapping of Gitlab users and AD users, depending on the AD state, blocks or unblocks
     * Gitlab users.
     *
     * @return processing result in JSON format
     * @throws ApiConnectorException if exception occurs while communicating with Gitlab API
     * @throws LdapException if exception occurs while working with LDAP
     * @throws JsonConverterException if the API response cannot be converted to an array {@link User}
     */
    public String processGitUsers() throws ApiConnectorException, LdapException, JsonConverterException, IllegalArgumentException {
        User[] users;
        //Get all Gitlab users
        users = gitlabApi.getAllGitUsers();
        //Remove exclude users from processing
        users = removeExclude(users, usernameExclude);
        //Remove users whose names do not match the pattern
        users = applyTemplate(users, template);
        if (withIdentities){
            //Remove users who not have AD binding
            users = removeWithoutIdentities(users);
        }

        if (users.length == 0){
            LOGGER.info("No one user find to check AD state.");
            return "[]";
        }
        Map<String, Boolean> userLocked;
        String[] usersArr;
        //Search users in AD : key - username (id), value - is user exists
        Map<String, Boolean> userMap = ldapSearcher.isUserExist(getUsername(users));
        //Remove users that don't exist in AD
        userMap = removeNonexistentUsers(userMap);
        if (userMap.size() == 0){
            StringBuilder sb = new StringBuilder("No AD user found. Search list : ");
            for (User user : users){
                sb.append(user.getUsername()).append("; ");
            }
            sb.append(".");
            LOGGER.info(sb.toString());
            return "[]";
        }
        //Get array of username fields from map of users
        usersArr = getUsernameFromMap(userMap);
        userMap.clear();

        //Search users in AD : key - username (id), value - is user locked
        userLocked = ldapSearcher.isUserLocked(usersArr);

        ChangeUserStateAnswer answer;
        List<ChangeUserStateAnswer> ansList = new ArrayList<>();
        //Check each user
        for (User user : users){
            if (!userLocked.containsKey(user.getUsername())){
                continue;
            }
            //Each user have own answer
            answer = new ChangeUserStateAnswer();
            answer.setUser(user);
            //Send block or unblock request to Gitlab API
            if (USER_ACTIVE.equals(user.getState()) && userLocked.get(user.getUsername())) {
                answer.setToState(UserChangeState.BLOCK);
                if (prodMode){
                    try {
                        gitlabApi.blockUser(user.getId());
                        answer.setResult(UserChangeStateResult.SUCCESS);
                    } catch (ApiConnectorException e){
                        answer.setResult(UserChangeStateResult.FAIL);
                        LOGGER.error("Error at Gitlab API block user {}-{} request : {}",
                                user.getId(),
                                user.getUsername(),
                                e.getMessage()
                        );
                    }
                } else {
                    answer.setResult(UserChangeStateResult.NONE);
                }
            } else if (USER_BLOCKED.equals(user.getState()) && !userLocked.get(user.getUsername())) {
                answer.setToState(UserChangeState.UNBLOCK);
                if (prodMode){
                    try {
                        gitlabApi.unblockUser(user.getId());
                        answer.setResult(UserChangeStateResult.SUCCESS);
                    } catch (ApiConnectorException e){
                        answer.setResult(UserChangeStateResult.FAIL);
                        LOGGER.error("Error at Gitlab API unblock user {}-{} request : {}",
                                user.getId(),
                                user.getUsername(),
                                e.getMessage()
                        );
                    }
                } else {
                    answer.setResult(UserChangeStateResult.NONE);
                }
            } else {
                continue;
            }

            ansList.add(answer);
        }

        //Collect all responses and convert to JSON
        try {
            ChangeUserStateAnswer[] arr = new ChangeUserStateAnswer[ansList.size()];
            ansList.toArray(arr);
            return new JsonConverter().toJson(arr);
        } catch (Exception e){
            throw new JsonConverterException(String.format("Error at converting result to JSON : %s", e.getMessage()));
        }
    }

    /**
     * Parse exclude user list to array.
     *
     * @param exclude list of users that be excluded from processing
     * @return array of users that be excluded from processing
     */
    protected String[] parseUsernameExclude(String exclude){
        if (StringUtil.isEmptyOrNull(exclude)){
            return new String[0];
        } else {
            List<String> list = new ArrayList<>();
            for (String s : exclude.split(",")){
                if (s.trim().length() > 0){
                    list.add(s);
                }
            }
            if (list.size() == 0){
                return new String[0];
            }
            String[] result = new String[list.size()];
            list.toArray(result);
            return result;
        }
    }

    /**
     * Remove exclude users from all users array.
     *
     * @param users all users array
     * @param exclude users that be excluded from processing
     * @return users array without excluded users
     */
    protected User[] removeExclude(User[] users, String[] exclude){
        if (users == null){
            return new User[0];
        } else if (users.length == 0){
            return users;
        }
        if (exclude == null){
            return users;
        } else if (exclude.length == 0){
            return users;
        }

        List<User> list = new ArrayList<>();
        boolean skip;
        for (User user : users){
            skip = false;
            for (String excl : exclude){
                if (user.getUsername().equals(excl)){
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                list.add(user);
            }
        }
        User[] result = new User[list.size()];
        list.toArray(result);
        return result;
    }

    /**
     * Remove users that not matching the template.
     *
     * @param users all users array
     * @param template regex template for processing usernames
     * @return users array matching the template
     * @throws IllegalArgumentException if template format is invalid
     */
    protected User[] applyTemplate(User[] users, String template) throws IllegalArgumentException {
        if (users == null){
            return new User[0];
        } else if (users.length == 0){
            return users;
        }
        if (StringUtil.isEmptyOrNull(template)){
            return users;
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(template);
        } catch (PatternSyntaxException e){
            throw new IllegalArgumentException(String.format("Wrong template format : %s.", e.getMessage()));
        }

        List<User> list = new ArrayList<>();
        for (User user : users){
            if (pattern.matcher(user.getUsername()).matches()){
                list.add(user);
            }
        }
        User[] result = new User[list.size()];
        list.toArray(result);
        return result;
    }

    /**
     * Remove users who not have AD binding.
     *
     * @param users all users array
     * @return array of only those users who have AD binding
     * @throws LdapException if an error occurred while extracting a domain from the {@code point}
     */
    protected User[] removeWithoutIdentities(User[] users) throws LdapException {
        if (users == null){
            return new User[0];
        } else if (users.length == 0){
            return users;
        }
        List<User> arr = new ArrayList<>();
        for (User user : users){
            if (user.getIdentities().length > 0){
                for (Identity identity : user.getIdentities()){
                    if (StringUtil.isEmptyOrNull(identity.getExternUid())){
                        continue;
                    }
                    if (LdapSearcher.parseDomain(identity.getExternUid()).toLowerCase().equals(ldapSearcher.getDomain().toLowerCase())){
                        arr.add(user);
                        break;
                    }
                }
            }
        }
        User[] result = new User[arr.size()];
        arr.toArray(result);
        return result;
    }

    /**
     * Get array of username fields from user object array.
     *
     * @param users user array
     * @return array of username
     */
    protected String[] getUsername(User[] users){
        if (users == null){
            return new String[0];
        }
        String[] username = new String[users.length];
        for (int i = 0; i < users.length; i++){
            username[i] = users[i].getUsername();
        }
        return username;
    }

    /**
     * Remove users that don't exist in AD.
     *
     * @param map map of users, where key - user ID (username), value - boolean sign of the user's existence in AD
     * @return map of users without users that don't exist
     */
    protected Map<String, Boolean> removeNonexistentUsers(Map<String, Boolean> map){
        Map<String, Boolean> result = new HashMap<>();
        for(Map.Entry<String, Boolean> entry : map.entrySet()){
            if (entry.getValue()){
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Get array of username fields from map of users.
     *
     * @param map map of users, where key - user ID, value - boolean sign of the user's existence in AD
     * @return array of username
     */
    protected String[] getUsernameFromMap(Map<String, Boolean> map){
        String[] username = new String[map.keySet().size()];
        map.keySet().toArray(username);
        return username;
    }
}
