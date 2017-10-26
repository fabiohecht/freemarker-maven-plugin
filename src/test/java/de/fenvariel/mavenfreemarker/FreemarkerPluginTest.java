package de.fenvariel.mavenfreemarker;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

/**
 * Created by fabio on 10/26/17.
 */
public class FreemarkerPluginTest extends FreemarkerPlugin {

    @Test
    public void testGeneration() throws MojoExecutionException {
        TemplateConfiguration pluginConfiguration = new TemplateConfiguration();

        pluginConfiguration.setVersion(Version.VERSION_2_3_26);
        setTemplateDir(new File("src/test/resources/template"));
        pluginConfiguration.setFtlTemplate("hello_world.xml.ftl");
        pluginConfiguration.setOutputDir(new File("src/test/resources"));
        SourceBundle sourceBundle = new SourceBundle();
        setBaseDir(new File("src/test/resources/properties"));
        sourceBundle.setFiles("glob:**/hello_world.properties");
        pluginConfiguration.setSourceBundles(new SourceBundle[]{sourceBundle});

        setTemplateConfigurations(new TemplateConfiguration[]{pluginConfiguration});
        super.execute();
    }

    @Test
    public void escapePlaceholdersTest() {
        Properties properties = new Properties();
        String text = "abc ${abc} def ${env_dev} ghi ${build_ghi} jkl ${jkl}";

        escapePlaceholders(text, properties);
        Assert.assertTrue(properties.containsKey("abc"));
        Assert.assertTrue(properties.containsKey("jkl"));
        Assert.assertFalse(properties.containsKey("env.dev"));
        Assert.assertFalse(properties.containsKey("dev"));
        Assert.assertFalse(properties.containsKey("build.ghi"));
        Assert.assertFalse(properties.containsKey("ghi"));
    }
}