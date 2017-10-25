package de.fenvariel.mavenfreemarker;

import freemarker.template.*;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;

/**
 * Created by fabio on 9/21/17.
 */
public class Base64Encoder implements TemplateMethodModelEx {

    private String base64Encode(byte[] s) {
        return Base64.getEncoder().encodeToString(s);
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        return base64Encode(arguments.get(0).toString().getBytes(Charset.forName("UTF-8")));
    }
}
