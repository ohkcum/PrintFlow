# PrintFlowLite Troubleshooting

This document covers common issues and their solutions.

---

## Installation Issues

### "java: command not found" or "javac: command not found"

Java JDK is not installed or not in PATH.

**Solution:**
```bash
# Check Java installation
java -version
javac -version

# Install OpenJDK 11+
sudo apt install default-jdk-headless

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/default-java
export PATH=$JAVA_HOME/bin:$PATH
```

### "mvn: command not found"

Maven is not installed.

**Solution:**
```bash
# Install Maven
sudo apt install maven

# Verify installation
mvn -version
```

### Build fails with "package does not exist"

Dependencies could not be resolved.

**Solutions:**
- Ensure you are connected to the internet
- Check Maven repository access
- Try with a clean local repository:
```bash
rm -rf ~/.m2/repository
mvn clean install -DskipTests
```

### "Error: Could not find or load main class org.apache.maven.surefire.booter.ForkedBooter"

Maven Surefire configuration issue.

**Solution:**
```bash
# Use the pre-configured skipTests flag
mvn clean install -DskipTests

# Or run tests with system classloader
mvn test -DuseSystemClassLoader=false
```

### CUPS notifier fails to build — "xmlrpc++.h: No such file"

Missing XML-RPC C++ dependency.

**Solution:**
```bash
# Clone the xmlrpcpp dependency
cd ../../
git clone https://gitlab.com/printflowlite/xmlrpcpp.git

# Build it first
cd xmlrpcpp
make

# Then build cups-notifier
cd ../cups-notifier
make
```

### CUPS notifier fails to build — "cups/cups.h: No such file"

CUPS development headers not installed.

**Solution:**
```bash
sudo apt install libcups2-dev
```

---

## Server Startup Issues

### Server won't start — "Port 8632 already in use"

Another process is using the default port.

**Solution:**
```bash
# Find the process using port 8632
sudo lsof -i :8632
# or
sudo netstat -tlnp | grep 8632

# Stop the conflicting process, or change the port in server.properties
```

### Server won't start — "Database is locked"

Concurrent database access with embedded Derby.

**Solution:**
- This happens when the embedded Derby database is accessed concurrently
- **Switch to PostgreSQL** for production environments
- Ensure no other PrintFlowLite instances are running

### Server won't start — "OutOfMemoryError"

JVM heap memory is insufficient.

**Solution:**
```bash
# Increase heap size
java -Xmx2048m -Xms512m -jar printflowlite-server-1.7.0.war
```

Or set in `server.properties`:
```properties
jvm.maxHeap=2048m
jvm.minHeap=512m
```

### Server starts but browser shows "Connection refused"

The server may not be listening on the expected interface.

**Solutions:**
- Check the server is actually running: `ps aux | grep printflowlite`
- Check the bind address in `server.properties`:
```properties
server.host=0.0.0.0  # Listen on all interfaces
# server.host=127.0.0.1  # Localhost only
```
- Check firewall: `sudo iptables -L -n | grep 8632`

---

## Print Issues

### Print jobs don't appear in SafePages

**Possible causes and solutions:**

1. **Wrong printer selected** — Ensure you print to the PrintFlowLite queue, not a direct printer
2. **Authentication failed** — Check your username/password in the printer driver
3. **Server not reachable** — Verify network connectivity to the PrintFlowLite server
4. **CUPS not running** — `sudo systemctl status cups`
5. **Wrong protocol** — JetDirect accepts PostScript only; use IPP for other formats

**Quick test:**
```bash
# Test IPP printing directly
lp -d PrintFlowLite -H hold /etc/hosts
```

### "Access Denied" when printing from application

Authentication failed in the printer driver.

**Solutions:**
- Verify the username in the printer driver matches your PrintFlowLite login
- For LDAP users, use your LDAP password
- Check the `ipp-attribute` setting for the correct authentication method
- Regenerate your app-key in User Details > Internet Printer

### Proxy printer shows as "Offline"

**Solutions:**
1. Check the physical printer is powered on and connected
2. Verify CUPS queue is enabled: `lpstat -a`
3. Test printing directly from CUPS:
```bash
lp -d PrinterName /etc/hosts
```
4. Check SNMP settings (if configured)
5. For USB printers, try unplugging and reconnecting

### Proxy printing fails — "Client-error-not-possible"

The print job could not be sent to the physical printer.

**Solutions:**
- Check the physical printer has paper and toner
- Verify the printer is not in an error state (paper jam, door open)
- Test with a simple print job first
- Check the CUPS error log: `tail -50 /var/log/cups/error_log`

### Mail Print not working

**Solutions:**
1. **IMAP connection failed** — Verify server address, port, and credentials
2. **SSL/TLS error** — Check certificate validity or try different TLS mode
3. **Email not arriving** — Check the dedicated Mail Print address is correct
4. **Wrong sender** — Mail Print only accepts from whitelisted addresses
5. **Large attachment** — Check the `Max Attachment Size` setting

Test IMAP manually:
```bash
openssl s_client -connect mail.example.com:993
```

### Web Print upload fails

**Solutions:**
- File size exceeds limit — check `Max File Size` in Options > Web Print
- Unsupported file type — check `Allowed File Types`
- Server disk space full — `df -h`
- Network timeout — increase `Upload Timeout` in advanced settings

---

## User Management Issues

### LDAP sync fails

**Solutions:**
1. **Server unreachable** — `ping ldap.example.com`
2. **Port blocked** — `telnet ldap.example.com 389`
3. **Bind DN wrong** — Test with `ldapsearch`
4. **Password expired** — Update the service account password
5. **TLS certificate error** — Install the CA certificate

Test LDAP manually:
```bash
ldapsearch -H ldaps://ldap.example.com -D "cn=admin,dc=example,dc=com" \
  -W -b "dc=example,dc=com" "(objectClass=user)"
```

### OAuth login not working

**Solutions:**
1. Verify Client ID and Client Secret are correct
2. Check the redirect URI matches exactly (including protocol)
3. Ensure the OAuth provider is enabled
4. Check domain restrictions (if configured)
5. Verify the OAuth provider is not blocking the application

### Users can't log in — "Session expired"

Session timeout is too short or cookies are blocked.

**Solutions:**
- Increase session timeout in Options > User Authentication
- Check browser allows cookies from the PrintFlowLite domain
- Clear browser cache and cookies
- Check the server time is correct (time drift can cause session issues)

---

## Performance Issues

### Slow page loads / timeouts

**Solutions:**
1. **Increase JVM heap** — especially if processing large PDFs
2. **Database connection pool** — increase in `server.properties`:
```properties
db.pool.maxActive=30
db.pool.maxIdle=15
```
3. **Switch to PostgreSQL** — embedded Derby is slow for multi-user
4. **Too many SafePages** — implement retention policy and cleanup old jobs
5. **Large PDF rendering** — check disk I/O and CPU load

### High CPU usage

**Solutions:**
1. Check for runaway processes: `top` or `htop`
2. Too many concurrent users — consider scaling out
3. Large PDF processing — batch process or upgrade hardware
4. Database locking (embedded Derby) — migrate to PostgreSQL

### Out of disk space

SafePages and logs can consume significant disk space.

**Solutions:**
```bash
# Check disk usage
df -h

# Clean up old SafePages
# Set retention in Options > Proxy Print > Job Hold Expiry

# Clean logs
sudo find /var/log/printflowlite -name "*.log" -mtime +30 -delete

# Compress old logs
sudo journalctl --vacuum-time=30d
```

---

## Database Issues

### Embedded Derby — "Deadlock detected"

Multiple concurrent connections to embedded Derby.

**Solution:**
- This is a known limitation of embedded Derby
- Migrate to PostgreSQL for production:
1. Install PostgreSQL
2. Create database and user
3. Update `server.properties`:
```properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/printflowlite
db.username=printflowlite
db.password=your-password
```

### PostgreSQL connection refused

**Solutions:**
1. PostgreSQL not running: `sudo systemctl status postgresql`
2. Wrong host/port in connection string
3. `pg_hba.conf` not allowing connections from PrintFlowLite server IP
4. Firewall blocking port 5432

---

## WebSocket (CometD) Issues

### Real-time updates not working

**Solutions:**
1. Check WebSocket support in browser
2. Proxy server (nginx, etc.) may need WebSocket upgrade headers:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```
3. Check the server's WebSocket endpoint is accessible
4. Browser firewall or antivirus may be blocking WebSocket

---

## Upgrade / Migration Issues

### Upgrading from SavaPage — database migration fails

**Solutions:**
1. Always back up the database before upgrading
2. Read the migration notes for your version
3. Ensure all SavaPage prerequisites are met
4. Run migration scripts in order

### Data loss after upgrade

**Prevention:**
- Always create a full backup before upgrading
- Test the upgrade on a staging server first
- Do not skip migration steps

**Recovery:**
- Restore from backup
- If partial data is missing, check the audit log
- Contact the community or support

---

## Getting Help

If you can't resolve an issue:

1. **Check the logs** — `/var/log/printflowlite/printflowlite.log`
2. **Search the community** — [PrintFlowLite Community](https://wiki.printflowlite.org)
3. **Open an issue** — [GitHub Issues](https://github.com/YOUR_GITHUB/PrintFlowLite/issues)
4. **Check SavaPage docs** — [SavaPage Manual](https://www.savapage.org/docs/manual/)

---

## Diagnostic Commands

Useful commands for troubleshooting:

```bash
# Check PrintFlowLite process
ps aux | grep printflowlite

# Check port listeners
sudo lsof -i :8632 -i :631 -i :9100

# Check CUPS status
sudo systemctl status cups
lpstat -a

# Check disk space
df -h

# Check memory
free -m

# Check Java process
jps -l
jstack <pid>  # Thread dump

# Check logs
tail -f /var/log/printflowlite/printflowlite.log

# Database (PostgreSQL)
sudo -u postgres psql -c "SELECT version();"
sudo -u postgres psql -d printflowlite -c "SELECT count(*) FROM users;"
```
