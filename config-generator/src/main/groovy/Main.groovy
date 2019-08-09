import groovy.text.XmlTemplateEngine
import org.yaml.snakeyaml.Yaml

def inputStream = getClass()
        .getClassLoader()
        .getResourceAsStream("config.yaml");
def ispnConfig = new Yaml().load(inputStream)

def xmlEngine = new XmlTemplateEngine()
def xml = new File(getClass().getResource('template.xml').toURI()).text
def xmlOutput = xmlEngine.createTemplate(xml).make(ispnConfig).toString()

print xmlOutput