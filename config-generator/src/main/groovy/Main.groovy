import groovy.text.XmlTemplateEngine
import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.yaml.snakeyaml.Yaml

def static printErrorAndExit(String error) {
    println(error)
    System.exit(1)
}

static String addSeparator(String path) {
    return path.endsWith(File.separator) ? path : "${path}${File.separator}"
}

static def exec(String cmd) {
    ProcessGroovyMethods.consumeProcessErrorStream(cmd.execute(), System.err)
}

if (args.length != 3) printErrorAndExit 'Usage: CONFIG_YAML IDENTITIES_YAML OUTPUT_DIR'

Map configYaml = new Yaml().load(new File(args[0]).newInputStream())
Map identitiesYaml = new Yaml().load(new File(args[1]).newInputStream())
def outputDir = addSeparator args[2]

// Add additional params to config map
configYaml.bindAddress = InetAddress.localHost.hostAddress

// Create Keystore if required
def ks = configYaml.keystore
if (ks.crtPath != null) {
    def ksRoot = ks.path == null ? new File("${outputDir}keystores") : new File(ks.path).parentFile
    ksRoot.mkdirs();
    ksRoot = addSeparator ksRoot.getAbsolutePath()
    String crtSrc = addSeparator((String) ks.crtPath)
    String ksPkcs = "${ksRoot}keystore.pkcs12"

    // Add values to the map so they can be used in the templates
    ks.path = ks.path ?: "${ksRoot}keystore.p12"
    ks.password = ks.password ?: "infinispan"

    exec "openssl pkcs12 -export -inkey ${crtSrc}tls.key -in ${crtSrc}tls.crt -out ${ksPkcs} -password pass:${ks.password}"

    exec "keytool -importkeystore -noprompt -srckeystore ${ksPkcs} -srcstoretype pkcs12 -srcstorepass ${ks.password} -destkeystore ${ks.path} -deststoretype pkcs12 -storepass ${ks.password}"
}

// Generate Infinispan configuration
def ispnTemplate = getClass().getResourceAsStream('infinispan.xml').text
def ispnConfig = new File("${outputDir}infinispan.xml")
new XmlTemplateEngine()
        .createTemplate(ispnTemplate)
        .make(configYaml)
        .writeTo(ispnConfig.newWriter())

// Generate JGroups stack file
def transport = configYaml.jgroups.transport
def jgroupsTemplate = getClass().getResourceAsStream("jgroups-${transport}.xml").text
def jgroupsConfig = new File("${outputDir}jgroups-${transport}.xml")
new XmlTemplateEngine()
        .createTemplate(jgroupsTemplate)
        .make(configYaml)
        .writeTo(jgroupsConfig.newWriter())
