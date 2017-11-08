package de.fenvariel.mavenfreemarker;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fabio on 10/26/17.
 */
public class FreemarkerPluginTest extends FreemarkerPlugin {

    @Test
    public void testGeneration() throws MojoExecutionException, IOException {

        setVersion(Version.VERSION_2_3_26);
        setTemplateDir(new File("src/test/resources/template"));
        setFtlTemplate("hello_world.xml.ftl");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        setOutputStream(baos);
        PropertiesFile propertiesFile = new PropertiesFile();
        setBaseDir(new File("src/test/resources/properties"));
        propertiesFile.setFiles("glob:**/hello_world.properties");
        setPropertiesFiles(new PropertiesFile[]{propertiesFile});

        super.execute();

        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        baos.close();

        System.out.print(content);
        Assert.assertTrue(content.length() > 0);
    }

    @Test
    public void escapePlaceholdersTest() {
        java.util.Properties properties = new java.util.Properties();
        String text = "abc ${abc} def ${env_dev} ghi ${build_ghi} jkl ${jkl}";

        escapePlaceholders(text, properties);
        Assert.assertTrue(properties.containsKey("abc"));
        Assert.assertTrue(properties.containsKey("jkl"));
        Assert.assertFalse(properties.containsKey("env.dev"));
        Assert.assertFalse(properties.containsKey("dev"));
        Assert.assertFalse(properties.containsKey("build.ghi"));
        Assert.assertFalse(properties.containsKey("ghi"));
    }

    @Test
    public void testInclude() throws Exception {
        java.util.Properties properties = new java.util.Properties();
        String text = "abc ${abc} def ${env_dev} ghi ${build_ghi} jkl ${jkl}";

        Assert.assertTrue(properties.containsKey("abc"));
    }
}
