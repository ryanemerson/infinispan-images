import groovy.text.XmlTemplateEngine
import org.yaml.snakeyaml.Yaml

Yaml parser = new Yaml()
InputStream inputStream = getClass()
        .getClassLoader()
        .getResourceAsStream("identities.yaml");
Map example = parser.load(inputStream)
example.each { println "Key=$it.key, Val=$it.value" }

def credentials = example.get("credentials")
def certificates = example.get("certificates")
def oauth = example.get("oath")

def xmlEngine = new XmlTemplateEngine()
def xml = new File(getClass().getResource('example.xml').toURI()).text
def xmlBinding = [sslEnabled: false]
def xmlOutput = xmlEngine.createTemplate(xml).make(xmlBinding).toString()

print xmlOutput