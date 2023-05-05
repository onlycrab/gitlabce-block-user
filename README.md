# About this project
This is a Java utility for synchronizing the status of **GitLab Community Edition** server users with a Microsoft Active Directory domain (AD).  
You can configure automatic blocking (or unblocking) of your GitLab server user accounts if the corresponding accounts on the AD is blocked (or unblocked).   

The utility does not work with the GitLab server database or its configuration, all actions are performed through the GitLab API.
# Table of contents 
- [Compatibility](#compatibility)
- [Features](#features)
- [Restrictions](#restrictions)
- [Usage example](#usageExample)
- [Arguments](#arguments)
- [Building](#build)
<a name="compatibility"></a>
# Compatibility
Java version        : 1.8 or above  
Operation system    : Windows or Linux family  
Tested on           : Windows Server 2016 with JRE 8u202, Debian 11 with openjdk-11, GitLab CE 15.0
<a name="features"></a>
# Features
- Block (or unblock) GitLab user if such user is blocked (or unblocked) in the AD
- Process only users with LDAP identities or all. When comparing user with identity, only the match of the identity with the root domain (DC) of AD with the domain checked, the full path of the identity is not used
- One-to-one correspondence of accounts occurs by fields `username` (GitLab) and `samAccountName` (AD)
- Process only user IDs (username) that match the pattern (regular expression)
- Specify a list of user IDs (username) exclusions that will not be processed by any option
- This utility can be run anywhere (not necessarily on the GitLab server), you only need to have access to the GitLab API and AD
<a name="restrictions"></a>
# Restrictions
- Only those accounts for which a corresponding AD entry exists are processed. If the account is deleted from AD, then its analogue on GitLab will not be blocked
- Only GitLab accounts with `active` or `blocked` statе are processed
- This utility is not a daemon and performs a one-time work at startup. For regular automatic processing, you need to use a some scheduler
<a name="usageExample"></a>
# Usage example
```
java -jar gitlabce-block-user.jar -g http://localhost -t mytoken -oi true -adp ldap://localhost:389 -adu cn=admin,ou=Users,ou=MC,dc=mycompany,dc=com -adc password -ads dc=mycompany,dc=com
```
<a name="arguments"></a>
# Arguments
|Short name|Long name|Required|Default|Description|
|----------|---------|--------|-------|-----------|
|h|help|false||Print help info.|
|v|version|false||Print version info.|
|ef|external-file|false||External file for reading argument values. Lines format like .ini file : 'arg'='value'.|
|g|git|true||Gitlab server address. If `https` is used, see `crt(certificate)` argument description.|
|crt|certificate|false||GitLab server certificate file. If not specified - connector will be trust any certificate.|
|t|token|true||GitLab access token with user edit access.|
|oi|only-identity|false|true|Process only GitLab users with identities. Users without identities will not be processed.|
|ex|exclude|false||Exclude this usernames from processing. Usernames must be separated by comma. Example: `username1,test,some_user`.|
|ut|user-template|false||GitLab username template for processing (regular expression). If username does not match the template, then the user will not be processed.|
|to|timeout|false|30|GitLab API response timeout in seconds.|
|adp|ad-provider|true||Active Directory LDAP provider. Example: `ldap://localhost:389`.|
|adu|ad-user|false||User for authentication on Active Directory. Example: `cn=admin,ou=Users,ou=MC,dc=mycompany,dc=com`.|
|adc|ad-credentials|false||Password for authentication on Active Directory.|
|ads|ad-search|true||Active Directory search point path. Example: `dc=mycompany,dc=com`.|
|pm|prod-mode|false|false|By default no one GitLab user will be block or unblock. Set this argument to change the real status of GitLab users.|
<a name="build"></a>
# Building
To build you need Java 1.8 or above and Maven 3.2.5 or above.
  
Add [argument-parser library](https://github.com/onlycrab/argument-parser/releases)  
```
mvn install:install-file -Dfile=<yourPath>/argument-parser-1.0.0.jar -DgroupId=com.github.onlycrab.argParser -DartifactId=argument-parser -Dversion=1.0.0 -Dpackaging=jar
```
Run the build process  
```
mvn clean package
```
