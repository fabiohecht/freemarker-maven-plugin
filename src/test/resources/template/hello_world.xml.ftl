<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<l7:Service xmlns:l7="http://ns.l7tech.com/2010/04/gateway-management">
    <l7:ServiceDetail folderId="0000000000000000ffffffffffffec76">
        <l7:Name>${build_serviceName}</l7:Name>
        <l7:Enabled>true</l7:Enabled>
        <l7:ServiceMappings>
            <l7:HttpMapping>
                <l7:UrlPattern>${build_basePath}</l7:UrlPattern>
            </l7:HttpMapping>
        </l7:ServiceMappings>
    </l7:ServiceDetail>
    <l7:Resources>
        <l7:ResourceSet tag="policy">
            <l7:Resource type="policy">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
                &lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy"&gt;
                &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;
                    &lt;L7p:SetVariable&gt;
                        &lt;L7p:Base64Expression stringValue="${base64Encode(build_included)}"/&gt;
                        &lt;L7p:VariableToSet stringValue="swagger"/&gt;
                    &lt;/L7p:SetVariable&gt;
                    &lt;L7p:CommentAssertion&gt;
                        &lt;L7p:Comment stringValue="${build_included}"/&gt;
                    &lt;/L7p:CommentAssertion&gt;
                    &lt;L7p:HardcodedResponse&gt;
                        &lt;L7p:Base64ResponseBody stringValue=&quot;${dont_replace}&quot;/&gt;
                    &lt;/L7p:HardcodedResponse&gt;
                &lt;/wsp:All&gt;
                &lt;/wsp:Policy&gt;
            </l7:Resource>
        </l7:ResourceSet>
    </l7:Resources>
</l7:Service>