import groovy.text.XmlTemplateEngine
import org.yaml.snakeyaml.Yaml

if (args.length != 3) {
    println('Usage: CONFIG_YAML IDENTITIES_YAML OUTPUT_DIR')
    System.exit(1)
}

Map configYaml = new Yaml().load(new File(args[0]).newInputStream())
Map identitiesYaml = new Yaml().load(new File(args[1]).newInputStream())
def outputDir = args[2].endsWith("/") ? args[2] : "${args[2]}/"

// Add additional params to config map
configYaml.bindAddress = InetAddress.localHost.hostAddress

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
