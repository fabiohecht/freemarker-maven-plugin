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
import freemarker.core.XMLOutputFormat;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "process-ftl")
public class FreemarkerPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", readonly = true)
    private File baseDir;

    @Parameter
    private File templateDir;
    @Parameter
    private String ftlTemplate = null;
    @Parameter
    private String targetExtension = "";
    @Parameter
    private String prefix = "";
    @Parameter
    private String suffix = "";
    @Parameter
    private File outputDir;
    @Parameter
    private Version version;  
    @Parameter
    private PropertiesFile[] propertiesFiles;

    private OutputStream outputStream;

    private Logger log = Logger.getLogger(FreemarkerPlugin.class.getName());

    private Configuration fmConfiguration;

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

    @Override
    public void execute() throws MojoExecutionException {
        generate();
    }
    
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
    
    private Configuration getFreemarker(Version version) throws MojoExecutionException {
        Configuration configuration = new Configuration(version.freemarkerVersion);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(templateDir));
        } catch (IOException ex) {
            throw new MojoExecutionException("failed to initialize freemarker", ex);
        }
        return configuration;
    }

    private String getNameWithoutExtension(File file) {
        if (file.isDirectory()) {
            return file.getName();
        }
        String filename = file.getName();

        return getNameWithoutExtension(filename);
    }

    private String getNameWithoutExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx > 0) {
            return filename.substring(0, idx);
        } else {
            return filename;
        }
    }

    public Map<String, Object> getConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("ftlTemplate", getFtlTemplate());
        configMap.put("outputDir", getOutputDir().toString());
        configMap.put("prefix", getPrefix());
        configMap.put("suffix", getSuffix());
        configMap.put("targetExtension", getTargetExtension());
        configMap.put("version", getVersion());
        return configMap;
    }

    private void generate() throws MojoExecutionException {

        fmConfiguration = getFreemarker(getVersion());
        fmConfiguration.setOutputFormat(XMLOutputFormat.INSTANCE);

        Template template;
        try {
            template = fmConfiguration.getTemplate(getFtlTemplate(), "UTF-8");
        } catch (Exception ex) {
            throw new MojoExecutionException("error reading template-file " + getFtlTemplate(), ex);
        }

        File outputDir = getOutputDir();
        if (outputDir != null && !outputDir.exists()) {
            System.out.println("creating output dir " + outputDir);
            outputDir.mkdirs();
        }

        Properties data = new Properties();

        for (PropertiesFile propertiesFile : propertiesFiles) {

            System.out.println("process PropertiesFile = " + propertiesFile.toString());

            Collection<File> sourceFiles;
            try {
                sourceFiles = propertiesFile.getPropertiesFiles(baseDir);
            } catch (IOException ex) {
                throw new MojoExecutionException("error reading source files", ex);
            }
            if (sourceFiles == null || sourceFiles.isEmpty()) {
                throw new MojoExecutionException("no source files found for bundle");
            }
            for (File file : sourceFiles) {
                System.out.println("processing source file = " + file.getName());

                try {
                    data.putAll(loadProperties(file.getCanonicalPath()));
                } catch (IOException e) {
                    throw new MojoExecutionException("PropertiesFile file '" + propertiesFile + "' cannot be loaded: " + e.getMessage(), e);
                }
            }

        }

        data.put("base64Encode", new Base64Encoder());
        if (this.outputStream!=null) {
            generate(template, new PrintWriter(this.outputStream), data);
        } else {
            String destinationFilename = getPrefix() + getNameWithoutExtension(getFtlTemplate()) + getSuffix();
            Path destinationFilePath = Paths.get(outputDir.getAbsolutePath(), destinationFilename + getTargetExtension());
            File destinationFile = destinationFilePath.toFile();

            Map<String, Object> configMap = getConfig();
            configMap.put("destinationFilePath", destinationFilePath);
            configMap.put("destinationFilename", destinationFilename);
            data.put("config", configMap);

            generate(template, destinationFile, data);
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

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    private void generate(Template template, Writer writer, Properties data) throws MojoExecutionException {

        escapePlaceholders(new File(getTemplateDir(), getFtlTemplate()), data);

        System.out.println("using config = " + data.toString());
        try {
            template.process(data, writer);
            writer.flush();
        } catch (IOException ex) {
            throw new MojoExecutionException("error writing output stream for template " + template.getName() + " and source " + data, ex);
        } catch (TemplateException ex) {
            throw new MojoExecutionException("error processing template " + template.getName() + " and source " + data, ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                throw new MojoExecutionException("error closing output stream for template " + template.getName() + " and source " + data, ex);
            }
        }
    }

    private void generate(Template template, File file, Properties data) throws MojoExecutionException {
            file.getParentFile().mkdirs();

            try {
            Writer writer = new FileWriter(file);
            generate(template, writer, data);
                System.out.println("Written " + file.getCanonicalPath());
        } catch (IOException ex) {
            throw new MojoExecutionException("error generating file: " + file.getAbsolutePath() + " from template " + template.getName() + " and source " + data, ex);
        }
    }

    /**
     * Escapes placeholders that
     */
    void escapePlaceholders(File templateFile, Properties properties) throws MojoExecutionException {

        String contents;

        if (templateFile.exists()) {
            try {
                contents = readFile(templateFile);
            } catch (IOException ex) {
                throw new MojoExecutionException("error parsing file for keep-sections: " + templateFile.getAbsolutePath(), ex);
            }

            escapePlaceholders(contents, properties);
        }
    }


    void escapePlaceholders(String templateContents, Properties properties) {
        Pattern pattern = Pattern.compile("\\$\\{((?!build_|env_)[^\\}]+)\\}");
        Matcher matcher = pattern.matcher(templateContents);

        while (matcher.find()) {
            String var = matcher.group(1);
            properties.put(var, "${" + var + "}");
        }
    }

    private String readFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(file.toURI()));
        return new String(bytes);
    }

    public String getFtlTemplate() {
        return ftlTemplate;
    }

    public void setFtlTemplate(String ftlTemplate) {
        this.ftlTemplate = ftlTemplate;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public String getTargetExtension() {
        return targetExtension;
    }

    public void setTargetExtension(String targetExtension) {
        if (targetExtension == null || targetExtension.trim().isEmpty()) {
            this.targetExtension = "";
        } else {
            targetExtension = targetExtension.trim();
            if (targetExtension.charAt(0) != '.') {
                targetExtension = "." + targetExtension;
            }
            this.targetExtension = targetExtension;
        }

    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    private Pattern compilePattern(String sectionName) {
        int flags = Pattern.DOTALL | Pattern.MULTILINE;
        return Pattern.compile(".*^\\s*?//\\s*?EDITABLE SECTION " + sectionName + ".*?\n(.*?)^\\s*// EDITABLE SECTION " + sectionName
                + " END.*?\n", flags);
    }

    public PropertiesFile[] getPropertiesFiles() {
        return propertiesFiles;
    }

    public void setPropertiesFiles(PropertiesFile[] propertiesFiles) {
        this.propertiesFiles = propertiesFiles;
    }
}
