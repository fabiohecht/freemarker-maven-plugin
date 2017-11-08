/*
 * Copyright 2015 AlexS.
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
package de.fenvariel.mavenfreemarker;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PropertiesFile {

    private String files;

    public Collection<File> getPropertiesFiles(File basedir) throws IOException {

        System.out.println("baseDir: " + basedir.getAbsolutePath());

        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher(files);

        System.out.println("matcher: " + "\"" + files + "\"");

        FindAllFileVisitor visitor = new FindAllFileVisitor(matcher);
        Files.walkFileTree(basedir.toPath(), visitor);
        return visitor.getFiles();
    }

    public void setFiles(String files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return files;
    }
}
