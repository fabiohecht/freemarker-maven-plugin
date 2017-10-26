package de.fenvariel.mavenfreemarker;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.FirstMatchTemplateConfigurationFactory;
import freemarker.core.XMLOutputFormat;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "process-ftl")
public class FreemarkerPlugin extends AbstractMojo {

    @Parameter
    private File templateDir;

    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File baseDir;

    private Configuration fmConfiguration;

    public File getTemplateDir() {
        return templateDir;
    }

    public void setTemplateDir(File templateDir) {
        this.templateDir = templateDir;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public TemplateConfiguration[] getTemplateConfigurations() {
        return templateConfigurations;
    }

    public void setTemplateConfigurations(TemplateConfiguration[] templateConfigurations) {
        this.templateConfigurations = templateConfigurations;
    }

    @Parameter
    private TemplateConfiguration[] templateConfigurations = new TemplateConfiguration[0];
    private Logger log = Logger.getLogger(FreemarkerPlugin.class.getName());

    private Configuration getFreemarker(Version version) throws MojoExecutionException {
        Configuration configuration = new Configuration(version.freemarkerVersion);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(templateDir));
        } catch (IOException ex) {
            throw new MojoExecutionException("failed to initialize freemarker", ex);
        }
        return configuration;
    }

    public void execute() throws MojoExecutionException {
        for (TemplateConfiguration config : templateConfigurations) {
            generate(config);
        }
    }

    private String getNameWithoutExtension(File file) {
        if (file.isDirectory()) {
            return file.getName();
        }
        String filename = file.getName();
        int idx = filename.lastIndexOf('.');
        if (idx > 0) {
            return filename.substring(0, idx);
        } else {
            return filename;
        }
    }

    public Map<String, Object> getConfig(TemplateConfiguration config) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("editableSectionNames", config.getEditableSectionNames().keySet());
        configMap.put("ftlTemplate", config.getFtlTemplate());
        configMap.put("outputDir", config.getOutputDir().toString());
        configMap.put("prefix", config.getPrefix());
        configMap.put("suffix", config.getSuffix());
        configMap.put("targetExtension", config.getTargetExtension());
        configMap.put("version", config.getVersion());
        return configMap;
    }

    private void generate(TemplateConfiguration config) throws MojoExecutionException {

        fmConfiguration = getFreemarker(config.getVersion());
        fmConfiguration.setOutputFormat(XMLOutputFormat.INSTANCE);

        Template template;
        try {
            template = fmConfiguration.getTemplate(config.getFtlTemplate(), "UTF-8");
        } catch (Exception ex) {
            throw new MojoExecutionException("error reading template-file " + config.getFtlTemplate(), ex);
        }

        File outputDir = config.getOutputDir();
        if (!outputDir.exists()) {
            System.out.println("creating output dir " + outputDir);
            outputDir.mkdirs();
        }

        System.out.println("# SourceBundles = " + config.getSourceBundles().length);

        for (SourceBundle source : config.getSourceBundles()) {
            System.out.println("process SourceBundle = " + source.toString());

            Collection<File> sourceFiles;
            try {
                sourceFiles = source.getSourceFiles(baseDir);
            } catch (IOException ex) {
                throw new MojoExecutionException("error reading source files", ex);
            }
            if (sourceFiles == null || sourceFiles.isEmpty()) {
                throw new MojoExecutionException("no source files found for bundle");
            }
            for (File sourceFile : sourceFiles) {
                System.out.println("processing source file = " + sourceFile.getName());

                String destinationFilename = config.getPrefix() + getNameWithoutExtension(sourceFile) + config.getSuffix();
                Path destinationFilePath = Paths.get(outputDir.getAbsolutePath(), destinationFilename + config.getTargetExtension());
                File destinationFile = destinationFilePath.toFile();
                Properties data;
                try {
                    data = loadProperties(sourceFile.getCanonicalPath());
                } catch (IOException e) {
                    throw new MojoExecutionException("Properties file '" + sourceFile + "' cannot be loaded: " + e.getMessage(), e);
                }
                data.put("additionalData", source.getAdditionalData());
                Map<String, Object> configMap = getConfig(config);
                configMap.put("destinationFilePath", destinationFilePath);
                configMap.put("destinationFilename", destinationFilename);
                data.put("config", configMap);
                generate(template, destinationFile, data, config.getEditableSectionNames());
            }
        }
    }

    private Map<String, Object> readJson(File source) throws MojoExecutionException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(source, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            throw new MojoExecutionException("error reading source-file " + source.getAbsolutePath(), ex);
        }
    }

    private void generate(Template template, File file, Properties data, Map<String, Pattern> keepPatterns) throws MojoExecutionException {
        try {

            file.getParentFile().mkdirs();

            escapePlaceholders(file, data);

            Writer writer = new FileWriter(file);
            try {
                System.out.println("using config = " + data.toString());

                template.process(data, writer);
                writer.flush();
                System.out.println("Written " + file.getCanonicalPath());
            } finally {
                writer.close();
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("error generating file: " + file.getAbsolutePath() + " from template " + template.getName() + " and source " + data, ex);
        }
    }

    /**
     * Escapes placeholders that
     */
    void escapePlaceholders(File file, Properties properties) throws MojoExecutionException {

        String contents;

        if (file.exists()) {
            try {
                contents = readFile(file);
            } catch (IOException ex) {
                throw new MojoExecutionException("error parsing file for keep-sections: " + file.getAbsolutePath(), ex);
            }

            escapePlaceholders(contents, properties);
        }
    }


    void escapePlaceholders(String contents, Properties properties) {
        Pattern pattern = Pattern.compile("\\$\\{((?!build_|env_)[^\\}]+)\\}");
        Matcher matcher = pattern.matcher(contents);

        while (matcher.find()) {
            String var = matcher.group(1);
            properties.put(var, "${" + var + "}");
        }
    }

    private String readFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(file.toURI()));
        return new String(bytes);
    }

    protected static Properties loadProperties(String filename) {

        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = Files.newInputStream(Paths.get(filename));//Generator.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                throw new RuntimeException("Unable to find configuration file " + filename);
            }

            //load a properties file from class path, inside static method
            prop.load(new InputStreamReader(input, Charset.forName("UTF-8")));

            return prop;

        } catch (IOException e) {
            throw new RuntimeException("Unable to read configuration file " + filename, e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read configuration file " + filename, e);
                }
            }
        }
    }
}
