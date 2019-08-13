import groovy.text.XmlTemplateEngine
import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.yaml.snakeyaml.Yaml

import java.security.MessageDigest

static void printErrorAndExit(String error) {
    println error
    System.exit 1
}

static String addSeparator(String path) {
    return path.endsWith(File.separator) ? path : "${path}${File.separator}"
}

static void exec(String cmd) {
    ProcessGroovyMethods.consumeProcessErrorStream cmd.execute(), System.err
}

static void processTemplate(String templateName, String dest, Map binding) {
    String template = Main.getResourceAsStream(templateName).text
    new XmlTemplateEngine()
            .createTemplate(template)
            .make(binding)
            .writeTo(new File(dest).newWriter())
}

static void createKeystore(ks, String outputDir) {
    if (ks.crtPath == null) return

    def ksRoot = ks.path == null ? new File("${outputDir}keystores") : new File(ks.path).parentFile
    ksRoot.mkdirs()
    ksRoot = addSeparator ksRoot.getAbsolutePath()
    String crtSrc = addSeparator((String) ks.crtPath)
    String ksPkcs = "${ksRoot}keystore.pkcs12"

    // Add values to the map so they can be used in the templates
    ks.path = ks.path ?: "${ksRoot}keystore.p12"
    ks.password = ks.password ?: "infinispan"

    exec "openssl pkcs12 -export -inkey ${crtSrc}tls.key -in ${crtSrc}tls.crt -out ${ksPkcs} -password pass:${ks.password}"

    exec "keytool -importkeystore -noprompt -srckeystore ${ksPkcs} -srcstoretype pkcs12 -srcstorepass ${ks.password} -destkeystore ${ks.path} -deststoretype pkcs12 -storepass ${ks.password}"
}

static void processCredentials(credentials, String outputDir, realm = "default") {
    if (!credentials) return

    def (users, groups) = [new Properties(), new Properties()]
    credentials.each { c ->
        MessageDigest md5 = MessageDigest.getInstance "MD5"
        byte[] hashed = md5.digest "${c.username}:${realm}:${c.password}".getBytes("UTF-8")
        users.put c.username, hashed.encodeHex().toString()
        groups.put c.username, c.roles.join(",")
    }
    users.store new File("${outputDir}users.properties").newWriter(), "\$REALM_NAME=${realm}\$"
    groups.store new File("${outputDir}groups.properties").newWriter(), null
}

private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

static void processIdentities(Map identities, String outputDir) {
    processCredentials identities.credentials, outputDir
}

if (args.length != 3) printErrorAndExit 'Usage: CONFIG_YAML IDENTITIES_YAML OUTPUT_DIR'

Map configYaml = new Yaml().load(new File(args[0]).newInputStream())
Map identitiesYaml = new Yaml().load(new File(args[1]).newInputStream())
def outputDir = addSeparator args[2]

// Add additional params to config map
configYaml.bindAddress = InetAddress.localHost.hostAddress

// Create Keystore if required
createKeystore configYaml.keystore, outputDir

// Generate Infinispan configuration
processTemplate 'infinispan.xml', "${outputDir}infinispan.xml", configYaml

// Generate JGroups stack file
def transport = configYaml.jgroups.transport
processTemplate "jgroups-${transport}.xml", "${outputDir}jgroups-${transport}.xml", configYaml

// Process Identities
processIdentities identitiesYaml, outputDir
