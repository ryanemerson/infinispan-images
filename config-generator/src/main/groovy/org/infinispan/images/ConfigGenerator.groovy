package org.infinispan.images

import groovy.text.XmlTemplateEngine
import org.yaml.snakeyaml.Yaml

import java.security.MessageDigest

Map.metaClass.addNested = { Map rhs ->
    def lhs = delegate
    rhs.each { k, v -> lhs[k] = lhs[k] in Map ? lhs[k].addNested(v) : lhs[k] ?: v }
    lhs
}

static void printErrorAndExit(String error) {
    System.err.println error
    System.exit 1
}

static String addSeparator(String path) {
    return path.endsWith(File.separator) ? path : "${path}${File.separator}"
}

static void exec(String cmd) {
    Process process = cmd.execute()
    process.waitForProcessOutput System.out, System.err
    def exitValue = process.exitValue()
    if (exitValue) System.exit exitValue
}

static void processTemplate(String templateName, String dest, Map binding) {
    String template = ConfigGenerator.classLoader.getResourceAsStream(templateName).text
    new XmlTemplateEngine()
            .createTemplate(template)
            .make(binding)
            .writeTo(new File(dest).newWriter())
}

static void createKeystore(ks, String outputDir) {
    if (ks?.crtPath == null) return

    def ksRoot = ks.path == null ? new File("${outputDir}keystores") : new File(ks.path).parentFile
    ksRoot.mkdirs()
    ksRoot = addSeparator ksRoot.getAbsolutePath()
    String crtSrc = addSeparator((String) ks.crtPath)
    String ksPkcs = "${ksRoot}keystore.pkcs12"

    // Add values to the map so they can be used in the templates
    ks.path = ks.path ?: "${ksRoot}keystore.p12"
    ks.password = ks.password ?: "infinispan"

    exec "openssl pkcs12 -export -inkey ${crtSrc}tls.key -in ${crtSrc}tls.crt -out ${ksPkcs} -name ${ks.alias} -password pass:${ks.password}"

    exec "keytool -importkeystore -noprompt -srckeystore ${ksPkcs} -srcstoretype pkcs12 -srcstorepass ${ks.password} -srcalias ${ks.alias} " +
            "-destalias ${ks.alias} -destkeystore ${ks.path} -deststoretype pkcs12 -storepass ${ks.password}"
}

static void processCredentials(credentials, String outputDir, realm = "default") {
    if (!credentials) return

    def (users, groups) = [new Properties(), new Properties()]
    credentials.each { c ->
        if (!c.username || !c.password) printErrorAndExit "Credential identities require both a 'username' and 'password'"

        MessageDigest md5 = MessageDigest.getInstance "MD5"
        byte[] hashed = md5.digest "${c.username}:${realm}:${c.password}".getBytes("UTF-8")
        users.put c.username, hashed.encodeHex().toString()

        if (c.roles) groups.put c.username, c.roles.join(",")
    }
    users.store new File("${outputDir}users.properties").newWriter(), "\$REALM_NAME=${realm}\$"
    groups.store new File("${outputDir}groups.properties").newWriter(), null
}

static void processIdentities(Map identities, String outputDir) {
    processCredentials identities.credentials, outputDir
}

if (args.length != 3) printErrorAndExit 'Usage: CONFIG_YAML IDENTITIES_YAML OUTPUT_DIR'

Map configYaml = new Yaml().load(new File(args[0]).newInputStream())
Map identitiesYaml = new Yaml().load(new File(args[1]).newInputStream())
def outputDir = addSeparator args[2]

// Add default values to Infinispan configuration map if not specified
configYaml.addNested([
        infinispan: [clusterName: 'infinispan'],
        endpoints : [
                hotrod: [
                        qop       : 'auth',
                        serverName: 'infinispan'
                ]],
        jgroups   : [
                bindAddress: InetAddress.localHost.hostAddress,
                transport  : 'udp'
        ],
        keystore  : [alias: 'server']
])

// Create Keystore if required
createKeystore configYaml.keystore, outputDir

// Generate JGroups stack file
def transport = configYaml.jgroups.transport
processTemplate "jgroups-${transport}.xml", "${outputDir}jgroups-${transport}.xml", configYaml

// Generate Infinispan configuration
processTemplate 'infinispan.xml', "${outputDir}infinispan.xml", configYaml

// Process Identities
processIdentities identitiesYaml, outputDir
