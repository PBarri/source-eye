# Source Eye

This project is based on [Dependency Check](https://www.owasp.org/index.php/OWASP_Dependency_Check), which may fit better your needs.

Also, this project is in a Beta phase, designed for a MSc project and not completely refined.

Project migrated from [Gitlab](https://gitlab.com/PBarrientos/source-eye).

## Goals:

The main goal of this project is to detect vulnerabilities in third party dependencies in projects built with Maven or Gradle.
Once this vulnerabilities are discovered, it aims to inform the user via a REST API, or logs, either local or sent by syslog to remote machines in charge of analyzing logs.

## Installation guide

This installation guide is done with a CentOS7 Linux distribution, although installers are provided for Debian based systems.
The installers can be found at the [documentation page](https://gitlab.com/SourceEye/docs/tree/master/anexo-I).

### Install Java

Source Eye needs Java to execute, with Java 8 as baseline. To install it:

```bash
wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u181-b13/96a7b8442fe848ef90c96a2fad6ed6d1/jdk-8u181-linux-x64.rpm"
yum localinstall jdk-8u181-linux-x64.rpm
```

### Install DB

[MariaDB](https://mariadb.com/) have been chosen as database engine. To install it:

```bash
nano /etc/yum.repos.d/mariadb.repo
```
```text
[mariadb]
name = MariaDB
baseurl = http://yum.mariadb.org/10.1/centos7-amd64
gpgkey=https://yum.mariadb.org/RPM-GPG-KEY-MariaDB
gpgcheck=1
```
```bash
yum install mariadb-server

# El siguiente comando no es obligatorio pero si muy recomendable
mysql_secure_installation

systemctl enable mariadb && systemctl start mariadb
```

### Install Source Eye service

To install Source Eye, we download one of the available installers and execute the following:

```bash
yum localinstall source-eye-{version}.{arch}.rpm
```

After the installation, we would initialize the database, executing the provided SQL file located at ``/var/lib/source-eye/scripts/initialize_database-mysql.sql``

After that, we can configure the service modifying the file ``/var/lib/source-eye/conf/source-eye.yml``. At least the connection with the database and the desired scanners are needed.

To start the service, we execute the commands:

```bash
systemctl enable source-eye && systemctl start source-eye
```
