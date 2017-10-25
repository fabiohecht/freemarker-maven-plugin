package de.fenvariel.mavenfreemarker;

import freemarker.template.TemplateModelException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Base64EncoderTest extends Base64Encoder {
    @Test
    public void testBase64Encode() throws Exception {
        List<String> arguments = new ArrayList(1);
        arguments.add("Hello, world!");
        String encoded = (String) this.exec(arguments);
        Assert.assertEquals("SGVsbG8sIHdvcmxkIQ==", encoded);
    }

    @Test
    public void testBase64EncodeUtf8String() throws Exception {
        List<String> arguments = new ArrayList(1);
        arguments.add("Falsches Üben von Xylophonmusik quält jeden größeren Zwerg.");
        String encoded = (String) this.exec(arguments);
        Assert.assertEquals("RmFsc2NoZXMgw5xiZW4gdm9uIFh5bG9waG9ubXVzaWsgcXXDpGx0IGplZGVuIGdyw7bDn2VyZW4gWndlcmcu", encoded);
    }
}